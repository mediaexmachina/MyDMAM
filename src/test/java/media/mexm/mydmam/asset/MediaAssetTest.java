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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
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
	@Fake
	String name;
	@Fake
	int index;
	@Fake
	String previewType;

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
		when(service.declareRenderedStaticFiles(eq(ma), any()))
				.thenReturn(Map.of(assetRenderedFileEntity, renderedFile));
		when(service.getPhysicalRenderedFile(assetRenderedFileEntity, realmName))
				.thenReturn(renderedFile);

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
	void testCommit_empty() throws IOException {// NOSONAR S2699
		ma.commit(Optional.ofNullable(realmIndexer));
	}

	@Test
	void testCommit() throws IOException {
		ma.declareRenderedStaticFile(workingFile, name, false, mimeTypeDetector, index, previewType);
		ma.commit(Optional.ofNullable(realmIndexer));

		assertThat(ma.getRenderedFiles())
				.hasSize(1)
				.containsEntry(assetRenderedFileEntity, renderedFile);

		verify(realmIndexer, times(1)).updateAsset(ma);

		declaredRenderedFile = new DeclaredRenderedFile(
				workingFile, name, false, mimeTypeDetector, index, previewType);
		verify(service, times(1)).declareRenderedStaticFiles(ma, List.of(declaredRenderedFile));
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

}
