/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * Copyright (C) Media ex Machina 2026
 *
 */
package media.mexm.mydmam.component;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static tv.hd3g.processlauncher.CapturedStreams.BOTH_STDOUT_STDERR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.zip.CRC32;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.repository.ExternalExecDao;
import media.mexm.mydmam.tools.ExternalExecCapabilityEvaluator;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.ExecutionTimeLimiter;
import tv.hd3g.processlauncher.InvalidExecution;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

/**
 * Thread safe
 */
@Slf4j
@Component
public class ExternalExecCapabilities {

    private final ReentrantLock lock;
    private final ExecutableFinder executableFinder;
    private final ExternalExecDao externalExecDao;
    private final ConcurrentHashMap<File, ExecConf> execConfs;
    private final ExecutionTimeLimiter executionTimeLimiter;
    private final File execCapabilitiesTempDir;

    public ExternalExecCapabilities(@Autowired final ExecutableFinder executableFinder,
                                    @Autowired final ScheduledExecutorService maxExecTimeScheduler,
                                    @Autowired final MyDMAMConfigurationProperties configuration,
                                    @Autowired final ExternalExecDao externalExecDao) {
        lock = new ReentrantLock();
        this.executableFinder = executableFinder;
        this.externalExecDao = externalExecDao;
        execConfs = new ConcurrentHashMap<>();
        executionTimeLimiter = new ExecutionTimeLimiter(30, SECONDS, maxExecTimeScheduler);

        execCapabilitiesTempDir = Optional.ofNullable(configuration.tools())
                .flatMap(t -> Optional.ofNullable(t.execCapabilitiesTempDir()))
                .map(File::new)
                .map(File::getAbsoluteFile)
                .orElse(new File(getTempDirectory(), "mydmam-exec-capabilities-test-zone"));
        log.debug("Use {} as working temp directory", execCapabilitiesTempDir);
    }

    private record ExecConf(String name,
                            File exec,
                            File workingDir,
                            List<Parameters> setupParams,
                            long crc,
                            AtomicBoolean isSetup) {

        ExecConf(final String name,
                 final File exec,
                 final List<Parameters> setupParams,
                 final File execCapabilitiesTempDir) {
            var crcValue = 0L;
            try (final var reader = new RandomAccessFile(exec, "r")) {
                final var channel = reader.getChannel();
                final var buff = ByteBuffer.allocateDirect((int) Math.min(100_000l, exec.length()));

                final var crc = new CRC32();
                while (channel.read(buff) > 0) {
                    crc.update(buff.flip());
                    buff.clear();
                }
                crcValue = crc.getValue();
            } catch (final IOException e) {
                throw new UncheckedIOException("Can't read " + exec, e);
            }

            final var workingDir = new File(execCapabilitiesTempDir, name);
            if (workingDir.exists()) {
                try {
                    log.debug("Delete working directory {}", workingDir);
                    cleanDirectory(workingDir);
                } catch (final IOException e) {
                    throw new UncheckedIOException("Can't setup working directory " + workingDir, e);
                }
            }

            this(name, exec, workingDir, setupParams, crcValue, new AtomicBoolean());
        }
    }

    public void setup(final String execName, final List<Parameters> params) {
        final var oExec = executableFinderGet(execName);
        if (oExec.isEmpty()) {
            return;
        }
        log.debug("Setup {}", execName);
        final var exec = oExec.get();
        execConfs.put(exec, new ExecConf(execName, exec, params, execCapabilitiesTempDir));
    }

    private File setupRun(final File exec) {
        final var execConf = execConfs.get(exec);
        requireNonNull(execConf, "Please setup() " + exec + " before addPlaybook()");

        final var workingDir = execConf.workingDir();
        execConf.setupParams().forEach(p -> {
            log.info("Setup exec to process capabilities: {} {} [on {}]", exec, p, workingDir);
            run(execConf.name(), "setup", exec, p, workingDir, _ -> true);
        });

        execConf.isSetup().set(true);
        return workingDir;
    }

    private Optional<File> executableFinderGet(final String execName) {
        try {
            return Optional.ofNullable(executableFinder.get(execName));
        } catch (final FileNotFoundException e) {
            externalExecDao.removeExec(execName);
            return empty();
        }
    }

    public void addPlaybook(final String execName,
                            final String playbookName,
                            final Parameters params,
                            final Predicate<ExternalExecCapabilityEvaluator> evaluator) {
        final var oExec = executableFinderGet(execName);
        if (oExec.isEmpty()) {
            return;
        }
        final var exec = oExec.get();

        lock.lock();
        try {
            final var conf = execConfs.computeIfAbsent(exec, newExec -> {
                final var newConf = new ExecConf(execName, newExec, List.of(), execCapabilitiesTempDir);
                newConf.isSetup().set(true);
                return newConf;
            });

            if (conf.isSetup().get() == false) {
                setupRun(exec);
            }

            log.debug("Add playbook \"{}\": {} {}", playbookName, exec, params);

            final var playbookResult = externalExecDao.getPlaybookResult(execName, exec, playbookName, conf.crc());
            if (playbookResult.isPresent()) {
                final var playbookResultStatus = (boolean) playbookResult.get();
                log.trace("Contains playbook \"{}\" [{}] for {}",
                        playbookName,
                        playbookResultStatus ? "passing" : "fail",
                        exec);
                return;
            }

            final var pass = run(execName, playbookName, exec, params, conf.workingDir(), evaluator);

            log.info("Playbook compute result \"{}\" is {}: {} {}",
                    playbookName,
                    pass ? "passing" : "fail",
                    exec,
                    params);

            externalExecDao.addPlaybookResult(execName, exec, playbookName, pass, conf.crc());
        } finally {
            lock.unlock();
        }
    }

    private boolean run(final String name,
                        final String playbookName,
                        final File execName,
                        final Parameters params,
                        final File workingDir,
                        final Predicate<ExternalExecCapabilityEvaluator> evaluator) {
        try {
            forceMkdir(workingDir);
            final var commandLine = new CommandLine(execName.getAbsolutePath(), params, executableFinder);
            final var processlauncherBuilder = new ProcesslauncherBuilder(commandLine);
            final var capText = new CapturedStdOutErrTextRetention();
            processlauncherBuilder.getSetCaptureStandardOutputAsOutputText(BOTH_STDOUT_STDERR)
                    .addObserver(capText);
            processlauncherBuilder.setExecutionTimeLimiter(executionTimeLimiter);
            processlauncherBuilder.setWorkingDirectory(workingDir);
            processlauncherBuilder.setExecCodeMustBeZero(false);
            final var result = processlauncherBuilder.start().checkExecution();

            return evaluator.test(new ExternalExecCapabilityEvaluator(
                    name,
                    playbookName,
                    workingDir,
                    result.getExitCode(),
                    capText));
        } catch (final IOException e) {
            log.error("Fail start process {} {} [on {}]", execName, params, workingDir, e);
        } catch (final InvalidExecution e) {
            log.error("Fail to run process {} {} [on {}]", execName, params, workingDir, e);
        }
        return false;
    }

    public void tearDown(final String execName) {
        final var oExec = executableFinderGet(execName);
        if (oExec.isEmpty()) {
            return;
        }
        Optional.ofNullable(execConfs.remove(oExec.get()))
                .ifPresent(oldSetup -> {
                    final var workingDir = oldSetup.workingDir();
                    if (workingDir.exists()) {
                        log.debug("Tear down {}", execName);
                        try {
                            forceDelete(oldSetup.workingDir());
                        } catch (final IOException e) {
                            log.error("Can't delete {}", workingDir, e);
                        }
                    }
                });

    }

    public Set<String> getPassingPlaybookNames(final String execName) {
        final var oExec = executableFinderGet(execName);
        if (oExec.isEmpty()) {
            return Set.of();
        }

        lock.lock();
        try {
            final var exec = oExec.get();
            final var conf = execConfs.computeIfAbsent(exec, newExec -> {
                final var newConf = new ExecConf(execName, newExec, List.of(), execCapabilitiesTempDir);
                newConf.isSetup().set(true);
                return newConf;
            });

            return externalExecDao.getAllPlaybookPass(execName, exec, conf.crc());
        } finally {
            lock.unlock();
        }
    }

}
