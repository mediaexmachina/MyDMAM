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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static media.mexm.mydmam.asset.MediaAsset.MTD_KEY_FULL_INDEXED_TEXT;
import static media.mexm.mydmam.asset.MediaAsset.PREVIEW_TYPE_TEXT_CONTENT;
import static media.mexm.mydmam.asset.MediaAsset.readTextFile;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.dto.KeyValueMetadataResponse;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import media.mexm.mydmam.indexer.RealmIndexer;
import media.mexm.mydmam.service.MediaAssetService;
import net.datafaker.Faker;
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
	@Mock
	DeclaredRenderedFile declaredRenderedFile2;
	@Mock
	AssetRenderedFileEntity assetRenderedFileEntity;
	@Mock
	AssetRenderedFileEntity assetRenderedFileEntity2;
	@Mock
	MimeTypeDetector mimeTypeDetector;
	@Mock
	RealmIndexer realmIndexer;
	@Mock
	FileMetadataEntity fileMetadataEntity;
	@Mock
	MetadataExtractorHandler mtdHander;

	@Captor
	ArgumentCaptor<Collection<FileMetadataEntity>> fileMetadataEntitiesCaptor;

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
	String name;
	@Fake
	int index;
	@Fake
	String previewType;

	@Fake
	String originHandler;
	@Fake
	String classifier;
	@Fake
	int layer;
	@Fake
	String key;
	@Fake
	String value;

	File workingFile;
	String path;
	File renderedFile;
	MediaAsset ma;
	DeclaredRenderedFile declaredRenderedFile;

	@BeforeEach
	void init() throws IOException {
		path = "/" + parentPath + "/" + basePath + "." + fileExt;
		renderedFile = new File(".");
		ma = new MediaAsset(service, file);
		when(file.getHashPath()).thenReturn(hashPath);
		when(file.getPath()).thenReturn(path);
		when(file.getRealm()).thenReturn(realmName);
		when(file.getStorage()).thenReturn(storageName);

		when(service.getAllRenderedFiles(hashPath, realmName))
				.thenReturn(Set.of(assetRenderedFileEntity));
		when(service.declareRenderedStaticFiles(eq(file), any()))
				.thenReturn(Map.of(assetRenderedFileEntity, renderedFile));
		when(service.getPhysicalRenderedFile(assetRenderedFileEntity, realmName))
				.thenReturn(renderedFile);
		when(service.getAllMetadatas(ma))
				.thenReturn(Set.of(fileMetadataEntity));

		workingFile = new File(".");
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

	@Test
	void testDeclareRenderedStaticFiles() throws IOException {
		verifyNoInteractions(service);

		ma.declareRenderedStaticFile(workingFile, name, false, mimeTypeDetector, index, previewType);

		declaredRenderedFile = new DeclaredRenderedFile(
				workingFile, name, false, mimeTypeDetector, index, previewType);

		verifyNoInteractions(service);

		verify(mimeTypeDetector, atLeastOnce()).getMimeType(workingFile);

		assertThat(ma.getRenderedFiles())
				.hasSize(1)
				.containsEntry(assetRenderedFileEntity, renderedFile);

		verify(service, times(1)).getAllRenderedFiles(hashPath, realmName);
		verify(service, times(1)).getPhysicalRenderedFile(assetRenderedFileEntity, realmName);
		verify(file, times(1)).getHashPath();
		verify(file, times(1)).getRealm();

		assertThat(ma.getRenderedFiles())
				.hasSize(1)
				.containsEntry(assetRenderedFileEntity, renderedFile);
	}

	@Test
	void testGetRenderedFiles() {
		when(service.getAllRenderedFiles(hashPath, realmName))
				.thenReturn(Set.of(assetRenderedFileEntity));
		when(service.getPhysicalRenderedFile(assetRenderedFileEntity, realmName))
				.thenReturn(renderedFile);

		assertThat(ma.getRenderedFiles())
				.hasSize(1)
				.containsEntry(assetRenderedFileEntity, renderedFile);
		verify(service, times(1)).getAllRenderedFiles(hashPath, realmName);
		verify(service, times(1)).getPhysicalRenderedFile(assetRenderedFileEntity, realmName);
		verify(file, times(1)).getHashPath();
		verify(file, times(1)).getRealm();

		assertThat(ma.getRenderedFiles())
				.hasSize(1)
				.containsEntry(assetRenderedFileEntity, renderedFile);
	}

	@Test
	void testGetMetadatas() {
		assertThat(ma.getMetadatas()).containsExactly(fileMetadataEntity);
		verify(service, times(1)).getAllMetadatas(ma);

		assertThat(ma.getMetadatas()).containsExactly(fileMetadataEntity);
	}

	@Test
	void testCreateFileMetadataEntry() {
		ma.createFileMetadataEntry(originHandler, classifier, layer, key, value);
		verifyNoInteractions(service);
	}

	@Test
	void testCommit_empty() throws IOException {// NOSONAR S2699
		ma.commit(Optional.ofNullable(realmIndexer));
	}

	@Test
	void testCommit() throws IOException {
		ma.declareRenderedStaticFile(workingFile, name, false, mimeTypeDetector, index, previewType);
		ma.createFileMetadataEntry(originHandler, classifier, layer, key, value);

		ma.commit(Optional.ofNullable(realmIndexer));

		verify(realmIndexer, times(1)).updateAsset(ma);

		declaredRenderedFile = new DeclaredRenderedFile(
				workingFile, name, false, mimeTypeDetector, index, previewType);
		verify(service, times(1)).declareRenderedStaticFiles(file, List.of(declaredRenderedFile));
		verify(service, times(1)).declareFileMetadatas(eq(file), fileMetadataEntitiesCaptor.capture());
		assertThat(fileMetadataEntitiesCaptor.getValue())
				.hasSize(1)
				.map(FileMetadataEntity::toKeyValueMetadataResponse)
				.first()
				.isEqualTo(new KeyValueMetadataResponse(classifier, key, value));

		verify(mimeTypeDetector, atLeastOnce()).getMimeType(workingFile);
	}

	@Test
	void testHashCode() {
		final var id = Faker.instance().random().nextInt();
		when(file.getId()).thenReturn(id);
		assertThat(ma.hashCode()).isEqualTo(Objects.hash(id));
		verify(file, times(1)).getId();
	}

	@Test
	void testEquals() {
		final var id = Faker.instance().random().nextInt();
		when(file.getId()).thenReturn(id);

		final var serviceCompared = Mockito.mock(MediaAssetService.class);
		final var fileCompared = Mockito.mock(FileEntity.class);
		final var maCompared = new MediaAsset(serviceCompared, fileCompared);
		when(fileCompared.getId()).thenReturn(id);

		when(file.getId()).thenReturn(id);
		assertThat(ma).isEqualTo(maCompared);
		assertThat(maCompared).isEqualTo(ma);

		final var id2 = Faker.instance().random().nextInt();
		when(fileCompared.getId()).thenReturn(id2);
		assertThat(ma).isNotEqualTo(maCompared);
		assertThat(maCompared).isNotEqualTo(ma);

		verify(file, atLeastOnce()).getId();
		verify(fileCompared, atLeastOnce()).getId();
		verifyNoMoreInteractions(serviceCompared, fileCompared);
	}

	@Test
	void testAddFullTextToIndex() throws IOException {
		final var textToIndex = File.createTempFile("mydmam-test-" + getClass().getName(), ".txt");
		write(textToIndex, Faker.instance().lorem().paragraphs(5).stream().collect(joining()), UTF_8);

		when(mtdHander.getMetadataOriginName()).thenReturn(originHandler);
		when(mimeTypeDetector.getMimeType(any(File.class))).thenReturn("text/plain");

		ma.addFullTextToIndex(mtdHander, classifier, layer, mimeTypeDetector, UTF_8, textToIndex);

		verify(mtdHander, atLeastOnce()).getMetadataOriginName();
		verify(mimeTypeDetector, times(1)).getMimeType(textToIndex);
		assertThat(textToIndex).doesNotExist();

		verifyNoInteractions(service);

		ma.commit(Optional.ofNullable(realmIndexer));

		verify(realmIndexer, times(1)).updateAsset(ma);

		name = originHandler + "-" + classifier + "-full-text";

		write(textToIndex, Faker.instance().lorem().paragraphs(5).stream().collect(joining()), UTF_8);
		declaredRenderedFile = new DeclaredRenderedFile(
				textToIndex,
				name,
				true,
				mimeTypeDetector,
				layer,
				PREVIEW_TYPE_TEXT_CONTENT);
		verify(service, times(1)).declareRenderedStaticFiles(file, List.of(declaredRenderedFile));
		verify(service, times(1)).declareFileMetadatas(eq(file), fileMetadataEntitiesCaptor.capture());
		assertThat(fileMetadataEntitiesCaptor.getValue())
				.hasSize(1)
				.map(FileMetadataEntity::toKeyValueMetadataResponse)
				.first()
				.isEqualTo(new KeyValueMetadataResponse(
						classifier,
						MTD_KEY_FULL_INDEXED_TEXT,
						name));

		verify(mimeTypeDetector, times(2)).getMimeType(textToIndex);
	}

	@Test
	void testGetTextContentByfileMetadata() throws IOException {
		renderedFile = File.createTempFile("mydmam-test-" + getClass().getName(), ".txt");
		final var fullText = Faker.instance().lorem().paragraphs(5).stream().collect(joining());
		write(renderedFile, fullText, UTF_8);

		when(service.getPhysicalRenderedFile(assetRenderedFileEntity, realmName)).thenReturn(renderedFile);
		when(assetRenderedFileEntity.getPreviewType()).thenReturn(PREVIEW_TYPE_TEXT_CONTENT);
		when(assetRenderedFileEntity.getName()).thenReturn(name);
		when(assetRenderedFileEntity.isGzipEncoded()).thenReturn(false);
		when(fileMetadataEntity.getValue()).thenReturn(name);
		when(fileMetadataEntity.getKey()).thenReturn(MTD_KEY_FULL_INDEXED_TEXT);

		assertThat(ma.getTextContentByfileMetadata())
				.hasSize(1)
				.containsEntry(fileMetadataEntity, fullText);

		verify(service, times(1)).getAllRenderedFiles(hashPath, realmName);
		verify(service, times(2)).getPhysicalRenderedFile(assetRenderedFileEntity, realmName);
		verify(service, times(1)).getAllMetadatas(ma);

		assertThat(ma.getTextContentByfileMetadata())
				.hasSize(1)
				.containsEntry(fileMetadataEntity, fullText);

		verify(file, atLeastOnce()).getHashPath();
		verify(file, atLeastOnce()).getRealm();
		verify(assetRenderedFileEntity, atLeastOnce()).getPreviewType();
		verify(assetRenderedFileEntity, atLeastOnce()).getName();
		verify(assetRenderedFileEntity, atLeastOnce()).isGzipEncoded();
		verify(fileMetadataEntity, atLeastOnce()).getValue();
		verify(fileMetadataEntity, atLeastOnce()).getKey();

		deleteQuietly(renderedFile);
	}

	@Test
	void testReadTextFile_plain() throws IOException {
		renderedFile = File.createTempFile("mydmam-test-" + getClass().getName(), ".txt");
		final var fullText = Faker.instance().lorem().paragraphs(5).stream().collect(joining());
		write(renderedFile, fullText, UTF_8);

		assertThat(readTextFile(renderedFile, false)).isEqualTo(fullText);
		deleteQuietly(renderedFile);
	}

	@Test
	void testReadTextFile_gz() throws IOException {
		renderedFile = File.createTempFile("mydmam-test-" + getClass().getName(), ".txt");
		final var fullText = Faker.instance().lorem().paragraphs(5).stream().collect(joining());

		try (final var fso = new GZIPOutputStream(new FileOutputStream(renderedFile))) {
			fso.write(fullText.getBytes(UTF_8));
		}

		assertThat(readTextFile(renderedFile, true)).isEqualTo(fullText);
		deleteQuietly(renderedFile);
	}

}
