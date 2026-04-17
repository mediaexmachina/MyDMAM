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

import static java.io.File.separatorChar;
import static media.mexm.mydmam.SimpleApp.OUT_FILE_CONTENT;
import static media.mexm.mydmam.SimpleApp.OUT_FILE_NAME;
import static media.mexm.mydmam.SimpleApp.STATUS;
import static media.mexm.mydmam.SimpleApp.SYS_OUT;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;

import media.mexm.mydmam.SimpleApp;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.tools.ExternalExecCapabilityDb;
import media.mexm.mydmam.tools.ExternalExecCapabilityEvaluator;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@SpringBootTest(webEnvironment = NONE,
                properties = {
                               "mydmam.tools.exec-capabilities-json-file=target/exec-capabilities-test.json"
                })
@ActiveProfiles({ "Default" })
@ExtendWith(MockToolsExtendsJunit.class)
class ExternalExecCapabilitiesTest {

    @Autowired
    ExecutableFinder executableFinder;
    @Autowired
    ScheduledExecutorService maxExecTimeScheduler;
    @Autowired
    MyDMAMConfigurationProperties configuration;
    @Autowired
    ObjectMapper objectMapper;

    ExternalExecCapabilities eec;

    @Test
    void testEndToEnd() throws IOException {
        final var execCapabilitiesJsonFile = new File(configuration.tools().execCapabilitiesJsonFile());
        if (execCapabilitiesJsonFile.exists()) {
            forceDelete(execCapabilitiesJsonFile);
        }

        final var ext = IS_OS_WINDOWS ? ".exe" : "";
        final var execFile = new File((String) System.getProperties().get("java.home") + "/bin/java" + ext)
                .getAbsoluteFile()
                .getCanonicalFile();
        assertThat(execFile).exists();
        final var execName = "java-test";
        executableFinder.registerExecutable(execName, execFile);

        final var javaClassFile = new File("src/test/java/"
                                           + SimpleApp.class.getName().replace('.', separatorChar)
                                           + ".java");
        assertThat(javaClassFile).exists();

        eec = new ExternalExecCapabilities(executableFinder, maxExecTimeScheduler, configuration, objectMapper);
        eec.setup(execName, List.of(Parameters.bulk("-version")));

        assertThat(eec.getPassingPlaybookNames(execName)).isEmpty();

        final var evaluatorRef = new AtomicReference<ExternalExecCapabilityEvaluator>();
        final var playbookName = SimpleApp.class.getSimpleName();
        eec.addPlaybook(
                execName,
                playbookName,
                Parameters.bulk(javaClassFile.getAbsolutePath()),
                evaluator -> {
                    evaluatorRef.set(evaluator);
                    return true;
                });

        final var evaluator = evaluatorRef.get();
        assertThat(evaluator).isNotNull();

        assertThat(evaluator.name()).isEqualTo(execName);
        assertThat(evaluator.playbookName()).isEqualTo(playbookName);

        final var expectFile = new File(evaluator.workingDir(), OUT_FILE_NAME);
        assertThat(expectFile).exists().content().isEqualTo(OUT_FILE_CONTENT);

        assertThat(evaluator.returnCode()).isEqualTo(STATUS);
        final var lines = evaluator.captured().getStdouterrLines(false).toList();
        assertThat(lines).hasSize(1).contains(SYS_OUT);

        eec.tearDown(execName);

        assertThat(evaluator.workingDir()).doesNotExist();

        assertThat(execCapabilitiesJsonFile).exists();
        final var db = objectMapper.readValue(execCapabilitiesJsonFile, ExternalExecCapabilityDb.class);

        final var execPath = execFile.getCanonicalPath();
        assertThat(db.getVersion()).isEqualTo(1);
        assertThat(db.getBinaries()).containsKey(execPath);
        final var execPlaybooks = db.getBinaries().get(execPath);
        assertThat(execPlaybooks.getCrc()).isNotZero();
        assertThat(execPlaybooks.getDate()).isNotZero();
        assertThat(execPlaybooks.getSize()).isNotZero();
        assertThat(execPlaybooks.getResults()).hasSize(1).containsExactly(Map.entry(playbookName, true));

        eec = new ExternalExecCapabilities(executableFinder, maxExecTimeScheduler, configuration, objectMapper);
        eec.setup(execName, List.of(Parameters.bulk("-NOPENOPE")));
        eec.addPlaybook(
                execName,
                playbookName,
                Parameters.bulk("NOPENOPE"),
                _ -> {
                    throw new IllegalStateException();
                });

        assertThat(evaluator.workingDir()).doesNotExist();
        eec.tearDown(execName);

        assertThat(eec.getPassingPlaybookNames(execName)).containsExactly(playbookName);
        assertThat(execCapabilitiesJsonFile).exists();
        assertThat(evaluator.workingDir()).doesNotExist();

        forceDelete(execCapabilitiesJsonFile);
    }

    // TODO test with not empty newWorkingDir
    // TODO test with not found exec
    // TODO test with not passing

}
