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
package media.mexm.mydmam.service;

import static media.mexm.mydmam.asset.DatabaseUpdateDirection.GET_FROM_DB;
import static media.mexm.mydmam.asset.DatabaseUpdateDirection.PUSH_TO_DB;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.entity.AssetSummaryEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.AssetSummaryDao;
import media.mexm.mydmam.repository.AssetSummaryRepository;
import media.mexm.mydmam.repository.FileRepository;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.transfertfiles.FileAttributesReference;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class MediaAssetServiceTest {

	@MockitoBean
	FileRepository fileRepository;
	@MockitoBean
	AssetSummaryRepository assetSummaryRepository;
	@MockitoBean
	AssetSummaryDao assetSummaryDao;

	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	String filePath;

	@Mock
	FileAttributesReference fileAttributesReference;
	@Mock
	MediaAssetService injectedService;
	@Mock
	FileEntity file;
	@Mock
	MediaAsset asset;
	@Mock
	AssetSummaryEntity assetSummaryEntity;

	@Autowired
	MediaAssetService mas;

	@Test
	void testGetFromWatchfolder() {
		final var hashPath = hashPath(realmName, storageName, filePath);

		when(fileAttributesReference.getPath()).thenReturn(filePath);
		when(fileRepository.getByHashPath(hashPath, realmName)).thenReturn(file);

		final var mediaAsset = mas.getFromWatchfolder(realmName, storageName, fileAttributesReference, injectedService);
		assertEquals(file, mediaAsset.getFile());
		assertEquals(injectedService, mediaAsset.getService());

		verify(fileAttributesReference, times(1)).getPath();
		verify(fileRepository, times(1)).getByHashPath(hashPath, realmName);
	}

	@Test
	void testGetFromFileEntry() {
		final var mediaAsset = mas.getFromFileEntry(file, injectedService);
		assertEquals(file, mediaAsset.getFile());
		assertEquals(injectedService, mediaAsset.getService());
	}

	@Test
	void testPurgeAssetArtefacts() {
		assertDoesNotThrow(() -> mas.purgeAssetArtefacts(realmName, storageName, fileAttributesReference));
	}

	@Fake
	String mimeType;

	@Test
	void testUpdateMimeType_push() {
		when(asset.getFile()).thenReturn(file);
		when(asset.getMimeType()).thenReturn(mimeType);

		assertEquals(mimeType, mas.updateMimeType(asset, PUSH_TO_DB));

		verify(assetSummaryDao, times(1)).updateMimeType(file, mimeType);
		verify(asset, atLeastOnce()).getFile();
		verify(asset, atLeastOnce()).getMimeType();
	}

	@Test
	void testUpdateMimeType_get() {
		when(asset.getFile()).thenReturn(file);
		when(assetSummaryDao.getForFile(file)).thenReturn(true);
		when(file.getAssetSummary()).thenReturn(assetSummaryEntity);
		when(assetSummaryEntity.getMimeType()).thenReturn(mimeType);

		assertEquals(mimeType, mas.updateMimeType(asset, GET_FROM_DB));

		verify(asset, atLeastOnce()).getFile();
		verify(assetSummaryDao, times(1)).getForFile(file);
		verify(file, times(1)).getAssetSummary();
		verify(assetSummaryEntity, times(1)).getMimeType();

	}

	@Test
	void testUpdateMimeType_get_empty() {
		when(asset.getFile()).thenReturn(file);
		when(assetSummaryDao.getForFile(file)).thenReturn(false);

		assertNull(mas.updateMimeType(asset, GET_FROM_DB));

		verify(asset, atLeastOnce()).getFile();
		verify(assetSummaryDao, times(1)).getForFile(file);
	}

}
