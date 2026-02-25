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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.entity.FileMetadataEntity;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class AccessFileMetadataEntryTraitTest {

	class Impl implements AccessFileMetadataEntryTrait {

		final Set<FileMetadataEntity> metadatas;

		Impl(final Set<FileMetadataEntity> metadatas) {
			this.metadatas = metadatas;
		}

		@Override
		public Set<FileMetadataEntity> getMetadatas() {
			return metadatas;
		}

	}

	@Mock
	FileMetadataEntity fileMetadataEntity;

	@Fake
	String classifier;
	@Fake
	String key;
	@Fake
	int layer;
	@Fake
	String value;

	Impl afmet;

	@BeforeEach
	void init() {
		afmet = new Impl(Set.of(fileMetadataEntity));
	}

	@Test
	void testGetMetadataValueStringStringInt() {
		when(fileMetadataEntity.getClassifier()).thenReturn(classifier);
		when(fileMetadataEntity.getLayer()).thenReturn(layer);
		when(fileMetadataEntity.getKey()).thenReturn(key);
		when(fileMetadataEntity.getValue()).thenReturn(value);

		assertThat(afmet.getMetadataValue(classifier, key, layer)).contains(value);

		verify(fileMetadataEntity, times(1)).getClassifier();
		verify(fileMetadataEntity, times(1)).getLayer();
		verify(fileMetadataEntity, times(1)).getKey();
		verify(fileMetadataEntity, times(1)).getValue();
	}

	@Test
	void testGetMetadataValueStringString() {
		when(fileMetadataEntity.getClassifier()).thenReturn(classifier);
		when(fileMetadataEntity.getLayer()).thenReturn(0);
		when(fileMetadataEntity.getKey()).thenReturn(key);
		when(fileMetadataEntity.getValue()).thenReturn(value);

		assertThat(afmet.getMetadataValue(classifier, key)).contains(value);

		verify(fileMetadataEntity, times(1)).getClassifier();
		verify(fileMetadataEntity, times(1)).getLayer();
		verify(fileMetadataEntity, times(1)).getKey();
		verify(fileMetadataEntity, times(1)).getValue();
	}

}
