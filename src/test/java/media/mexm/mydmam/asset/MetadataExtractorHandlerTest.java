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

import java.io.File;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.HandlingResult;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class MetadataExtractorHandlerTest {

	record TestMetadataExtractorHandler(Set<String> managedMimeTypes) implements MetadataExtractorHandler {

		@Override
		public Set<String> getManagedMimeTypes() {
			return managedMimeTypes;
		}

		@Override
		public boolean canHandle(final MediaAsset asset,
								 final ActivityEventType eventType,
								 final RealmStorageConfiguredEnv storedOn) {
			throw new UnsupportedOperationException();
		}

		@Override
		public HandlingResult handle(final MediaAsset asset,
									 final ActivityEventType eventType,
									 final RealmStorageConfiguredEnv storedOn) throws Exception {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> getProducedPreviewTypes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getMetadataOriginName() {
			throw new UnsupportedOperationException();
		}

	}

	@Mock
	MediaAsset asset;
	@Mock
	RealmStorageConfiguredEnv storedOn;
	@Mock
	RealmConf realm;
	@Mock
	FileEntity fileEntity;

	@Fake
	String mimeType;
	@Fake
	String fileName;
	@Fake
	int id;

	TestMetadataExtractorHandler meh;

	@BeforeEach
	void init() {
		meh = new TestMetadataExtractorHandler(Set.of(mimeType));
	}

	@Test
	void testGetManagedMimeTypes() {
		assertThat(meh.getManagedMimeTypes()).isEqualTo(Set.of(mimeType));
	}

	@Test
	final void testCanHandleMimeType() {
		when(asset.getMimeType()).thenReturn(Optional.ofNullable(mimeType));
		assertThat(meh.canHandleMimeType(asset)).isTrue();

		when(asset.getMimeType()).thenReturn(Optional.ofNullable("other"));
		assertThat(meh.canHandleMimeType(asset)).isFalse();

		verify(asset, times(2)).getMimeType();
	}

	@Test
	void testMakeWorkingFile() {
		final var workingFile = id + "-" + fileName;

		when(storedOn.realm()).thenReturn(realm);
		when(realm.makeWorkingFile(workingFile, meh.getClass())).thenReturn(new File(fileName));
		when(asset.getFile()).thenReturn(fileEntity);
		when(fileEntity.getId()).thenReturn(id);

		meh.makeWorkingFile(fileName, asset, storedOn);

		verify(storedOn, times(1)).realm();
		verify(realm, times(1)).makeWorkingFile(workingFile, meh.getClass());
		verify(asset, times(1)).getFile();
		verify(fileEntity, times(1)).getId();
	}

}
