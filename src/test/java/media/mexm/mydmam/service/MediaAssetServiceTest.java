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
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.apache.commons.io.FileUtils.touch;
import static org.apache.commons.io.FileUtils.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import media.mexm.mydmam.indexer.RealmIndexer;
import media.mexm.mydmam.repository.AssetRenderedFileDao;
import media.mexm.mydmam.repository.AssetRenderedFileRepository;
import media.mexm.mydmam.repository.FileMetadataDao;
import media.mexm.mydmam.repository.FileMetadataRepository;
import media.mexm.mydmam.repository.FileRepository;
import net.datafaker.Faker;
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
	AssetRenderedFileDao assetRenderedFileDao;
	@MockitoBean
	FileMetadataRepository fileMetadataRepository;
	@MockitoBean
	FileMetadataDao fileMetadataDao;
	@MockitoBean
	MyDMAMConfigurationProperties configuration;
	@MockitoBean
	AssetRenderedFileRepository assetRenderedFileRepository;
	@MockitoBean
	Indexer indexer;

	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	String filePath;
	@Fake
	String mimeType;
	@Fake
	String fileHashpath;

	@Mock
	FileAttributesReference fileAttributesReference;
	@Mock
	MediaAssetService injectedService;
	@Mock
	FileEntity file;
	@Mock
	MediaAsset asset;
	@Mock
	RealmConf realmConf;
	@Mock
	AssetRenderedFileEntity assetRenderedFileEntity;
	@Mock
	FileMetadataEntity fileMetadataEntity;

	@Autowired
	MediaAssetService mas;

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(
				fileRepository,
				assetRenderedFileDao,
				assetRenderedFileRepository,
				fileMetadataDao,
				fileMetadataRepository,
				indexer);
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

	@Nested
	class DeclareRenderedStaticFiles {

		@Mock
		DeclaredRenderedFile declaredRenderedFile;

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
			when(declaredRenderedFile.workingFile()).thenReturn(workingFile);
			when(declaredRenderedFile.index()).thenReturn(index);
			when(declaredRenderedFile.mimeType()).thenReturn(mimeType);
			when(declaredRenderedFile.previewType()).thenReturn(previewType);
			when(declaredRenderedFile.toGzip()).thenReturn(false);
			when(assetRenderedFileEntity.getEtag()).thenReturn(etag);
			when(assetRenderedFileEntity.getName()).thenReturn(renderedName);
			when(assetRenderedFileEntity.getRelativePath()).thenReturn(relativePath);
			when(assetRenderedFileRepository.getRenderedForFileByEtags(eq(fileId), any()))
					.thenReturn(Set.of(assetRenderedFileEntity));
		}

		@AfterEach
		void ends() {
			deleteQuietly(workingFile);
		}

		@Test
		void testEmpty() throws IOException {
			final var result = mas.declareRenderedStaticFiles(file, List.of());
			assertThat(result).isEmpty();
		}

		@Test
		void testDeclare() throws IOException {
			final var result = mas.declareRenderedStaticFiles(file, declaredRenderedFiles);

			assertThat(result).hasSize(1).containsEntry(assetRenderedFileEntity, expectedRenderedFile);

			verify(assetRenderedFileRepository, times(1)).saveAllAndFlush(entitiesToSaveCaptor.capture());
			entitiesToSaveCaptor.getValue().forEach(savedEntities::add);
			assertThat(savedEntities).hasSize(1);

			assertThat(expectedRenderedFile).exists().hasContent(renderedContent);
			assertThat(workingFile).doesNotExist();

			verify(file, atLeastOnce()).getRealm();
			verify(file, atLeastOnce()).getId();
			verify(configuration, atLeastOnce()).getRealmByName(realmName);
			verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
			verify(assetRenderedFileEntity, atLeastOnce()).getName();
			verify(assetRenderedFileEntity, atLeastOnce()).getRelativePath();
			verify(assetRenderedFileRepository, times(1)).getRenderedForFileByEtags(eq(fileId), any());
			verify(declaredRenderedFile, atLeastOnce()).name();
			verify(declaredRenderedFile, atLeastOnce()).workingFile();
			verify(declaredRenderedFile, atLeastOnce()).index();
			verify(declaredRenderedFile, atLeastOnce()).mimeType();
			verify(declaredRenderedFile, atLeastOnce()).previewType();
			verify(declaredRenderedFile, atLeastOnce()).toGzip();
		}

		@Test
		void testTwiceSameFileName() {
			declaredRenderedFiles = List.of(declaredRenderedFile, declaredRenderedFile);

			assertThrows(IOException.class,
					() -> mas.declareRenderedStaticFiles(file, declaredRenderedFiles));

			verify(file, atLeastOnce()).getRealm();
			verify(configuration, atLeastOnce()).getRealmByName(realmName);
			verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
			verify(declaredRenderedFile, atLeastOnce()).name();
		}

		@Test
		void testCantFoundRenderedFile() {
			when(declaredRenderedFile.name()).thenReturn("NOPE");

			assertThrows(IllegalStateException.class,
					() -> mas.declareRenderedStaticFiles(file, declaredRenderedFiles));

			verify(assetRenderedFileRepository, times(1)).saveAllAndFlush(entitiesToSaveCaptor.capture());
			entitiesToSaveCaptor.getValue().forEach(savedEntities::add);
			assertThat(savedEntities).hasSize(1);

			verify(file, atLeastOnce()).getRealm();
			verify(file, atLeastOnce()).getId();
			verify(configuration, atLeastOnce()).getRealmByName(realmName);
			verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
			verify(assetRenderedFileEntity, atLeastOnce()).getName();
			verify(declaredRenderedFile, atLeastOnce()).name();
			verify(declaredRenderedFile, atLeastOnce()).workingFile();
			verify(declaredRenderedFile, atLeastOnce()).index();
			verify(declaredRenderedFile, atLeastOnce()).mimeType();
			verify(declaredRenderedFile, atLeastOnce()).previewType();
			verify(declaredRenderedFile, atLeastOnce()).toGzip();
			verify(assetRenderedFileRepository, times(1)).getRenderedForFileByEtags(eq(fileId), any());
		}

		@Test
		void testRenderedFileExists() throws IOException {
			write(expectedRenderedFile, "NOPE", UTF_8, false);

			assertThrows(IOException.class,
					() -> mas.declareRenderedStaticFiles(file, declaredRenderedFiles));

			verify(assetRenderedFileRepository, times(1)).saveAllAndFlush(entitiesToSaveCaptor.capture());
			entitiesToSaveCaptor.getValue().forEach(savedEntities::add);
			assertThat(savedEntities).hasSize(1);

			verify(file, atLeastOnce()).getRealm();
			verify(file, atLeastOnce()).getId();
			verify(configuration, atLeastOnce()).getRealmByName(realmName);
			verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
			verify(assetRenderedFileEntity, atLeastOnce()).getName();
			verify(assetRenderedFileEntity, atLeastOnce()).getRelativePath();
			verify(declaredRenderedFile, atLeastOnce()).name();
			verify(declaredRenderedFile, atLeastOnce()).workingFile();
			verify(declaredRenderedFile, atLeastOnce()).index();
			verify(declaredRenderedFile, atLeastOnce()).mimeType();
			verify(declaredRenderedFile, atLeastOnce()).previewType();
			verify(declaredRenderedFile, atLeastOnce()).toGzip();
			verify(assetRenderedFileRepository, times(1)).getRenderedForFileByEtags(eq(fileId), any());
		}

	}

	@Test
	void testGetAllRenderedFiles() {
		when(assetRenderedFileRepository.getAllRenderedFiles(fileHashpath, realmName))
				.thenReturn(Set.of(assetRenderedFileEntity));
		assertThat(mas.getAllRenderedFiles(fileHashpath, realmName))
				.containsOnly(assetRenderedFileEntity);
		verify(assetRenderedFileRepository, times(1)).getAllRenderedFiles(fileHashpath, realmName);
	}

	@Test
	void testDeclareFileMetadatas() throws IOException {
		final var list = List.of(fileMetadataEntity);
		when(fileMetadataDao.addUpdateEntries(file, list)).thenReturn(list);
		final var result = mas.declareFileMetadatas(file, list);
		assertThat(result).isEqualTo(list);
		verify(fileMetadataDao, times(1)).addUpdateEntries(file, list);
	}

	@Test
	void testGetAllMetadatas() {
		final var set = Set.of(fileMetadataEntity);

		when(asset.getFile()).thenReturn(file);
		when(fileMetadataRepository.getByFile(file)).thenReturn(set);

		final var result = mas.getAllMetadatas(asset);
		assertThat(result).isEqualTo(set);

		verify(asset, times(1)).getFile();
		verify(fileMetadataRepository, times(1)).getByFile(file);
	}

	@Nested
	class GetPhysicalRenderedFile {

		File renderedMetadataDirectory;
		File tempFile;

		@BeforeEach
		void init() throws IOException {
			renderedMetadataDirectory = getTempDirectory();
			tempFile = File.createTempFile(
					"mydmam-" + getClass().getSimpleName(),
					"physicalRenderedFile",
					renderedMetadataDirectory);
			forceDelete(tempFile);

			when(configuration.getRealmByName(realmName)).thenReturn(Optional.ofNullable(realmConf));
			when(realmConf.renderedMetadataDirectory()).thenReturn(renderedMetadataDirectory);
			when(assetRenderedFileEntity.getRelativePath()).thenReturn(tempFile.getName());
		}

		@AfterEach
		void ends() {
			deleteQuietly(tempFile);
			verify(configuration, atLeastOnce()).getRealmByName(realmName);
			verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
		}

		@Test
		void testGetFile() throws IOException {
			write(tempFile, Faker.instance().lorem().paragraph(5), UTF_8);
			when(assetRenderedFileEntity.getLength()).thenReturn(tempFile.length());

			final var result = mas.getPhysicalRenderedFile(assetRenderedFileEntity, realmName);
			assertThat(result).isEqualTo(tempFile);

			verify(assetRenderedFileEntity, atLeastOnce()).getRelativePath();
			verify(assetRenderedFileEntity, atLeastOnce()).getLength();
		}

		@Test
		void testInvalidSize() throws IOException {
			touch(tempFile);
			when(assetRenderedFileEntity.getLength()).thenReturn(1l);
			assertThrows(UncheckedIOException.class,
					() -> mas.getPhysicalRenderedFile(assetRenderedFileEntity, realmName));

			verify(assetRenderedFileEntity, atLeastOnce()).getRelativePath();
			verify(assetRenderedFileEntity, atLeastOnce()).getLength();
		}

		@Test
		void testNotFound() {
			assertThrows(UncheckedIOException.class,
					() -> mas.getPhysicalRenderedFile(assetRenderedFileEntity, realmName));

			verify(assetRenderedFileEntity, atLeastOnce()).getRelativePath();
		}
	}

	@Nested
	class ResetDetectedMetadatas {

		@Fake
		int id;
		@Mock
		RealmIndexer realmIndexer;
		@Captor
		ArgumentCaptor<MediaAsset> mediaAssetCaptor;

		Set<Integer> fileIdsToReset;
		File renderedMetadataDirectory;
		File tempFile;

		@BeforeEach
		void init() throws IOException {
			fileIdsToReset = Set.of(id);

			renderedMetadataDirectory = getTempDirectory();
			tempFile = File.createTempFile(
					"mydmam-" + getClass().getSimpleName(),
					"physicalRenderedFile",
					renderedMetadataDirectory);
			forceDelete(tempFile);

			when(configuration.getRealmByName(realmName)).thenReturn(Optional.ofNullable(realmConf));
			when(realmConf.renderedMetadataDirectory()).thenReturn(renderedMetadataDirectory);
			when(asset.getFile()).thenReturn(file);
			when(file.getId()).thenReturn(id);
			when(file.getRealm()).thenReturn(realmName);
			when(assetRenderedFileDao.deleteRenderedFilesByFileId(fileIdsToReset))
					.thenReturn(Map.of(realmName, Set.of(tempFile.getName())));
			when(fileRepository.getByIds(fileIdsToReset)).thenReturn(Set.of(file));
			when(indexer.getIndexerByRealm(realmName)).thenReturn(Optional.ofNullable(realmIndexer));
		}

		@Test
		void empty() {
			final var result = mas.resetDetectedMetadatas(List.of(), injectedService);
			assertThat(result).isEmpty();
		}

		@Test
		void test() {
			final var result = mas.resetDetectedMetadatas(List.of(asset), injectedService);
			assertThat(result).hasSize(1);
			assertThat(result.iterator().next().getFile()).isEqualTo(file);

			verify(asset, atLeastOnce()).getFile();
			verify(file, atLeastOnce()).getId();
			verify(file, atLeastOnce()).getRealm();
			verify(fileMetadataRepository, times(1)).deleteByFileId(fileIdsToReset);
			verify(assetRenderedFileDao, times(1)).deleteRenderedFilesByFileId(fileIdsToReset);
			verify(configuration, atLeastOnce()).getRealmByName(realmName);
			verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
			verify(fileRepository, times(1)).getByIds(fileIdsToReset);
			verify(indexer, atLeastOnce()).getIndexerByRealm(realmName);
			verify(realmIndexer, times(1)).resetAsset(mediaAssetCaptor.capture());
			assertThat(mediaAssetCaptor.getValue().getFile()).isEqualTo(file);
		}

	}

}
