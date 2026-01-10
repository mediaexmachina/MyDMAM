/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2025
 *
 */
package media.mexm.mydmam.asset;

import static media.mexm.mydmam.asset.DatabaseUpdateDirection.GET_FROM_DB;
import static media.mexm.mydmam.asset.DatabaseUpdateDirection.PUSH_TO_DB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.service.MediaAssetService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class MediaAssetTest {

	@Mock
	MediaAssetService service;
	@Mock
	FileEntity file;
	@Mock
	PathIndexingStorage storage;

	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	String parentPath;
	@Fake
	String basePath;
	@Fake
	String fileExt;
	@Fake
	String hashPath;
	@Fake
	String mimeType;

	String path;
	MediaAsset ma;

	@BeforeEach
	void init() {
		path = "/" + parentPath + "/" + basePath + "." + fileExt;
		ma = new MediaAsset(service, file);
		when(file.getHashPath()).thenReturn(hashPath);
		when(file.getPath()).thenReturn(path);
		when(file.getRealm()).thenReturn(realmName);
		when(file.getStorage()).thenReturn(storageName);
	}

	@Test
	void testGetHashPath() {
		assertEquals(hashPath, ma.getHashPath());
		verify(file, atLeastOnce()).getHashPath();
	}

	@Test
	void testGetName() {
		assertEquals(basePath + "." + fileExt, ma.getName());
		verify(file, atLeastOnce()).getPath();
	}

	@Test
	void testToString() {
		assertThat(ma.toString()).contains(realmName, storageName, path);
		verify(file, atLeastOnce()).getRealm();
		verify(file, atLeastOnce()).getStorage();
		verify(file, atLeastOnce()).getPath();
	}

	@Test
	void testGetMimeType() {
		when(service.updateMimeType(ma, GET_FROM_DB)).thenReturn(mimeType);
		assertEquals(mimeType, ma.getMimeType());
		verify(service, times(1)).updateMimeType(ma, GET_FROM_DB);
		assertEquals(mimeType, ma.getMimeType());
	}

	@Test
	void testSetMimeType() {
		assertThrows(NullPointerException.class, () -> ma.setMimeType(null));
		verifyNoInteractions(service);

		ma.setMimeType(mimeType);
		verify(service, times(1)).updateMimeType(ma, PUSH_TO_DB);

		assertEquals(mimeType, ma.getMimeType());
		ma.setMimeType(mimeType);
	}

	@Test
	void testGetLocalInternalFile() {
		final var pomFile = new File("pom.xml").getAbsoluteFile();
		when(storage.path()).thenReturn("file://localhost/" + pomFile.getParent());
		when(file.getPath()).thenReturn(pomFile.getName());

		final var fileStorage = ma.getLocalInternalFile(storage);
		assertNotNull(fileStorage);
		assertEquals(fileStorage, pomFile);

		verify(storage, times(1)).path();
		verify(file, times(1)).getPath();
	}

}
