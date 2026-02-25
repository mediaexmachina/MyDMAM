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
 * Copyright (C) Media ex Machina 2025
 *
 */
package media.mexm.mydmam.controller;

import static media.mexm.mydmam.dto.StorageCategory.DAS;
import static media.mexm.mydmam.dto.StorageCategory.EXTERNAL;
import static media.mexm.mydmam.dto.StorageCategory.NAS;
import static media.mexm.mydmam.dto.StorageStateClass.OFFLINE;
import static media.mexm.mydmam.dto.StorageStateClass.ONLINE;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static media.mexm.mydmam.tools.SortOrder.none;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.configuration.TechnicalName;
import media.mexm.mydmam.dto.FileItemResponse;
import media.mexm.mydmam.dto.FileResponse;
import media.mexm.mydmam.dto.KeyValueMetadataResponse;
import media.mexm.mydmam.dto.RealmListResponse;
import media.mexm.mydmam.dto.RenderedFileResponse;
import media.mexm.mydmam.dto.StorageCategory;
import media.mexm.mydmam.dto.StorageListResponse;
import media.mexm.mydmam.dto.StorageState;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import media.mexm.mydmam.repository.AssetRenderedFileDao;
import media.mexm.mydmam.repository.FileDao;
import media.mexm.mydmam.repository.FileMetadataDao;
import media.mexm.mydmam.repository.FileRepository;
import media.mexm.mydmam.repository.FileSort;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class FileSystemControllerTest {

	private static final RequestMapping REQUEST_MAPPING = FileSystemController.class.getAnnotation(
			RequestMapping.class);
	private static final String BASE_MAPPING = REQUEST_MAPPING.value()[0];
	private static final ResultMatcher CONTENT_TYPE = content().contentType(REQUEST_MAPPING.produces()[0]);
	private static final ResultMatcher STATUS_OK = status().isOk();
	private static final ResultMatcher STATUS_BAD_REQUEST = status().isBadRequest();
	private static final StorageState emptyStorageState = new StorageState("", "", EXTERNAL, OFFLINE);

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;

	@MockitoBean
	MyDMAMConfigurationProperties conf;
	@MockitoBean
	FileRepository fileRepository;
	@MockitoBean
	FileDao fileDao;
	@MockitoBean
	AuditTrail auditTrail;
	@MockitoBean
	AssetRenderedFileDao assetRenderedFileDao;
	@MockitoBean
	FileMetadataDao fileMetadataDao;

	@Mock
	HttpServletRequest request;
	@Mock
	FileEntity file;
	@Mock
	FileEntity fileChildren0;
	@Mock
	FileEntity fileChildren1;

	@Fake
	String realm;
	@Fake
	String realm1;
	@Fake
	String storage;
	@Fake
	String storage1;
	@Fake(min = 0, max = 10000)
	int skip;
	@Fake(min = 0, max = 100)
	int limit;
	@Fake
	String hashPath;
	@Fake
	String basePath;
	@Fake
	String baseName;
	@Fake
	int totalSize;
	@Fake
	long length;
	@Fake
	String specifications;

	HttpHeaders baseHeaders;
	long modified;
	String parentHashPath;
	FileSort fileSort;
	int dirListMaxSize;

	@BeforeEach
	void init() {
		baseHeaders = new HttpHeaders();
		baseHeaders.setContentType(APPLICATION_JSON);
		modified = System.currentTimeMillis();
		hashPath = hashPath.toLowerCase();
		parentHashPath = hashPath(realm, storage, "/" + basePath);
		fileSort = new FileSort(none, none, none, none);
		dirListMaxSize = 100;

		when(conf.dirListMaxSize()).thenReturn(dirListMaxSize);

		when(file.getHashPath()).thenReturn(hashPath);
		when(file.getLength()).thenReturn(0l);
		when(file.getModified()).thenReturn(new Timestamp(modified));
		when(file.getParentHashPath()).thenReturn(parentHashPath);
		when(file.getPath()).thenReturn("/" + basePath + "/" + baseName);
		when(file.getRealm()).thenReturn(realm);
		when(file.getStorage()).thenReturn(storage);
		when(file.isDirectory()).thenReturn(true);
		when(file.isWatchMarkedAsDone()).thenReturn(true);

		when(fileChildren0.getHashPath()).thenReturn(hashPath + "filechildren0");
		when(fileChildren0.getLength()).thenReturn(length);
		when(fileChildren0.getModified()).thenReturn(new Timestamp(modified));
		when(fileChildren0.getParentHashPath()).thenReturn(hashPath);
		when(fileChildren0.getPath()).thenReturn("/" + basePath + "/" + baseName + "/filechildren0");
		when(fileChildren0.getRealm()).thenReturn(realm);
		when(fileChildren0.getStorage()).thenReturn(storage);
		when(fileChildren0.isDirectory()).thenReturn(false);
		when(fileChildren0.isWatchMarkedAsDone()).thenReturn(true);

		when(fileChildren1.getHashPath()).thenReturn(hashPath + "filechildren1");
		when(fileChildren1.getLength()).thenReturn(length);
		when(fileChildren1.getModified()).thenReturn(new Timestamp(modified));
		when(fileChildren1.getParentHashPath()).thenReturn(hashPath);
		when(fileChildren1.getPath()).thenReturn("/" + basePath + "/" + baseName + "/filechildren1");
		when(fileChildren1.getRealm()).thenReturn(realm);
		when(fileChildren1.getStorage()).thenReturn(storage);
		when(fileChildren1.isDirectory()).thenReturn(false);
		when(fileChildren1.isWatchMarkedAsDone()).thenReturn(true);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(
				fileRepository,
				fileDao,
				auditTrail,
				assetRenderedFileDao,
				fileMetadataDao);
	}

	@Test
	void testGetRealms() throws Exception {
		when(fileRepository.getAllRealms()).thenReturn(Set.of(realm, realm1));

		final var content = mvc.perform(get(BASE_MAPPING + "/list")
				.headers(baseHeaders))
				.andExpect(STATUS_OK)
				.andExpect(CONTENT_TYPE)
				.andExpect(jsonPath("$.realms").exists())
				.andReturn()
				.getResponse()
				.getContentAsString();

		final var response = objectMapper.readValue(content, RealmListResponse.class);
		assertThat(response.realms()).containsExactlyInAnyOrder(realm, realm1);

		verify(fileRepository, times(1)).getAllRealms();
	}

	@Nested
	class GetStorages {

		@Mock
		RealmConf realmConf;
		@Mock
		PathIndexingStorage piStorageIndexedInDb;
		@Mock
		PathIndexingStorage piStorageIndexedOutOfDb;

		@Fake
		String storageNameIndexedOutOfDb;
		@Fake
		String description;
		@Fake
		String location;
		@Fake
		boolean storageCategoryNas;

		StorageCategory storageCategory;
		StorageState expectedStorageState;

		@BeforeEach
		void init() {
			when(fileRepository.getAllStoragesByRealm(realm)).thenReturn(Set.of(storage, storage1));
		}

		@Test
		void testNoState() throws Exception {
			final var content = mvc.perform(get(BASE_MAPPING + "/list/" + realm)
					.headers(baseHeaders))
					.andExpect(STATUS_OK)
					.andExpect(CONTENT_TYPE)
					.andExpect(jsonPath("$.realm").exists())
					.andExpect(jsonPath("$.storages").exists())
					.andExpect(jsonPath("$.storageStates").exists())
					.andReturn()
					.getResponse()
					.getContentAsString();

			final var response = objectMapper.readValue(content, StorageListResponse.class);
			assertThat(response.realm()).isEqualTo(realm);
			assertThat(response.storages()).containsExactlyInAnyOrder(storage, storage1);

			assertThat(response.storageStates())
					.containsKeys(storage, storage1)
					.containsEntry(storage, emptyStorageState)
					.containsEntry(storage1, emptyStorageState)
					.size().isEqualTo(2);

			verify(fileRepository, times(1)).getAllStoragesByRealm(realm);
			verify(conf, atLeastOnce()).getRealmByName(realm);
		}

		@Test
		void testWithState() throws Exception {
			when(conf.getRealmByName(realm)).thenReturn(Optional.ofNullable(realmConf));
			when(realmConf.storages()).thenReturn(Map.of(
					new TechnicalName(storage), piStorageIndexedInDb,
					new TechnicalName(storageNameIndexedOutOfDb), piStorageIndexedOutOfDb));

			when(piStorageIndexedInDb.location()).thenReturn(location);
			when(piStorageIndexedOutOfDb.location()).thenReturn(location);

			when(piStorageIndexedInDb.description()).thenReturn(description);
			when(piStorageIndexedOutOfDb.description()).thenReturn(description);

			storageCategory = storageCategoryNas ? NAS : DAS;
			when(piStorageIndexedInDb.getCategory()).thenReturn(storageCategory);
			when(piStorageIndexedOutOfDb.getCategory()).thenReturn(storageCategory);

			when(piStorageIndexedInDb.getStorageStateClass()).thenReturn(ONLINE);
			when(piStorageIndexedOutOfDb.getStorageStateClass()).thenReturn(ONLINE);

			final var content = mvc.perform(get(BASE_MAPPING + "/list/" + realm)
					.headers(baseHeaders))
					.andExpect(STATUS_OK)
					.andExpect(CONTENT_TYPE)
					.andExpect(jsonPath("$.realm").exists())
					.andExpect(jsonPath("$.storages").exists())
					.andExpect(jsonPath("$.storageStates").exists())
					.andReturn()
					.getResponse()
					.getContentAsString();

			final var response = objectMapper.readValue(content, StorageListResponse.class);
			assertThat(response.realm()).isEqualTo(realm);
			assertThat(response.storages()).containsExactlyInAnyOrder(storage, storage1);

			expectedStorageState = new StorageState(description, location, storageCategory, ONLINE);
			assertThat(response.storageStates())
					.containsKeys(storage, storage1, storageNameIndexedOutOfDb)
					.containsEntry(storage, expectedStorageState)
					.containsEntry(storage1, emptyStorageState)
					.containsEntry(storageNameIndexedOutOfDb, expectedStorageState)
					.size().isEqualTo(3);

			verify(fileRepository, times(1)).getAllStoragesByRealm(realm);
			verify(conf, times(1)).getRealmByName(realm);
			verify(realmConf, times(1)).storages();

			Stream.of(piStorageIndexedInDb,
					piStorageIndexedOutOfDb)
					.forEach(piStorage -> {
						verify(piStorage, times(1)).location();
						verify(piStorage, times(1)).description();
						verify(piStorage, times(1)).getCategory();
						verify(piStorage, times(1)).getStorageStateClass();
					});

		}
	}

	@Test
	void testList_badRealm() throws Exception {
		when(fileDao.getByParentHashPath(any(), anyInt(), anyInt(), any())).thenReturn(List.of());
		when(fileDao.countParentHashPathItems(any(), any(), any())).thenReturn(0);
		when(fileRepository.getByHashPath(any(String.class), anyString())).thenReturn(file);

		when(file.getRealm()).thenReturn(realm1);

		mvc.perform(get(BASE_MAPPING + "/list/" + realm + "/" + storage + "/" + hashPath)
				.headers(baseHeaders))
				.andExpect(STATUS_BAD_REQUEST);

		verify(file, atLeastOnce()).getRealm();
		verify(file, atLeastOnce()).getPath();
		verify(conf, atLeastOnce()).instancename();
		verify(conf, atLeastOnce()).infra();
		verify(conf, atLeastOnce()).dirListMaxSize();
		verify(fileRepository, times(1)).getByHashPath(hashPath.toLowerCase(), realm);
		verify(fileDao, times(1)).getByParentHashPath(
				hashPath.toLowerCase(),
				0,
				dirListMaxSize,
				Optional.ofNullable(new FileSort(none, none, none, none)));
		verify(fileDao, times(1)).countParentHashPathItems(realm, storage, hashPath.toLowerCase());
	}

	@Test
	void testListRoot() throws Exception {
		final var computedHashPath = hashPath(realm, storage, "/");

		when(fileDao.getByParentHashPath(any(), anyInt(), anyInt(), any())).thenReturn(List.of());
		when(fileDao.countParentHashPathItems(any(), any(), any())).thenReturn(0);
		when(fileRepository.getByHashPath(any(String.class), anyString())).thenReturn(file);

		final var content = mvc.perform(get(BASE_MAPPING + "/list/" + realm + "/" + storage)
				.headers(baseHeaders)
				.queryParam("skip", String.valueOf(skip))
				.queryParam("limit", String.valueOf(limit)))
				.andExpect(STATUS_OK)
				.andExpect(CONTENT_TYPE)
				.andExpect(jsonPath("$.realm").exists())
				.andExpect(jsonPath("$.storage").exists())
				.andExpect(jsonPath("$.currentItem").exists())
				.andExpect(jsonPath("$.list").exists())
				.andExpect(jsonPath("$.metadatas").exists())
				.andReturn()
				.getResponse()
				.getContentAsString();

		final var response = objectMapper.readValue(content, FileResponse.class);

		assertThat(response.realm()).isEqualTo(realm);
		assertThat(response.storage()).isEqualTo(storage);
		assertThat(response.currentItem())
				.isEqualTo(new FileItemResponse(true, baseName, hashPath, modified, -1, false));
		assertThat(response.path()).isEqualTo("/" + basePath + "/" + baseName);
		assertThat(response.parentHashPath()).isEqualTo(parentHashPath);
		assertThat(response.listSize()).isZero();
		assertThat(response.skipCount()).isEqualTo(skip);
		assertThat(response.total()).isZero();
		assertThat(response.list()).isEmpty();
		assertThat(response.metadatas()).isEmpty();

		verify(fileDao, times(1)).getByParentHashPath(computedHashPath, skip, limit, Optional.ofNullable(fileSort));
		verify(fileDao, times(1)).countParentHashPathItems(realm, storage, computedHashPath);
		verify(fileRepository, times(1)).getByHashPath(computedHashPath, realm);

		verify(file, atLeastOnce()).getHashPath();
		verify(file, atLeastOnce()).getModified();
		verify(file, atLeastOnce()).getPath();
		verify(file, atLeastOnce()).getRealm();
		verify(file, atLeastOnce()).getStorage();
		verify(file, atLeastOnce()).isDirectory();

		verify(conf, atLeast(1)).dirListMaxSize();
	}

	@Nested
	class Lists {

		@Mock
		AssetRenderedFileEntity assetRenderedFileEntity;
		@Mock
		FileMetadataEntity fileMetadataEntity;

		@Fake
		String previewType;
		@Fake
		String name;

		@Fake
		String classifier;
		@Fake
		String key;
		@Fake
		String value;

		@Fake
		int indexLayer;

		@BeforeEach
		void init() {
			when(fileDao.getByParentHashPath(any(), anyInt(), anyInt(), any()))
					.thenReturn(List.of(fileChildren0, fileChildren1));
			when(fileDao.countParentHashPathItems(realm, storage, hashPath)).thenReturn(totalSize);
			when(fileRepository.getByHashPath(hashPath, realm)).thenReturn(file);

		}

		@AfterEach
		void ends() {
			verify(fileDao, times(1)).getByParentHashPath(hashPath, skip, limit, Optional.ofNullable(fileSort));
			verify(fileDao, times(1)).countParentHashPathItems(realm, storage, hashPath);
			verify(fileRepository, times(1)).getByHashPath(hashPath, realm);

			for (final var f : Set.of(file, fileChildren0, fileChildren1)) {
				verify(f, atLeastOnce()).getHashPath();
				verify(f, atLeastOnce()).getModified();
				verify(f, atLeastOnce()).getPath();
				verify(f, atLeastOnce()).getRealm();
				verify(f, atLeastOnce()).getStorage();
				verify(f, atLeastOnce()).isDirectory();
			}

			for (final var f : Set.of(fileChildren0, fileChildren1)) {
				verify(f, atLeastOnce()).getLength();
				verify(f, atLeastOnce()).isWatchMarkedAsDone();
			}

			verify(conf, atLeast(1)).dirListMaxSize();
		}

		private void checkResponse(final FileResponse response) {
			assertThat(response.realm()).isEqualTo(realm);
			assertThat(response.storage()).isEqualTo(storage);
			assertThat(response.currentItem())
					.isEqualTo(new FileItemResponse(true, baseName, hashPath, modified, -1, false));
			assertThat(response.path()).isEqualTo("/" + basePath + "/" + baseName);
			assertThat(response.parentHashPath()).isEqualTo(parentHashPath);
			assertThat(response.listSize()).isEqualTo(2);
			assertThat(response.skipCount()).isEqualTo(skip);
			assertThat(response.total()).isEqualTo(totalSize);
			assertThat(response.list()).size().isEqualTo(2);
			assertTrue(response.list().stream().anyMatch(f -> f.hashPath().equals(hashPath + "filechildren0")));
			assertTrue(response.list().stream().anyMatch(f -> f.hashPath().equals(hashPath + "filechildren1")));
		}

		@Test
		void testList() throws Exception {
			final var content = mvc.perform(get(BASE_MAPPING + "/list/" + realm + "/" + storage + "/" + hashPath)
					.headers(baseHeaders)
					.queryParam("skip", String.valueOf(skip))
					.queryParam("limit", String.valueOf(limit)))
					.andExpect(STATUS_OK)
					.andExpect(CONTENT_TYPE)
					.andExpect(jsonPath("$.realm").exists())
					.andExpect(jsonPath("$.storage").exists())
					.andExpect(jsonPath("$.currentItem").exists())
					.andExpect(jsonPath("$.list").exists())
					.andExpect(jsonPath("$.metadatas").exists())
					.andReturn()
					.getResponse()
					.getContentAsString();

			final var response = objectMapper.readValue(content, FileResponse.class);
			checkResponse(response);
			assertThat(response.metadatas()).isEmpty();
		}

		@Test
		void testList_withMetadatas() throws Exception {
			when(fileChildren0.getId()).thenReturn(0);
			when(fileChildren1.getId()).thenReturn(1);

			when(fileMetadataDao.getFileMetadatasByFileIds(Set.of(0, 1), realm))
					.thenReturn(Map.of(hashPath, Set.of(fileMetadataEntity)));
			when(assetRenderedFileDao.getRenderedFilesByFileId(Set.of(0, 1), realm))
					.thenReturn(Map.of(hashPath, Set.of(assetRenderedFileEntity)));

			final var keyValueMetadataResponse = new KeyValueMetadataResponse(classifier, key, value);
			when(fileMetadataEntity.toKeyValueMetadataResponse())
					.thenReturn(keyValueMetadataResponse);
			when(fileMetadataEntity.getLayer())
					.thenReturn(indexLayer);

			final var renderedReponse = new RenderedFileResponse(previewType, name);
			when(assetRenderedFileEntity.toRenderedReponse())
					.thenReturn(renderedReponse);
			when(assetRenderedFileEntity.getIndexref())
					.thenReturn(indexLayer);

			final var content = mvc.perform(get(BASE_MAPPING + "/list/" + realm + "/" + storage + "/" + hashPath)
					.headers(baseHeaders)
					.queryParam("skip", String.valueOf(skip))
					.queryParam("limit", String.valueOf(limit))
					.queryParam("file-metadatas", "1")
					.queryParam("rendered", "1"))
					.andExpect(STATUS_OK)
					.andExpect(CONTENT_TYPE)
					.andExpect(jsonPath("$.realm").exists())
					.andExpect(jsonPath("$.storage").exists())
					.andExpect(jsonPath("$.currentItem").exists())
					.andExpect(jsonPath("$.list").exists())
					.andExpect(jsonPath("$.metadatas").exists())
					.andReturn()
					.getResponse()
					.getContentAsString();

			final var response = objectMapper.readValue(content, FileResponse.class);
			checkResponse(response);
			assertThat(response.metadatas()).hasSize(1);

			final var metadatas = response.metadatas();
			final var metadata = metadatas.get(hashPath);
			assertNotNull(metadata);

			assertThat(metadata.index())
					.hasSize(1)
					.containsKey(indexLayer);
			final var assetResponse = metadata.index().get(indexLayer);

			assertThat(assetResponse.fileMetadatas())
					.hasSize(1)
					.contains(keyValueMetadataResponse);
			assertThat(assetResponse.rendered())
					.hasSize(1)
					.contains(renderedReponse);
			assertThat(assetResponse.index()).isEqualTo(indexLayer);

			verify(fileChildren0, times(1)).getId();
			verify(fileChildren1, times(1)).getId();
			verify(fileMetadataDao, times(1)).getFileMetadatasByFileIds(Set.of(0, 1), realm);
			verify(assetRenderedFileDao, times(1)).getRenderedFilesByFileId(Set.of(0, 1), realm);
			verify(fileMetadataEntity, times(1)).getLayer();
			verify(fileMetadataEntity, times(1)).toKeyValueMetadataResponse();
			verify(assetRenderedFileEntity, times(1)).toRenderedReponse();
			verify(assetRenderedFileEntity, times(1)).getIndexref();
		}

	}
}
