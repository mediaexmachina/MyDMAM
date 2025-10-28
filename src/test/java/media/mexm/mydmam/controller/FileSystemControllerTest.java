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

import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import media.mexm.mydmam.dto.FileItemResponse;
import media.mexm.mydmam.dto.FileResponse;
import media.mexm.mydmam.dto.RealmListResponse;
import media.mexm.mydmam.dto.StorageListResponse;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.FileDao;
import media.mexm.mydmam.repository.FileRepository;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "FlatJobKit" })
class FileSystemControllerTest {

	private static final RequestMapping REQUEST_MAPPING = FileSystemController.class.getAnnotation(
			RequestMapping.class);
	private static final String BASE_MAPPING = REQUEST_MAPPING.value()[0];
	private static final ResultMatcher CONTENT_TYPE = content().contentType(REQUEST_MAPPING.produces()[0]);
	private static final ResultMatcher STATUS_OK = status().isOk();
	private static final ResultMatcher STATUS_BAD_REQUEST = status().isBadRequest();

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	FlatJobKitEngine flatJobKitEngine;

	@MockitoBean
	FileRepository fileRepository;
	@MockitoBean
	FileDao fileDao;

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

	HttpHeaders baseHeaders;
	long modified;
	String parentHashPath;

	@BeforeEach
	void init() {
		baseHeaders = new HttpHeaders();
		baseHeaders.setContentType(APPLICATION_JSON);
		modified = System.currentTimeMillis();
		hashPath = hashPath.toLowerCase();
		parentHashPath = hashPath(realm, storage, "/" + basePath);

		when(file.getHashPath()).thenReturn(hashPath);
		when(file.getLength()).thenReturn(0l);
		when(file.getModified()).thenReturn(new Timestamp(modified));
		when(file.getParentHashPath()).thenReturn(parentHashPath);
		when(file.getPath()).thenReturn("/" + basePath + "/" + baseName);
		when(file.getRealm()).thenReturn(realm);
		when(file.getStorage()).thenReturn(storage);
		when(file.isDirectory()).thenReturn(true);

		when(fileChildren0.getHashPath()).thenReturn(hashPath + "filechildren0");
		when(fileChildren0.getLength()).thenReturn(length);
		when(fileChildren0.getModified()).thenReturn(new Timestamp(modified));
		when(fileChildren0.getParentHashPath()).thenReturn(hashPath);
		when(fileChildren0.getPath()).thenReturn("/" + basePath + "/" + baseName + "/filechildren0");
		when(fileChildren0.getRealm()).thenReturn(realm);
		when(fileChildren0.getStorage()).thenReturn(storage);
		when(fileChildren0.isDirectory()).thenReturn(false);

		when(fileChildren1.getHashPath()).thenReturn(hashPath + "filechildren1");
		when(fileChildren1.getLength()).thenReturn(length);
		when(fileChildren1.getModified()).thenReturn(new Timestamp(modified));
		when(fileChildren1.getParentHashPath()).thenReturn(hashPath);
		when(fileChildren1.getPath()).thenReturn("/" + basePath + "/" + baseName + "/filechildren1");
		when(fileChildren1.getRealm()).thenReturn(realm);
		when(fileChildren1.getStorage()).thenReturn(storage);
		when(fileChildren1.isDirectory()).thenReturn(false);
	}

	@AfterEach
	void ends() {
		assertTrue(flatJobKitEngine.isEmptyActiveServicesList());
		assertEquals(0, flatJobKitEngine.getEndEventsList().size());
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

	@Test
	void testGetStorages() throws Exception {
		when(fileRepository.getAllStoragesByRealm(realm)).thenReturn(Set.of(storage, storage1));

		final var content = mvc.perform(get(BASE_MAPPING + "/list/" + realm)
				.headers(baseHeaders))
				.andExpect(STATUS_OK)
				.andExpect(CONTENT_TYPE)
				.andExpect(jsonPath("$.realm").exists())
				.andExpect(jsonPath("$.storages").exists())
				.andReturn()
				.getResponse()
				.getContentAsString();

		final var response = objectMapper.readValue(content, StorageListResponse.class);
		assertThat(response.realm()).isEqualTo(realm);
		assertThat(response.storages()).containsExactlyInAnyOrder(storage, storage1);

		verify(fileRepository, times(1)).getAllStoragesByRealm(realm);
	}

	@Test
	void testList() throws Exception {
		when(fileDao.getByParentHashPath(hashPath, skip, limit))
				.thenReturn(List.of(fileChildren0, fileChildren1));
		when(fileDao.countParentHashPathItems(realm, storage, hashPath)).thenReturn(totalSize);
		when(fileRepository.getByHashPath(hashPath)).thenReturn(file);

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
				.andReturn()
				.getResponse()
				.getContentAsString();

		final var response = objectMapper.readValue(content, FileResponse.class);

		verify(fileDao, times(1)).getByParentHashPath(hashPath, skip, limit);
		verify(fileDao, times(1)).countParentHashPathItems(realm, storage, hashPath);
		verify(fileRepository, times(1)).getByHashPath(hashPath);

		assertThat(response.realm()).isEqualTo(realm);
		assertThat(response.storage()).isEqualTo(storage);
		assertThat(response.currentItem()).isEqualTo(new FileItemResponse(true, baseName, hashPath, modified, -1));
		assertThat(response.path()).isEqualTo("/" + basePath + "/" + baseName);
		assertThat(response.parentHashPath()).isEqualTo(parentHashPath);
		assertThat(response.listSize()).isEqualTo(2);
		assertThat(response.skipCount()).isEqualTo(skip);
		assertThat(response.total()).isEqualTo(totalSize);
		assertThat(response.list()).size().isEqualTo(2);

		assertTrue(response.list().stream().anyMatch(f -> f.hashPath().equals(hashPath + "filechildren0")));
		assertTrue(response.list().stream().anyMatch(f -> f.hashPath().equals(hashPath + "filechildren1")));

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
		}

	}

	@Test
	void testList_badRealm() throws Exception {
		when(fileDao.getByParentHashPath(any(), anyInt(), anyInt())).thenReturn(List.of());
		when(fileDao.countParentHashPathItems(any(), any(), any())).thenReturn(0);
		when(fileRepository.getByHashPath(any(String.class))).thenReturn(file);

		when(file.getRealm()).thenReturn(realm1);

		mvc.perform(get(BASE_MAPPING + "/list/" + realm + "/" + storage + "/" + hashPath)
				.headers(baseHeaders))
				.andExpect(STATUS_BAD_REQUEST);

		verify(file, atLeastOnce()).getRealm();
		verify(file, atLeastOnce()).getPath();
	}

	@Test
	void testListRoot() throws Exception {
		final var computedHashPath = hashPath(realm, storage, "/");

		when(fileDao.getByParentHashPath(any(), anyInt(), anyInt())).thenReturn(List.of());
		when(fileDao.countParentHashPathItems(any(), any(), any())).thenReturn(0);
		when(fileRepository.getByHashPath(any(String.class))).thenReturn(file);

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
				.andReturn()
				.getResponse()
				.getContentAsString();

		final var response = objectMapper.readValue(content, FileResponse.class);

		assertThat(response.realm()).isEqualTo(realm);
		assertThat(response.storage()).isEqualTo(storage);
		assertThat(response.currentItem()).isEqualTo(new FileItemResponse(true, baseName, hashPath, modified, -1));
		assertThat(response.path()).isEqualTo("/" + basePath + "/" + baseName);
		assertThat(response.parentHashPath()).isEqualTo(parentHashPath);
		assertThat(response.listSize()).isZero();
		assertThat(response.skipCount()).isEqualTo(skip);
		assertThat(response.total()).isZero();
		assertThat(response.list()).isEmpty();

		verify(fileDao, times(1)).getByParentHashPath(computedHashPath, skip, limit);
		verify(fileDao, times(1)).countParentHashPathItems(realm, storage, computedHashPath);
		verify(fileRepository, times(1)).getByHashPath(computedHashPath);

		verify(file, atLeastOnce()).getHashPath();
		verify(file, atLeastOnce()).getModified();
		verify(file, atLeastOnce()).getPath();
		verify(file, atLeastOnce()).getRealm();
		verify(file, atLeastOnce()).getStorage();
		verify(file, atLeastOnce()).isDirectory();
	}

}
