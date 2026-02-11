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

import static java.lang.Long.toHexString;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static media.mexm.mydmam.entity.AssetRenderedFileEntity.GZIP_ENCODED;
import static media.mexm.mydmam.entity.AssetRenderedFileEntity.NOT_ENCODED;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.asset.DeclaredRenderedFile;
import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class AssetRenderedFileEntityTest {

	@Mock
	FileEntity file;

	@Fake
	String mimeType;
	@Fake
	String previewType;
	@Fake(min = 1, max = 100)
	int indexref;
	@Fake
	String name;
	@Fake(min = 1, max = 10_000)
	int id;
	@Fake(min = 1, max = 10_000)
	int fileId;
	@Fake
	String encoded;

	AssetRenderedFileEntity arf;
	long etag;
	DeclaredRenderedFile rendered;
	File workingFile;
	long length;

	@BeforeEach
	void init() throws IOException {
		when(file.getId()).thenReturn(fileId);

		workingFile = File.createTempFile("mydmam-" + getClass().getSimpleName(), "workingFile");
		write(workingFile, Faker.instance().lorem().paragraph(5), UTF_8);
		length = workingFile.length();

		rendered = new DeclaredRenderedFile(workingFile, name, false, mimeType, indexref, previewType);
		arf = new AssetRenderedFileEntity(file, rendered);
		setId();

		final var nameb = name.getBytes(UTF_8);
		final var numbers = ByteBuffer.allocate((64 + 32 + 32 + 64) / 8 + nameb.length);
		numbers.putLong(length);
		numbers.putInt(indexref);
		numbers.putInt(fileId);
		numbers.putLong(arf.getCreateDate().getTime());
		numbers.put(nameb);

		final var crc = new CRC32();
		crc.update(numbers.flip());
		etag = crc.getValue();
	}

	@AfterEach
	void ends() {
		verify(file, atLeastOnce()).getId();
		deleteQuietly(workingFile);
	}

	void setId() {
		try {
			final var f1 = AssetRenderedFileEntity.class.getDeclaredField("id");
			f1.setAccessible(true);
			f1.set(arf, id);
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException e) {
			throw new IllegalCallerException(e);
		}
	}

	@Test
	void testGetRelativePath() {
		when(file.getId()).thenReturn(0x1234ABCD);

		rendered = new DeclaredRenderedFile(workingFile, name, true, mimeType, indexref, previewType);
		arf = new AssetRenderedFileEntity(file, rendered);
		setId();
		assertEquals("/1234/ABCD/" + id + "." + indexref + "." + name + ".gz",
				arf.getRelativePath());

		rendered = new DeclaredRenderedFile(workingFile, name, false, mimeType, indexref, previewType);
		arf = new AssetRenderedFileEntity(file, rendered);
		setId();
		assertEquals("/1234/ABCD/" + id + "." + indexref + "." + name,
				arf.getRelativePath());
	}

	@Test
	void testToRenderedReponse() {
		final var response = arf.toRenderedReponse();
		assertNotNull(response);
		assertEquals(previewType, response.previewType());
		assertEquals(indexref, response.index());
		assertEquals(name, response.name());
	}

	@Test
	void testGetHexETag() {
		assertEquals(toHexString(etag), arf.getHexETag());
	}

	@Test
	void testGetCreateDate() {
		assertThat(arf.getCreateDate().getTime())
				.isCloseTo(currentTimeMillis(), offset(10_000l));
	}

	@Test
	void testGetFile() {
		assertEquals(file, arf.getFile());
	}

	@Test
	void testGetMimeType() {
		assertEquals(mimeType, arf.getMimeType());
	}

	@Test
	void testGetPreviewType() {
		assertEquals(previewType, arf.getPreviewType());
	}

	@Test
	void testGetEncoded() {
		assertEquals(NOT_ENCODED, arf.getEncoded());

		rendered = new DeclaredRenderedFile(workingFile, name, true, mimeType, indexref, previewType);
		arf = new AssetRenderedFileEntity(file, rendered);
		assertEquals(GZIP_ENCODED, arf.getEncoded());
	}

	@Test
	void testGetIndexref() {
		assertEquals(indexref, arf.getIndexref());
	}

	@Test
	void testGetName() {
		assertEquals(name, arf.getName());
	}

	@Test
	void testGetLength() {
		assertEquals(length, arf.getLength());
	}

	@Test
	void testGetEtag() {
		assertEquals(etag, arf.getEtag());
	}

}
