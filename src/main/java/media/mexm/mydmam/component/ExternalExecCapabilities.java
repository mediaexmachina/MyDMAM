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
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.apache.commons.io.FileUtils.forceDelete;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.tools.ExternalExecCapabilityDb;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Slf4j
@Component
public class ExternalExecCapabilities implements InternalService { // TODO impl + test

    private final ExecutableFinder executableFinder;
    private final MyDMAMConfigurationProperties configuration;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<File, List<Parameters>> setupParamsByExec;
    private final ConcurrentHashMap<File, File> currentSetupDirByExec;

    private File execCapabilitiesJsonFile;
    private ExternalExecCapabilityDb db;

    public ExternalExecCapabilities(@Autowired final ExecutableFinder executableFinder,
                                    @Autowired final MyDMAMConfigurationProperties configuration,
                                    @Autowired final ObjectMapper objectMapper) {
        this.executableFinder = executableFinder;
        this.configuration = configuration;
        this.objectMapper = objectMapper;
        setupParamsByExec = new ConcurrentHashMap<>();
        currentSetupDirByExec = new ConcurrentHashMap<>();
    }

    @Override
    public void internalServiceStart() throws Exception {
        final var execCapabilitiesJsonFileName = configuration.tools().execCapabilitiesJsonFile();
        if (execCapabilitiesJsonFileName == null) {
            db = new ExternalExecCapabilityDb();
            return;
        }
        execCapabilitiesJsonFile = new File(execCapabilitiesJsonFileName);
        if (execCapabilitiesJsonFile.exists()) {
            log.info("Load exec capabilities json file {}", execCapabilitiesJsonFile);
            db = objectMapper.readValue(execCapabilitiesJsonFile, ExternalExecCapabilityDb.class);
        } else {
            db = new ExternalExecCapabilityDb();
            save();
        }
    }

    // TODO thread safe

    private void save() {
        if (execCapabilitiesJsonFile == null) {
            log.debug("Can't save, disabled {}", getClass().getSimpleName());
            return;
        }

        try {
            log.debug("Save exec capabilities json file to {}", execCapabilitiesJsonFile);
            objectMapper.writeValue(execCapabilitiesJsonFile, db);
        } catch (final IOException e) {
            throw new UncheckedIOException("Can't save json", e);
        }
    }

    public void setup(final String execName, final List<Parameters> params) throws FileNotFoundException {
        final var exec = executableFinder.get(execName);
        log.debug("Setup {}", execName);
        setupParamsByExec.put(exec, params);
    }

    public void tearDown(final String execName) throws IOException {
        final var exec = executableFinder.get(execName);
        log.debug("Tear down {}", execName);

        final var workingDir = currentSetupDirByExec.remove(exec);
        if (workingDir != null) {
            forceDelete(workingDir);
        }
    }

    public void addPlaybook(final String execName,
                            final String playbookName,
                            final Parameters params) throws FileNotFoundException {
        final var exec = executableFinder.get(execName);
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

        final var workingDir = currentSetupDirByExec.computeIfAbsent(exec, newExec -> {
            final var paramListToSetup = setupParamsByExec.get(exec);
            requireNonNull(paramListToSetup, "Please setup() " + newExec + " before addPlaybook()");
            final var newWorkingDir = new File(".");// XXX

            // TODO run setup actions !

            return newWorkingDir;
        });

        final var pass = false;
        // TODO compute with workingDir

        log.info("Playbook compute result \"{}\" is {}: {} {}",
                playbookName,
                pass ? "passing" : "fail",
                exec,
                params);
        db.addPlaybookResult(exec, playbookName, pass);
        save();
    }

    public Set<String> getPassingPlaybookNames(final String execName) throws FileNotFoundException {
        final var exec = executableFinder.get(execName);
        return db.getAllPlaybookResults(exec)
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() == true)
                .map(Entry::getKey)
                .collect(toUnmodifiableSet());
    }

    @Override
    public String getInternalServiceName() {
        return getClass().getSimpleName();
    }

}
