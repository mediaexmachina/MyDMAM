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
package media.mexm.mydmam.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class AssetTextExtractedFileEntityTest {

    @Fake
    String name;
    @Fake
    long length;

    @Mock
    FileEntity file;

    AssetTextExtractedFileEntity atefe;

    @BeforeEach
    void init() {
        atefe = new AssetTextExtractedFileEntity(file, name, length);
    }

    @Test
    void testCreate() {
        assertEquals(file, atefe.getFile());
        assertEquals(name, atefe.getName());
        assertEquals(0, atefe.getIndexref());
        assertEquals(length, atefe.getLength());
        assertThat(atefe.getCreateDate()).isBeforeOrEqualTo(new Date());
        assertTrue(atefe.isGzipEncoded());
    }

    @Test
    void testGetAuditTrailPayload() {
        final var renderedFile = new File(".").getAbsoluteFile();
        assertThat(atefe.getAuditTrailPayload(renderedFile))
                .isEqualTo(Map.of(
                        "file", renderedFile.getAbsolutePath(),
                        "length", length,
                        "name", name));
    }

}
