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

import static media.mexm.mydmam.asset.FileMetadataResolutionTrait.MTD_HEIGHT_KEY;
import static media.mexm.mydmam.asset.FileMetadataResolutionTrait.MTD_WIDTH_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
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
class FileMetadataResolutionTraitTest {

	class Impl implements FileMetadataResolutionTrait {

		final FileMetadataResolutionTraitTest ref;
		final Map<String, String> metadataEntries;

		Impl(final FileMetadataResolutionTraitTest ref) {
			this.ref = ref;
			metadataEntries = new HashMap<>();
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
		public Optional<String> getMetadataValue(final String classifier, final String key) {
			assertEquals(MTD_TECHNICAL_CLASSIFIER, classifier);

			if (MTD_WIDTH_KEY.equals(key)) {
				return Optional.ofNullable(String.valueOf(ref.width));
			} else if (MTD_HEIGHT_KEY.equals(key)) {
				return Optional.ofNullable(String.valueOf(ref.height));
			}

			return Optional.empty();
		}

		@Override
		public void createFileMetadataEntry(final ActivityHandler hander,
											final String classifier,
											final int layer,
											final String key,
											final String value) {
			assertEquals(ref.hander, hander);
			assertEquals(MTD_TECHNICAL_CLASSIFIER, classifier);
			assertEquals(0, layer);
			metadataEntries.put(key, value);
		}

	}

	@Mock
	ActivityHandler hander;

	@Fake(min = 1, max = 1000)
	int width;
	@Fake(min = 1, max = 1000)
	int height;

	Impl impl;

	@BeforeEach
	void init() {
		impl = new Impl(this);
	}

	@Test
	void testGetWidth() {
		assertEquals(width, impl.getWidth());
	}

	@Test
	void testGetHeight() {
		assertEquals(height, impl.getHeight());
	}

	@Test
	void testSetResolution() {
		impl.setResolution(hander, width, height);

		assertThat(impl.metadataEntries)
				.hasSize(2)
				.containsEntry(MTD_WIDTH_KEY, String.valueOf(width))
				.containsEntry(MTD_HEIGHT_KEY, String.valueOf(height));
	}

	@Test
	void testSetResolution_invalid() {
		impl.setResolution(hander, -width, -height);
		impl.setResolution(hander, width, -height);
		impl.setResolution(hander, -width, height);
		assertThat(impl.metadataEntries).isEmpty();

	}

}
