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

import static java.io.File.createTempFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static media.mexm.mydmam.asset.DatabaseUpdateDirection.GET_FROM_DB;
import static media.mexm.mydmam.asset.DatabaseUpdateDirection.PUSH_TO_DB;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.apache.commons.io.FileUtils.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.asset.DeclaredRenderedFile;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.AssetSummaryEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.AssetRenderedFileRepository;
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
	@MockitoBean
	MyDMAMConfigurationProperties configuration;
	@MockitoBean
	AssetRenderedFileRepository assetRenderedFileRepository;

	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	String filePath;
	@Fake
	String mimeType;

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

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(
				fileRepository,
				assetSummaryRepository,
				assetSummaryDao,
				assetRenderedFileRepository);
	}

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

	@Nested
	class DeclareRenderedStaticFiles {

		@Mock
		DeclaredRenderedFile declaredRenderedFile;
		@Mock
		RealmConf realmConf;
		@Mock
		AssetRenderedFileEntity assetRenderedFileEntity;

		@Captor
		ArgumentCaptor<Iterable<AssetRenderedFileEntity>> entitiesToSaveCaptor;

		@Fake
		int fileId;
		@Fake
		long etag;
		@Fake
		int index;
		@Fake
		String previewType;
		@Fake
		String name;
		@Fake
		String renderedName;
		@Fake
		String relativePath;
		@Fake
		String renderedContent;

		File renderedMetadataDirectory;
		File workingFile;
		File expectedRenderedFile;
		ArrayList<AssetRenderedFileEntity> savedEntities;
		List<DeclaredRenderedFile> declaredRenderedFiles;

		@BeforeEach
		void init() throws IOException {
			renderedMetadataDirectory = getTempDirectory();
			savedEntities = new ArrayList<>();

			workingFile = createTempFile(DeclareRenderedStaticFiles.class.getSimpleName(), "workingFile");
			write(workingFile, renderedContent, UTF_8, false);

			relativePath = "/" + relativePath;
			expectedRenderedFile = new File(renderedMetadataDirectory, relativePath);
			deleteQuietly(expectedRenderedFile);

			declaredRenderedFiles = List.of(declaredRenderedFile);
			when(asset.getFile()).thenReturn(file);
			when(file.getRealm()).thenReturn(realmName);
			when(file.getId()).thenReturn(fileId);
			when(configuration.getRealmByName(realmName)).thenReturn(Optional.ofNullable(realmConf));
			when(realmConf.renderedMetadataDirectory()).thenReturn(renderedMetadataDirectory);
			when(declaredRenderedFile.name()).thenReturn(renderedName);
			when(declaredRenderedFile.makeAssetRenderedFileEntity(file, index, previewType))
					.thenReturn(assetRenderedFileEntity);
			when(declaredRenderedFile.workingFile()).thenReturn(workingFile);
			when(assetRenderedFileEntity.getEtag()).thenReturn(etag);
			when(assetRenderedFileEntity.getName()).thenReturn(renderedName);
			when(assetRenderedFileEntity.getRelativePath()).thenReturn(relativePath);
			when(assetRenderedFileRepository.getRenderedForFileByEtags(fileId, Set.of(etag)))
					.thenReturn(Set.of(assetRenderedFileEntity));
		}

		@AfterEach
		void ends() {
			deleteQuietly(workingFile);
		}

		@Test
		void testEmpty() throws IOException {
			final var result = mas.declareRenderedStaticFiles(asset, List.of(), index, previewType);
			assertThat(result).isEmpty();
		}

		@Test
		void testDeclare() throws IOException {
			final var result = mas.declareRenderedStaticFiles(asset, declaredRenderedFiles, index, previewType);
			assertThat(result).hasSize(1).containsEntry(assetRenderedFileEntity, expectedRenderedFile);

			verify(assetRenderedFileRepository, times(1)).saveAllAndFlush(entitiesToSaveCaptor.capture());
			entitiesToSaveCaptor.getValue().forEach(savedEntities::add);
			assertThat(savedEntities).hasSize(1).contains(assetRenderedFileEntity);

			assertThat(expectedRenderedFile).exists().hasContent(renderedContent);
			assertThat(workingFile).doesNotExist();

			verify(asset, atLeastOnce()).getFile();
			verify(file, atLeastOnce()).getRealm();
			verify(file, atLeastOnce()).getId();
			verify(configuration, atLeastOnce()).getRealmByName(realmName);
			verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
			verify(assetRenderedFileEntity, atLeastOnce()).getEtag();
			verify(assetRenderedFileEntity, atLeastOnce()).getName();
			verify(assetRenderedFileEntity, atLeastOnce()).getRelativePath();
			verify(declaredRenderedFile, atLeastOnce()).name();
			verify(declaredRenderedFile, atLeastOnce()).workingFile();
			verify(declaredRenderedFile, times(1)).makeAssetRenderedFileEntity(file, index, previewType);
			verify(assetRenderedFileRepository, times(1)).getRenderedForFileByEtags(fileId, Set.of(etag));
		}

		@Test
		void testTwiceSameFileName() {
			declaredRenderedFiles = List.of(declaredRenderedFile, declaredRenderedFile);

			assertThrows(IOException.class,
					() -> mas.declareRenderedStaticFiles(asset, declaredRenderedFiles, index, previewType));

			verify(asset, atLeastOnce()).getFile();
			verify(file, atLeastOnce()).getRealm();
			verify(configuration, atLeastOnce()).getRealmByName(realmName);
			verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
			verify(declaredRenderedFile, atLeastOnce()).name();
		}

		@Test
		void testCantFoundRenderedFile() {
			when(declaredRenderedFile.name()).thenReturn("NOPE");

			assertThrows(IllegalStateException.class,
					() -> mas.declareRenderedStaticFiles(asset, declaredRenderedFiles, index, previewType));

			verify(assetRenderedFileRepository, times(1)).saveAllAndFlush(entitiesToSaveCaptor.capture());
			entitiesToSaveCaptor.getValue().forEach(savedEntities::add);
			assertThat(savedEntities).hasSize(1).contains(assetRenderedFileEntity);

			verify(asset, atLeastOnce()).getFile();
			verify(file, atLeastOnce()).getRealm();
			verify(file, atLeastOnce()).getId();
			verify(configuration, atLeastOnce()).getRealmByName(realmName);
			verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
			verify(assetRenderedFileEntity, atLeastOnce()).getEtag();
			verify(assetRenderedFileEntity, atLeastOnce()).getName();
			verify(declaredRenderedFile, atLeastOnce()).name();
			verify(declaredRenderedFile, times(1)).makeAssetRenderedFileEntity(file, index, previewType);
			verify(assetRenderedFileRepository, times(1)).getRenderedForFileByEtags(fileId, Set.of(etag));
		}

		@Test
		void testRenderedFileExists() throws IOException {
			write(expectedRenderedFile, "NOPE", UTF_8, false);

			assertThrows(IOException.class,
					() -> mas.declareRenderedStaticFiles(asset, declaredRenderedFiles, index, previewType));

			verify(assetRenderedFileRepository, times(1)).saveAllAndFlush(entitiesToSaveCaptor.capture());
			entitiesToSaveCaptor.getValue().forEach(savedEntities::add);
			assertThat(savedEntities).hasSize(1).contains(assetRenderedFileEntity);

			verify(asset, atLeastOnce()).getFile();
			verify(file, atLeastOnce()).getRealm();
			verify(file, atLeastOnce()).getId();
			verify(configuration, atLeastOnce()).getRealmByName(realmName);
			verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
			verify(assetRenderedFileEntity, atLeastOnce()).getEtag();
			verify(assetRenderedFileEntity, atLeastOnce()).getName();
			verify(assetRenderedFileEntity, atLeastOnce()).getRelativePath();
			verify(declaredRenderedFile, atLeastOnce()).name();
			verify(declaredRenderedFile, times(1)).makeAssetRenderedFileEntity(file, index, previewType);
			verify(assetRenderedFileRepository, times(1)).getRenderedForFileByEtags(fileId, Set.of(etag));
		}

	}

}
