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

import static java.nio.charset.StandardCharsets.UTF_8;
import static media.mexm.mydmam.entity.FileMetadataEntity.MAX_VALUE_LENGTH;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Date;
import java.util.zip.CRC32;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.dto.KeyValueMetadataResponse;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class FileMetadataEntityTest {

	@Mock
	FileEntity file;

	@Fake
	String origin;
	@Fake
	String classifier;
	@Fake
	int layer;
	@Fake
	String key;
	@Fake
	String value;

	FileMetadataEntity fm;

	@BeforeEach
	void init() throws IOException {
		fm = new FileMetadataEntity(file, origin, classifier, layer, key, value);
	}

	@Test
	void testTooBigValue() {
		value = repeat("+", MAX_VALUE_LENGTH);
		new FileMetadataEntity(file, origin, classifier, layer, key, value);

		value += "!";
		assertThrows(IllegalArgumentException.class,
				() -> new FileMetadataEntity(file, origin, classifier, layer, key, value));
	}

	@Test
	void testToKeyValueMetadataResponse() {
		final var keyValueMetadataResponse = fm.toKeyValueMetadataResponse();
		assertThat(keyValueMetadataResponse).isNotNull()
				.isEqualTo(new KeyValueMetadataResponse(classifier, key, value));
	}

	@Test
	void testGetCreateDate() {
		assertThat(fm.getCreateDate()).isBeforeOrEqualTo(new Date());
	}

	@Test
	void testGetFile() {
		assertEquals(fm.getFile(), file);
	}

	@Test
	void testGetEntryCrc() {
		final var crc = new CRC32();
		crc.update((classifier + ":" + key + "[" + layer + "]").getBytes(UTF_8));
		assertThat(fm.getEntryCrc()).isEqualTo(crc.getValue());

	}

	@Test
	void testGetAuditTrailPayload() {
		final var payload = fm.getAuditTrailPayload();
		assertThat(payload)
				.isNotNull()
				.hasSize(5)
				.containsEntry("classifier", classifier)
				.containsEntry("key", key)
				.containsEntry("layer", layer)
				.containsEntry("origin", origin)
				.containsEntry("value", value);
	}

}
