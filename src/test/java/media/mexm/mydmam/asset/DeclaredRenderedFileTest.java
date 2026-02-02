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
import static media.mexm.mydmam.entity.AssetRenderedFileEntity.GZIP_ENCODED;
import static media.mexm.mydmam.entity.AssetRenderedFileEntity.NOT_ENCODED;
import static net.datafaker.Faker.instance;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
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
import media.mexm.mydmam.entity.FileEntity;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class DeclaredRenderedFileTest {

	@Mock
	MimeTypeDetector mimeTypeDetector;
	@Mock
	FileEntity fileEntity;

	@Fake
	String name;
	@Fake
	String mimeType;
	@Fake
	int index;
	@Fake
	String previewType;
	@Fake
	int fileId;

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
		when(fileEntity.getId()).thenReturn(fileId);

		drf = new DeclaredRenderedFile(workingFile, name, false, mimeTypeDetector);
		final var asset = drf.makeAssetRenderedFileEntity(fileEntity, index, previewType);
		assertNotNull(asset);

		assertEquals(fileEntity, asset.getFile());
		assertEquals(mimeType, asset.getMimeType());
		assertEquals(previewType, asset.getPreviewType());
		assertEquals(NOT_ENCODED, asset.getEncoded());
		assertEquals(index, asset.getIndexref());
		assertEquals(name, asset.getName());
		assertEquals(workingFile.length(), asset.getLength());

		verify(mimeTypeDetector, times(1)).getMimeType(workingFile);
		verify(fileEntity, atLeastOnce()).getId();
		assertThat(workingFile).exists().content().isEqualTo(content);
	}

	@Test
	void testMakeAssetRenderedFileEntity_gzip() throws IOException {
		when(mimeTypeDetector.getMimeType(workingFile)).thenReturn(mimeType);
		when(fileEntity.getId()).thenReturn(fileId);
		final var beforeSize = workingFile.length();

		drf = new DeclaredRenderedFile(workingFile, name, true, mimeTypeDetector);
		final var gzipWorkingFile = drf.workingFile();
		assertThat(workingFile).doesNotExist().isNotEqualTo(gzipWorkingFile);

		final var assertThatGWF = assertThat(gzipWorkingFile);
		assertThatGWF.exists();
		assertThatGWF.content().isNotEqualTo(content);
		assertThatGWF.size().isBetween(1l, beforeSize);

		final var asset = drf.makeAssetRenderedFileEntity(fileEntity, index, previewType);
		assertNotNull(asset);

		assertEquals(fileEntity, asset.getFile());
		assertEquals(mimeType, asset.getMimeType());
		assertEquals(previewType, asset.getPreviewType());
		assertEquals(GZIP_ENCODED, asset.getEncoded());
		assertEquals(index, asset.getIndexref());
		assertEquals(name, asset.getName());
		assertEquals(gzipWorkingFile.length(), asset.getLength());

		verify(mimeTypeDetector, times(1)).getMimeType(workingFile);
		verify(fileEntity, atLeastOnce()).getId();
		deleteQuietly(gzipWorkingFile);
	}

}
