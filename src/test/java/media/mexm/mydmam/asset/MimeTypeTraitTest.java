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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.entity.FileMetadataEntity;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class MimeTypeTraitTest {

	class Impl implements MimeTypeTrait {

		final MimeTypeTraitTest ref;

		Impl(final MimeTypeTraitTest ref) {
			this.ref = ref;
		}

		@Override
		public void createFileMetadataEntry(final String originHandler,
											final String classifier,
											final int layer,
											final String key,
											final String value) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<FileMetadataEntity> getMetadatas() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void createFileMetadataEntry(final ActivityHandler hander,
											final String classifier,
											final int layer,
											final String key,
											final String value) {
			assertEquals(ref.hander, hander);
			assertEquals(MTD_FILE_FORMAT_CLASSIFIER, classifier);
			assertEquals(0, layer);
			assertEquals(MTD_MIME_TYPE_KEY, key);
			assertEquals(ref.value, value);
		}

		@Override
		public Optional<String> getMetadataValue(final String classifier, final String key) {
			assertEquals(MTD_FILE_FORMAT_CLASSIFIER, classifier);
			assertEquals(MTD_MIME_TYPE_KEY, key);
			return Optional.ofNullable(ref.value);
		}

	}

	@Mock
	ActivityHandler hander;

	@Fake
	int layer;
	@Fake
	String value;

	Impl impl;

	@BeforeEach
	void init() {
		impl = new Impl(this);
	}

	@Test
	void testGetMimeType() {
		assertThat(impl.getMimeType()).contains(value);
	}

	@Test
	void testSetMimeType() {// NOSONAR S2699
		impl.setMimeType(hander, value);
	}

}
