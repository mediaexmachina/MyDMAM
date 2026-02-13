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
import static media.mexm.mydmam.dto.StorageCategory.DAS;
import static media.mexm.mydmam.dto.StorageStateClass.ONLINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import media.mexm.mydmam.activity.HandlingResult;
import media.mexm.mydmam.activity.PendingActivityJob;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.component.AboutInstance;
import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.component.InternalObjectMapper;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.PendingActivityEntity;
import media.mexm.mydmam.indexer.RealmIndexer;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.repository.FileDao;
import media.mexm.mydmam.repository.FileRepository;
import media.mexm.mydmam.repository.PendingActivityDao;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.transfertfiles.FileAttributesReference;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default", "MockActivityHandler" })
class PendingActivityServiceTest {

	@MockitoBean
	PendingActivityDao pendingActivityDao;
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
	FileService fileService;
	@MockitoBean
	FileDao fileDao;
	@MockitoBean
	Indexer indexer;

	@Mock
	RealmConf realm;
	@Mock
	PathIndexingStorage storage;
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
	@Mock
	RealmIndexer realmIndexer;

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
	Optional<RealmIndexer> oIndexer;
	RealmStorageConfiguredEnv configuredEnv;

	@Autowired
	FlatJobKitEngine jobKitEngine;
	@Autowired
	PendingActivityService pas;
	@Autowired
	ActivityHandler activityHandler;

	@BeforeEach
	void init() throws Exception {
		configuredEnv = new RealmStorageConfiguredEnv(realmName, storageName, realm, storage);

		oIndexer = Optional.ofNullable(realmIndexer);

		when(activityHandler.getHandlerName()).thenReturn(handlerName);
		when(activityHandler.getSupportedStorageStateClasses()).thenReturn(Set.of());
		when(activityHandler.canHandle(asset, eventType, configuredEnv)).thenReturn(true);
		when(activityHandler.handle(asset, eventType, configuredEnv)).thenReturn(new HandlingResult());

		when(configuration.getRealmNames()).thenReturn(Set.of(realmName));
		when(configuration.getRealmByName(realmName)).thenReturn(Optional.ofNullable(realm));
		when(configuration.getRealmAndStorage(realmName, storageName)).thenReturn(configuredEnv);
		when(indexer.getIndexerByRealm(realmName)).thenReturn(oIndexer);

		when(realm.spoolProcessAsset()).thenReturn("spool");
		when(file.isDirectory()).thenReturn(false);
		when(mediaAssetService.getFromWatchfolder(realmName, storageName, file, mediaAssetService)).thenReturn(asset);
		when(mediaAssetService.getFromFileEntry(fileEntity, mediaAssetService)).thenReturn(asset);
		when(mediaAssetService.resetDetectedMetadatas(List.of(asset), mediaAssetService)).thenReturn(List.of(asset));

		when(internalObjectMapper.writeValueAsString(Set.of(handlerName))).thenReturn(previousHandlersNames);
		when(aboutInstance.getInstanceName()).thenReturn(hostName);
		when(aboutInstance.getPid()).thenReturn(pid);
		when(asset.getHashPath()).thenReturn(hashPathItem);
		when(asset.getFile()).thenReturn(fileEntity);

		when(fileEntity.isDirectory()).thenReturn(false);
		when(fileEntity.getRealm()).thenReturn(realmName);
		when(fileEntity.getStorage()).thenReturn(storageName);
		when(fileEntity.getHashPath()).thenReturn(hashPathItem);

		when(storage.getStorageStateClass()).thenReturn(ONLINE);
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(
				pendingActivityDao,
				activityHandler,
				mediaAssetService,
				internalObjectMapper,
				aboutInstance,
				fileService);

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
			when(fileRepository.getByHashPath(hashPaths, realmName)).thenReturn(Set.of(fileEntity));
			when(activityHandler.canHandle(asset, eventType, configuredEnv)).thenReturn(false);
			when(fileService.resolveHashPaths(hashPaths, Set.of(DAS), Set.of(ONLINE), realmName, recursive))
					.thenReturn(List.of(subFileEntry));
			when(asset.getFile()).thenReturn(subFileEntry);
		}

		@Test
		void testEmpty() {
			final Set<String> emptySet = Set.of();
			assertThrows(IllegalArgumentException.class,
					() -> pas.startsActivities(realmName, emptySet, recursive, eventType));
			verify(activityHandler, times(3)).getSupportedStorageStateClasses();
		}

		@Test
		void testNoStorages() {
			when(fileService.resolveHashPaths(hashPaths, Set.of(DAS), Set.of(ONLINE), realmName, recursive))
					.thenThrow(IllegalStateException.class);

			assertThrows(IllegalStateException.class,
					() -> pas.startsActivities(realmName, hashPaths, recursive, eventType));

			verify(fileService, times(1))
					.resolveHashPaths(hashPaths, Set.of(DAS), Set.of(ONLINE), realmName, recursive);
			verify(activityHandler, times(2)).getSupportedStorageStateClasses();
		}

		@Test
		void testFile() {
			when(fileEntity.isDirectory()).thenReturn(false);
			when(realm.getStorageNames(anySet(), anySet()))
					.thenReturn(Set.of(storageName));
			when(subFileEntry.isDirectory()).thenReturn(false);
			when(subFileEntry.getRealm()).thenReturn(realmName);
			when(subFileEntry.getStorage()).thenReturn(storageName);
			when(mediaAssetService.getFromFileEntry(subFileEntry, mediaAssetService)).thenReturn(asset);

			pas.startsActivities(realmName, hashPaths, recursive, eventType);

			verify(asset, atLeastOnce()).getFile();
			verify(fileService, times(1))
					.resolveHashPaths(hashPaths, Set.of(DAS), Set.of(ONLINE), realmName, recursive);
			verify(pendingActivityDao, times(1)).declateActivities(anyList(), eq(hostName), eq(pid));
			verify(activityHandler, atLeastOnce()).getSupportedStorageStateClasses();
			verify(activityHandler, times(1)).canHandle(asset, eventType, configuredEnv);
			verify(mediaAssetService, times(1)).getFromFileEntry(subFileEntry, mediaAssetService);
			verify(mediaAssetService, times(1)).resetDetectedMetadatas(List.of(asset), mediaAssetService);
			verify(aboutInstance, atLeastOnce()).getInstanceName();
			verify(aboutInstance, atLeastOnce()).getPid();
			verify(subFileEntry, atLeastOnce()).getRealm();
			verify(subFileEntry, atLeastOnce()).getStorage();
			verify(subFileEntry, atLeastOnce()).isDirectory();
			verify(storage, atLeastOnce()).getStorageStateClass();
			verify(storage, atLeastOnce()).getCategory();
		}

		@Test
		void testDirectory_withFiles() {
			when(fileEntity.isDirectory()).thenReturn(true);
			when(subFileEntry.isDirectory()).thenReturn(false);
			when(subFileEntry.getRealm()).thenReturn(realmName);
			when(subFileEntry.getStorage()).thenReturn(storageName);
			when(mediaAssetService.getFromFileEntry(subFileEntry, mediaAssetService)).thenReturn(asset);

			pas.startsActivities(realmName, hashPaths, recursive, eventType);

			verify(asset, atLeastOnce()).getFile();
			verify(fileService, times(1))
					.resolveHashPaths(hashPaths, Set.of(DAS), Set.of(ONLINE), realmName, recursive);
			verify(pendingActivityDao, times(1)).declateActivities(anyList(), eq(hostName), eq(pid));
			verify(activityHandler, atLeastOnce()).getSupportedStorageStateClasses();
			verify(activityHandler, times(1)).canHandle(asset, eventType, configuredEnv);
			verify(mediaAssetService, times(1)).getFromFileEntry(subFileEntry, mediaAssetService);
			verify(mediaAssetService, times(1)).resetDetectedMetadatas(List.of(asset), mediaAssetService);
			verify(aboutInstance, atLeastOnce()).getInstanceName();
			verify(aboutInstance, atLeastOnce()).getPid();
			verify(subFileEntry, atLeastOnce()).getRealm();
			verify(subFileEntry, atLeastOnce()).getStorage();
			verify(subFileEntry, atLeastOnce()).isDirectory();
			verify(storage, atLeastOnce()).getStorageStateClass();
			verify(storage, atLeastOnce()).getCategory();
		}

		@Test
		void testDirectory_withDirs() {
			when(fileEntity.isDirectory()).thenReturn(true);
			when(subFileEntry.isDirectory()).thenReturn(true);
			when(realm.getStorageNames(anySet(), anySet()))
					.thenReturn(Set.of(storageName));

			pas.startsActivities(realmName, hashPaths, recursive, eventType);

			verify(fileService, times(1))
					.resolveHashPaths(hashPaths, Set.of(DAS), Set.of(ONLINE), realmName, recursive);
			verify(subFileEntry, atLeastOnce()).isDirectory();
			verify(activityHandler, times(3)).getSupportedStorageStateClasses();
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
		void testStartsActivities() {
			pas.startsActivities(realmName, storageName, realm, Set.of(file), eventType);

			verify(file, atLeastOnce()).isDirectory();
			verify(mediaAssetService, times(1)).getFromWatchfolder(realmName, storageName, file, mediaAssetService);
			verify(realm, atLeastOnce()).spoolProcessAsset();
			verify(mediaAssetService, times(1)).resetDetectedMetadatas(List.of(asset), mediaAssetService);
			verify(activityHandler, times(1)).canHandle(asset, eventType, configuredEnv);
			verify(activityHandler, atLeastOnce()).getHandlerName();
			verify(activityHandler, times(2)).getSupportedStorageStateClasses();
			verify(asset, atLeastOnce()).getName();
			verify(asset, atLeastOnce()).getFile();
			verify(fileEntity, atLeastOnce()).isDirectory();
			verify(fileEntity, atLeastOnce()).getRealm();
			verify(fileEntity, atLeastOnce()).getStorage();
			verify(internalObjectMapper, times(1)).writeValueAsString(Set.of(handlerName));
			verify(aboutInstance, atLeastOnce()).getInstanceName();
			verify(aboutInstance, atLeastOnce()).getPid();
			verify(pendingActivityDao, times(2)).declateActivities(anyList(), eq(hostName), eq(pid));
			verify(pendingActivityDao, times(1)).haveDeclaredActivity(fileEntity, activityHandler);
			verify(configuration, times(1)).getRealmAndStorage(realmName, storageName);
			verify(storage, atLeastOnce()).getStorageStateClass();
			verify(storage, atLeastOnce()).getCategory();
		}
	}

	@Test
	void testGetHandlersNames() {
		final var names = PendingActivityServiceImpl.getHandlersNames(List.of(activityHandler));
		assertThat(names).containsExactly(handlerName);
		verify(activityHandler, times(1)).getHandlerName();
	}

	@Test
	void testContinueAssetActivity() {
		final var previousHandlers = Set.of(handlerName, previousHandlersNames);
		when(internalObjectMapper.writeValueAsString(previousHandlers)).thenReturn(previousHandlersNames);

		pendingActivityJob = new PendingActivityJob(
				configuredEnv,
				asset,
				activityHandler,
				eventType,
				new HashSet<>(Set.of(previousHandlersNames)),
				previousHandlersNames,
				pendingActivityDao, pas, oIndexer);

		pas.continueAssetActivity(pendingActivityJob);

		verify(activityHandler, times(1)).canHandle(asset, eventType, configuredEnv);
		verify(activityHandler, atLeastOnce()).getHandlerName();
		verify(asset, atLeastOnce()).getFile();
		verify(asset, atLeastOnce()).getName();
		verify(internalObjectMapper, times(1)).writeValueAsString(previousHandlers);
		verify(aboutInstance, atLeastOnce()).getInstanceName();
		verify(aboutInstance, atLeastOnce()).getPid();
		verify(activityHandler, times(1)).getSupportedStorageStateClasses();
		verify(pendingActivityDao, times(2)).declateActivities(anyList(), eq(hostName), eq(pid));
		verify(pendingActivityDao, times(1)).haveDeclaredActivity(fileEntity, activityHandler);
		verify(realm, atLeastOnce()).spoolProcessAsset();
		verify(storage, atLeastOnce()).getStorageStateClass();
		verify(storage, atLeastOnce()).getCategory();
	}

	@Test
	void testRestartPendingActivities_empty() {
		when(pendingActivityDao.getFilesAndWithResetPendingActivities(Set.of(realmName), hostName, pid))
				.thenReturn(List.of());

		pas.restartPendingActivities();

		verify(configuration, atLeastOnce()).getRealmNames();
		verify(aboutInstance, atLeastOnce()).getInstanceName();
		verify(aboutInstance, atLeastOnce()).getPid();
		verify(pendingActivityDao, times(1))
				.getFilesAndWithResetPendingActivities(Set.of(realmName), hostName, pid);
	}

	@Nested
	class RestartPendingActivities {

		@Fake
		int activityFileId;
		@Fake
		String unknownHander;

		@BeforeEach
		void init() {
			when(pendingActivityDao.getFilesAndWithResetPendingActivities(Set.of(realmName), hostName, pid))
					.thenReturn(List.of(activityFileId));
			when(pendingActivityDao.getFilesAndPendingActivityByFileId(List.of(activityFileId)))
					.thenReturn(Map.of(fileEntity, Set.of(entity)));
			when(pendingActivityDao.haveDeclaredActivity(fileEntity, activityHandler)).thenReturn(false);
			when(entity.getHandlerName()).thenReturn(handlerName);
			when(entity.getPreviousHandlers()).thenReturn(previousHandlersNames);
			when(entity.getEventType()).thenReturn(eventType.name());
			when(mediaAssetService.getFromFileEntry(fileEntity, mediaAssetService)).thenReturn(asset);
			when(internalObjectMapper.readValue(previousHandlersNames, TYPE_LIST_STRING))
					.thenReturn(List.of());
			when(internalObjectMapper.writeValueAsString(Set.of(handlerName))).thenReturn(previousHandlersNames);
		}

		@AfterEach
		void end() {
			verify(configuration, atLeastOnce()).getRealmNames();
			verify(aboutInstance, atLeastOnce()).getInstanceName();
			verify(aboutInstance, atLeastOnce()).getPid();
			verify(pendingActivityDao, times(1))
					.getFilesAndWithResetPendingActivities(Set.of(realmName), hostName, pid);
			verify(pendingActivityDao, times(1))
					.getFilesAndPendingActivityByFileId(List.of(activityFileId));
			verify(entity, atLeastOnce()).getHandlerName();
			verify(entity, atLeastOnce()).getPreviousHandlers();

			verify(mediaAssetService, times(1)).getFromFileEntry(fileEntity, mediaAssetService);
			verify(internalObjectMapper, times(1)).readValue(previousHandlersNames, TYPE_LIST_STRING);
			verify(activityHandler, atLeastOnce()).getHandlerName();
			verify(fileEntity, atLeastOnce()).getRealm();
			verify(fileEntity, atLeastOnce()).getStorage();
		}

		@Test
		void test() {
			pas.restartPendingActivities();

			verify(pendingActivityDao, times(1))
					.haveDeclaredActivity(fileEntity, activityHandler);
			verify(pendingActivityDao, times(1)).declateActivities(anyList(), eq(hostName), eq(pid));
			verify(entity, atLeastOnce()).getEventType();
			verify(internalObjectMapper, times(1)).writeValueAsString(Set.of(handlerName));
			verify(realm, atLeastOnce()).spoolProcessAsset();
			verify(asset, atLeastOnce()).getName();
			verify(asset, atLeastOnce()).getFile();
			verify(activityHandler, atLeastOnce()).getSupportedStorageStateClasses();
			verify(storage, atLeastOnce()).getStorageStateClass();
			verify(storage, atLeastOnce()).getCategory();
		}

		@Test
		void test_unknownHander() {
			when(entity.getHandlerName()).thenReturn(unknownHander);
			pas.restartPendingActivities();
			verify(activityHandler, atLeastOnce()).getSupportedStorageStateClasses();
		}

	}

	@Test
	void testCleanupFiles() {
		pas.cleanupFiles(realmName, storageName, realm, Set.of(lostedFile));
		verify(mediaAssetService, times(1)).purgeAssetArtefacts(realmName, storageName, lostedFile);
	}

}
