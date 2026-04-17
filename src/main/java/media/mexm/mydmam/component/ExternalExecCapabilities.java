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
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.io.FileUtils.cleanDirectory;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static tv.hd3g.processlauncher.CapturedStreams.BOTH_STDOUT_STDERR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.tools.ExternalExecCapabilityDb;
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
public class ExternalExecCapabilities { // TODO test

    private final ExecutableFinder executableFinder;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<File, List<Parameters>> setupParamsByExec;
    private final ConcurrentHashMap<File, File> currentSetupDirByExec;
    private final ExecutionTimeLimiter executionTimeLimiter;
    private final File execCapabilitiesTempDir;
    private final ExternalExecCapabilityDb db;
    private final ReentrantLock lock;

    private File execCapabilitiesJsonFile;

    public ExternalExecCapabilities(@Autowired final ExecutableFinder executableFinder,
                                    @Autowired final ScheduledExecutorService maxExecTimeScheduler,
                                    @Autowired final MyDMAMConfigurationProperties configuration,
                                    @Autowired final ObjectMapper objectMapper) {
        lock = new ReentrantLock();
        this.executableFinder = executableFinder;
        this.objectMapper = objectMapper;
        setupParamsByExec = new ConcurrentHashMap<>();
        currentSetupDirByExec = new ConcurrentHashMap<>();
        executionTimeLimiter = new ExecutionTimeLimiter(30, SECONDS, maxExecTimeScheduler);
        execCapabilitiesTempDir = Optional.ofNullable(configuration.tools().execCapabilitiesTempDir())
                .map(File::new)
                .map(File::getAbsoluteFile)
                .orElse(new File(getTempDirectory(), "mydmam-exec-capabilities-test-zone"));
        log.debug("Use {} as working temp directory", execCapabilitiesTempDir);

        final var execCapabilitiesJsonFileName = configuration.tools().execCapabilitiesJsonFile();
        if (execCapabilitiesJsonFileName == null) {
            db = new ExternalExecCapabilityDb();
        } else {
            execCapabilitiesJsonFile = new File(execCapabilitiesJsonFileName);
            if (execCapabilitiesJsonFile.exists()) {
                log.info("Load exec capabilities json file {}", execCapabilitiesJsonFile);
                try {
                    db = objectMapper.readValue(execCapabilitiesJsonFile, ExternalExecCapabilityDb.class);
                } catch (final IOException e) {
                    throw new UncheckedIOException("Can't read from json file " + execCapabilitiesJsonFile, e);
                }
            } else {
                db = new ExternalExecCapabilityDb();
                save();
            }
        }
    }

    private void save() {
        if (execCapabilitiesJsonFile == null) {
            log.debug("Can't save, disabled {}", getClass().getSimpleName());
            return;
        }

        lock.lock();
        try {
            log.debug("Save exec capabilities json file to {}", execCapabilitiesJsonFile);
            objectMapper.writeValue(execCapabilitiesJsonFile, db);
        } catch (final IOException e) {
            throw new UncheckedIOException("Can't save json", e);
        } finally {
            lock.unlock();
        }
    }

    public void setup(final String execName, final List<Parameters> params) {
        final var oExec = executableFinderGet(execName);
        if (oExec.isEmpty()) {
            return;
        }
        log.debug("Setup {}", execName);
        setupParamsByExec.put(oExec.get(), params);
    }

    private File setupRun(final String name,
                          final File exec) {
        final var paramListToSetup = setupParamsByExec.get(exec);
        requireNonNull(paramListToSetup, "Please setup() " + exec + " before addPlaybook()");

        final var newWorkingDir = new File(execCapabilitiesTempDir, name);
        if (newWorkingDir.exists()) {
            try {
                cleanDirectory(newWorkingDir);
            } catch (final IOException e) {
                throw new UncheckedIOException("Can't setup working directory " + newWorkingDir, e);
            }
        }

        paramListToSetup.forEach(p -> {
            log.info("Setup exec to process capabilities: {} {} [on {}]", exec, p, newWorkingDir);
            run(name, "setup", exec, p, newWorkingDir, _ -> true);
        });

        return newWorkingDir;
    }

    private Optional<File> executableFinderGet(final String execName) {
        try {
            return Optional.ofNullable(executableFinder.get(execName));
        } catch (final FileNotFoundException e) {
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
            log.debug("Add playbook \"{}\": {} {}", playbookName, exec, params);

            final var playbookResult = db.getPlaybookResult(exec, playbookName);
            if (playbookResult.isPresent()) {
                final var playbookResultStatus = (boolean) playbookResult.get();
                log.trace("Contains playbook \"{}\" [{}] for {}",
                        playbookName,
                        playbookResultStatus ? "passing" : "fail",
                        exec);
                return;
            }

            final var workingDir = currentSetupDirByExec.computeIfAbsent(exec, newExec -> setupRun(execName, newExec));
            final var pass = run(execName, playbookName, exec, params, workingDir, evaluator);

            log.info("Playbook compute result \"{}\" is {}: {} {}",
                    playbookName,
                    pass ? "passing" : "fail",
                    exec,
                    params);
            db.addPlaybookResult(exec, playbookName, pass);
            save();
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

        log.debug("Tear down {}", execName);

        final var workingDir = currentSetupDirByExec.remove(oExec.get());
        if (workingDir != null) {
            try {
                forceDelete(workingDir);
            } catch (final IOException e) {
                log.error("Can't delete {}", workingDir, e);
            }
        }
    }

    public Set<String> getPassingPlaybookNames(final String execName) {
        final var oExec = executableFinderGet(execName);
        if (oExec.isEmpty()) {
            return Set.of();
        }

        lock.lock();
        try {
            return db.getAllPlaybookResults(oExec.get())
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() == true)
                    .map(Entry::getKey)
                    .collect(toUnmodifiableSet());
        } finally {
            lock.unlock();
        }
    }

}
