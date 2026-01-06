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
package media.mexm.mydmam.service;

import static media.mexm.mydmam.component.InternalObjectMapper.TYPE_LIST_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.PendingActivityJob;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.component.AboutInstance;
import media.mexm.mydmam.component.InternalObjectMapper;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.PendingActivityEntity;
import media.mexm.mydmam.repository.FileDao;
import media.mexm.mydmam.repository.FileRepository;
import media.mexm.mydmam.repository.PendingActivityDao;
import media.mexm.mydmam.tools.FileEntityConsumer;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.transfertfiles.FileAttributesReference;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class PendingActivityServiceTest {

	@MockitoBean
	PendingActivityDao pendingActivityDao;
	@MockitoBean
	ActivityHandler activityHandler;
	@MockitoBean
	MediaAssetService mediaAssetService;
	@MockitoBean
	MyDMAMConfigurationProperties configuration;
	@MockitoBean
	InternalObjectMapper internalObjectMapper;
	@MockitoBean
	AboutInstance aboutInstance;
	@MockitoBean
	FileRepository fileRepository;
	@MockitoBean
	FileDao fileDao;

	@Mock
	RealmConf realm;
	@Mock
	FileAttributesReference lostedFile;
	@Mock
	FileAttributesReference file;
	@Mock
	MediaAsset asset;
	@Mock
	PendingActivityEntity entity;
	@Mock
	FileEntity fileEntity;

	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	ActivityEventType eventType;
	@Fake
	String handlerName;
	@Fake
	String hostName;
	@Fake
	long pid;
	@Fake
	String previousHandlersNames;
	@Fake
	String hashPathItem;
	@Fake
	String spool;

	PendingActivityJob pendingActivityJob;

	@Autowired
	FlatJobKitEngine jobKitEngine;
	@Autowired
	PendingActivityService pas;

	@BeforeEach
	void init() {
		when(activityHandler.getHandlerName()).thenReturn(handlerName);
		when(activityHandler.canHandle(asset, eventType)).thenReturn(true);

		when(configuration.getRealmNames()).thenReturn(Set.of(realmName));
		when(configuration.getRealmByName(realmName)).thenReturn(Optional.ofNullable(realm));
		when(realm.spoolProcessAsset()).thenReturn("spool");
		when(file.isDirectory()).thenReturn(false);
		when(mediaAssetService.getFromWatchfolder(realmName, storageName, file, mediaAssetService)).thenReturn(asset);
		when(mediaAssetService.getFromFileEntry(fileEntity, mediaAssetService)).thenReturn(asset);
		when(internalObjectMapper.writeValueAsString(Set.of(handlerName))).thenReturn(previousHandlersNames);
		when(aboutInstance.getInstanceName()).thenReturn(hostName);
		when(aboutInstance.getPid()).thenReturn(pid);
		when(asset.getHashPath()).thenReturn(hashPathItem);

		when(fileEntity.getRealm()).thenReturn(realmName);
		when(fileEntity.getStorage()).thenReturn(storageName);
		when(fileEntity.getHashPath()).thenReturn(hashPathItem);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(
				pendingActivityDao,
				activityHandler,
				mediaAssetService,
				internalObjectMapper,
				aboutInstance);

		assertTrue(jobKitEngine.isEmptyActiveServicesList());
		assertTrue(jobKitEngine.getEndEventsList().isEmpty());
	}

	@Nested
	class HashPathsActivities {

		@Mock
		FileEntity subFileEntry;
		@Fake
		boolean recursive;
		Set<String> hashPaths;

		@BeforeEach
		void init() {
			hashPaths = Set.of(hashPathItem);
			when(realm.getOnlineDASStorageNames()).thenReturn(Set.of(storageName));
			when(fileRepository.getByHashPath(hashPaths)).thenReturn(Set.of(fileEntity));
			when(activityHandler.canHandle(asset, eventType)).thenReturn(false);
		}

		@Test
		void testEmpty() {
			final Set<String> emptySet = Set.of();
			assertThrows(IllegalArgumentException.class,
					() -> pas.startsActivities(realmName, emptySet, recursive, eventType));
		}

		@Test
		void testBadRealm() {
			assertThrows(IllegalArgumentException.class,
					() -> pas.startsActivities("NOPE", hashPaths, recursive, eventType));
		}

		@Test
		void testNoStorages() {
			when(realm.getOnlineDASStorageNames()).thenReturn(Set.of());
			assertThrows(IllegalStateException.class,
					() -> pas.startsActivities(realmName, hashPaths, recursive, eventType));
			verify(realm, times(1)).getOnlineDASStorageNames();
		}

		@Test
		void testFile_invalidRealm() {
			when(fileEntity.getRealm()).thenReturn("NOPE");

			assertThrows(IllegalStateException.class,
					() -> pas.startsActivities(realmName, hashPaths, recursive, eventType));

			verify(realm, times(1)).getOnlineDASStorageNames();
			verify(fileRepository, times(1)).getByHashPath(hashPaths);
			verify(fileEntity, atLeastOnce()).getRealm();
		}

		@Test
		void testFile() {
			when(fileEntity.isDirectory()).thenReturn(false);

			pas.startsActivities(realmName, hashPaths, recursive, eventType);

			verify(realm, times(1)).getOnlineDASStorageNames();
			verify(fileRepository, times(1)).getByHashPath(hashPaths);
			verify(fileEntity, atLeastOnce()).getRealm();
			verify(fileEntity, atLeastOnce()).isDirectory();
			verify(fileEntity, atLeastOnce()).getStorage();
			verify(mediaAssetService, atLeastOnce()).getFromFileEntry(fileEntity, mediaAssetService);

			verify(activityHandler, times(1)).canHandle(asset, eventType);
		}

		@Test
		void testDirectory_withFiles() {
			when(fileEntity.isDirectory()).thenReturn(true);
			when(subFileEntry.isDirectory()).thenReturn(false);
			when(mediaAssetService.getFromFileEntry(subFileEntry, mediaAssetService)).thenReturn(asset);

			doAnswer(invocation -> {
				final var args = invocation.getArguments();
				((FileEntityConsumer) args[2]).accept(subFileEntry);
				return null;
			}).when(fileDao).getByParentHashPath(eq(realmName), eq(hashPaths), any(), eq(recursive));

			pas.startsActivities(realmName, hashPaths, recursive, eventType);

			verify(fileDao, times(1)).getByParentHashPath(eq(realmName), eq(hashPaths), any(), eq(recursive));
			verify(realm, times(1)).getOnlineDASStorageNames();
			verify(fileRepository, times(1)).getByHashPath(hashPaths);
			verify(fileEntity, atLeastOnce()).getRealm();
			verify(fileEntity, atLeastOnce()).isDirectory();
			verify(fileEntity, atLeastOnce()).getStorage();
			verify(fileEntity, atLeastOnce()).getHashPath();
			verify(subFileEntry, atLeastOnce()).isDirectory();
			verify(mediaAssetService, atLeastOnce()).getFromFileEntry(subFileEntry, mediaAssetService);
			verify(activityHandler, times(1)).canHandle(asset, eventType);
		}

		@Test
		void testDirectory_withDirs() {
			when(fileEntity.isDirectory()).thenReturn(true);
			when(subFileEntry.isDirectory()).thenReturn(true);

			doAnswer(invocation -> {
				final var args = invocation.getArguments();
				((FileEntityConsumer) args[2]).accept(subFileEntry);
				return null;
			}).when(fileDao).getByParentHashPath(eq(realmName), eq(hashPaths), any(), eq(recursive));

			pas.startsActivities(realmName, hashPaths, recursive, eventType);

			verify(fileDao, times(1)).getByParentHashPath(eq(realmName), eq(hashPaths), any(), eq(recursive));
			verify(realm, times(1)).getOnlineDASStorageNames();
			verify(fileRepository, times(1)).getByHashPath(hashPaths);
			verify(fileEntity, atLeastOnce()).getRealm();
			verify(fileEntity, atLeastOnce()).isDirectory();
			verify(fileEntity, atLeastOnce()).getStorage();
			verify(fileEntity, atLeastOnce()).getHashPath();
			verify(subFileEntry, atLeastOnce()).isDirectory();
		}

	}

	@Nested
	class FileActivities {

		@Test
		void testStartsActivities_onlyDir() {
			when(file.isDirectory()).thenReturn(true);

			pas.startsActivities(realmName, storageName, realm, Set.of(file), eventType);
			verify(file, times(1)).isDirectory();
		}

		@Test
		void testStartsActivities_empty() {// NOSONAR S2699
			pas.startsActivities(realmName, storageName, realm, Set.of(), eventType);
		}

		@Test
		void testStartsActivities() throws Exception {
			pas.startsActivities(realmName, storageName, realm, Set.of(file), eventType);

			verify(file, atLeastOnce()).isDirectory();
			verify(mediaAssetService, times(1)).getFromWatchfolder(realmName, storageName, file, mediaAssetService);
			verify(realm, atLeastOnce()).spoolProcessAsset();
			verify(activityHandler, times(1)).canHandle(asset, eventType);
			verify(activityHandler, atLeastOnce()).getHandlerName();
			verify(asset, atLeastOnce()).getHashPath();
			verify(asset, atLeastOnce()).getName();
			verify(internalObjectMapper, times(1)).writeValueAsString(Set.of(handlerName));
			verify(aboutInstance, atLeastOnce()).getInstanceName();
			verify(aboutInstance, atLeastOnce()).getPid();
			verify(pendingActivityDao, times(1))
					.declateActivity(hashPathItem, activityHandler, eventType, previousHandlersNames, hostName, pid);

			verify(activityHandler, times(1)).handle(asset, eventType);
			verify(pendingActivityDao, times(1)).endsActivity(hashPathItem, activityHandler);
		}
	}

	@Test
	void testGetHandlersNames() {
		final var names = PendingActivityServiceImpl.getHandlersNames(List.of(activityHandler));
		assertThat(names).containsExactly(handlerName);
		verify(activityHandler, times(1)).getHandlerName();
	}

	@Test
	void testContinueAssetActivity() throws Exception {
		final var previousHandlers = Set.of(handlerName, previousHandlersNames);
		when(internalObjectMapper.writeValueAsString(previousHandlers)).thenReturn(previousHandlersNames);

		pendingActivityJob = new PendingActivityJob(
				spool,
				asset,
				activityHandler,
				eventType,
				new HashSet<>(Set.of(previousHandlersNames)),
				pendingActivityDao, pas);

		pas.continueAssetActivity(pendingActivityJob);

		verify(activityHandler, times(1)).canHandle(asset, eventType);
		verify(activityHandler, atLeastOnce()).getHandlerName();
		verify(asset, atLeastOnce()).getHashPath();
		verify(asset, atLeastOnce()).getName();
		verify(internalObjectMapper, times(1)).writeValueAsString(previousHandlers);
		verify(aboutInstance, atLeastOnce()).getInstanceName();
		verify(aboutInstance, atLeastOnce()).getPid();

		verify(pendingActivityDao, times(1))
				.declateActivity(hashPathItem, activityHandler, eventType, previousHandlersNames, hostName, pid);
		verify(activityHandler, times(1)).handle(asset, eventType);
		verify(pendingActivityDao, times(1)).endsActivity(hashPathItem, activityHandler);
	}

	@Test
	void testRestartPendingActivities_empty() {
		when(pendingActivityDao.getPendingActivities(Set.of(realmName), hostName)).thenReturn(List.of());

		pas.restartPendingActivities();

		verify(pendingActivityDao, times(1)).getPendingActivities(Set.of(realmName), hostName);
		verify(aboutInstance, atLeastOnce()).getInstanceName();
	}

	@Nested
	class RestartPendingActivities {

		@BeforeEach
		void init() {
			when(pendingActivityDao.getPendingActivities(Set.of(realmName), hostName)).thenReturn(List.of(entity));
			when(entity.getEventType()).thenReturn(eventType.name());
			when(entity.getFile()).thenReturn(fileEntity);
			when(entity.getPreviousHandlers()).thenReturn(previousHandlersNames);
			when(entity.getHandlerName()).thenReturn(handlerName);
			when(internalObjectMapper.readValue(previousHandlersNames, TYPE_LIST_STRING))
					.thenReturn(List.of(handlerName + "-previous"));
		}

		@Test
		void testRestartPendingActivities() throws Exception {
			pas.restartPendingActivities();

			verify(aboutInstance, atLeastOnce()).getPid();

			verify(pendingActivityDao, times(1)).resetPendingActivity(entity, hostName, pid);
			verify(activityHandler, times(1)).handle(asset, eventType);
			verify(pendingActivityDao, times(1)).endsActivity(hashPathItem, activityHandler);
			verify(asset, atLeastOnce()).getHashPath();
			verify(asset, atLeastOnce()).getName();
		}

		@Test
		void testRestartPendingActivities_unknownActivityHanders() {// NOSONAR S2699
			when(entity.getHandlerName()).thenReturn(handlerName + "-unknown");

			pas.restartPendingActivities();
		}

		@AfterEach
		void ends() {
			verify(pendingActivityDao, times(1)).getPendingActivities(Set.of(realmName), hostName);
			verify(aboutInstance, atLeastOnce()).getInstanceName();
			verify(entity, atLeastOnce()).getEventType();
			verify(entity, atLeastOnce()).getFile();
			verify(entity, atLeastOnce()).getPreviousHandlers();
			verify(entity, atLeastOnce()).getHandlerName();
			verify(fileEntity, atLeastOnce()).getRealm();
			verify(internalObjectMapper, times(1)).readValue(previousHandlersNames, TYPE_LIST_STRING);
			verify(mediaAssetService, times(1)).getFromFileEntry(fileEntity, mediaAssetService);
			verify(activityHandler, atLeastOnce()).getHandlerName();
			verify(configuration, atLeastOnce()).getRealmByName(realmName);
			verify(realm, atLeastOnce()).spoolProcessAsset();
		}

	}

	@Test
	void testCleanupFiles() {
		pas.cleanupFiles(realmName, storageName, realm, Set.of(lostedFile));
		verify(mediaAssetService, times(1)).purgeAssetArtefacts(realmName, storageName, lostedFile);
	}

}
