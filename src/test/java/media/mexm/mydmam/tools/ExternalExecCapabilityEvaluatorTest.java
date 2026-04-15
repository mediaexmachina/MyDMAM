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
package media.mexm.mydmam.tools;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.createParentDirectories;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.touch;
import static org.apache.commons.io.FileUtils.write;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;

@ExtendWith(MockToolsExtendsJunit.class)
class ExternalExecCapabilityEvaluatorTest {

    @Fake
    String name;
    @Fake
    String playbookName;
    @Fake
    String workingDirName;
    @Fake(min = 1, max = 100000)
    int returnCode;
    @Fake
    String fileName;

    @Mock
    CapturedStdOutErrTextRetention captured;

    File workingDir;
    ExternalExecCapabilityEvaluator eece;

    @BeforeEach
    void init() {
        workingDir = new File(FileUtils.getTempDirectory(), "mydmam-test-" + workingDirName);
        eece = new ExternalExecCapabilityEvaluator(name, playbookName, workingDir, returnCode, captured);
    }

    @AfterEach
    void ends() {
        deleteQuietly(workingDir);
    }

    @Test
    void testHaveFile() throws IOException {
        assertFalse(eece.haveFile(fileName));

        final var file = new File(workingDir, fileName);
        createParentDirectories(file);
        assertFalse(eece.haveFile(fileName));

        touch(file);
        assertFalse(eece.haveFile(fileName));

        write(file, "?", UTF_8);
        assertTrue(eece.haveFile(fileName));
    }

    @Test
    void testHaveReturnCode() {
        assertFalse(eece.haveReturnCode(returnCode * 2));
        assertTrue(eece.haveReturnCode(returnCode));
        assertTrue(eece.haveReturnCode(returnCode, returnCode * 2));
    }

    @Test
    void testHaveStringInStdOutErr() {
        final var needle = Faker.instance().numerify("Needle####");
        final var paragraphs = Faker.instance().lorem().paragraphs(5);

        final var len3 = paragraphs.get(3).length();
        final var haystack = List.of(
                paragraphs.get(0),
                paragraphs.get(1),
                paragraphs.get(2),
                paragraphs.get(3).substring(0, len3 / 2) + needle + paragraphs.get(3).substring(len3 / 2, len3),
                paragraphs.get(4));
        when(captured.getStdouterrLines(false)).then(_ -> haystack.stream());

        assertTrue(eece.haveStringInStdOutErr(needle));
        assertFalse(eece.haveStringInStdOutErr(needle + "NOPE"));

        verify(captured, times(2)).getStdouterrLines(false);
    }

}
