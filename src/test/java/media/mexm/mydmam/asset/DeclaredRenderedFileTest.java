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
package media.mexm.mydmam.asset;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.datafaker.Faker.instance;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.component.MimeTypeDetector;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class DeclaredRenderedFileTest {

	@Mock
	MimeTypeDetector mimeTypeDetector;

	@Fake
	String name;
	@Fake
	String mimeType;
	@Fake
	int index;
	@Fake
	String previewType;

	DeclaredRenderedFile drf;

	File workingFile;
	String content;

	@BeforeEach
	void init() throws IOException {
		content = instance().lorem().paragraph(20);
		workingFile = File.createTempFile("mydmam-" + getClass().getSimpleName(), "");
		write(workingFile, content, UTF_8, false);
	}

	@AfterEach
	void ends() {
		deleteQuietly(workingFile);
	}

	@Test
	void testMakeAssetRenderedFileEntity_plain() throws IOException {
		when(mimeTypeDetector.getMimeType(workingFile)).thenReturn(mimeType);

		drf = new DeclaredRenderedFile(workingFile, name, false, mimeTypeDetector, index, previewType);

		assertEquals(mimeType, drf.mimeType());
		assertEquals(previewType, drf.previewType());
		assertFalse(drf.toGzip());
		assertEquals(index, drf.index());
		assertEquals(name, drf.name());

		verify(mimeTypeDetector, times(1)).getMimeType(workingFile);
		assertThat(workingFile).exists().content().isEqualTo(content);
	}

	@Test
	void testMakeAssetRenderedFileEntity_gzip() throws IOException {
		when(mimeTypeDetector.getMimeType(workingFile)).thenReturn(mimeType);
		final var beforeSize = workingFile.length();

		drf = new DeclaredRenderedFile(workingFile, name, true, mimeTypeDetector, index, previewType);
		final var gzipWorkingFile = drf.workingFile();
		assertThat(workingFile).doesNotExist().isNotEqualTo(gzipWorkingFile);

		final var assertThatGWF = assertThat(gzipWorkingFile);
		assertThatGWF.exists();
		assertThatGWF.content().isNotEqualTo(content);
		assertThatGWF.size().isBetween(1l, beforeSize);

		assertEquals(mimeType, drf.mimeType());
		assertEquals(previewType, drf.previewType());
		assertTrue(drf.toGzip());
		assertEquals(index, drf.index());
		assertEquals(name, drf.name());

		verify(mimeTypeDetector, times(1)).getMimeType(workingFile);
		deleteQuietly(gzipWorkingFile);
	}

}
