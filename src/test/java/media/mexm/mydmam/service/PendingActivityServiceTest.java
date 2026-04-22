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

import static media.mexm.mydmam.activity.ActivityLimitPolicy.DISABLED;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FULL_PREVIEW;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.TYPE_EXTRACTION;
import static media.mexm.mydmam.dto.StorageCategory.DAS;
import static media.mexm.mydmam.dto.StorageStateClass.ONLINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.PendingActivityJob;
import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.InstanceEntity;
import media.mexm.mydmam.entity.PendingActivityEntity;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.repository.FileDao;
import media.mexm.mydmam.repository.FileRepository;
import media.mexm.mydmam.repository.InstanceDao;
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
    InstanceDao instanceDao;
    @MockitoBean
    MediaAssetService mediaAssetService;
    @MockitoBean
    MyDMAMConfigurationProperties configuration;
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
    PendingActivityEntity entity;
    @Mock
    FileEntity fileEntity;
    @Mock
    InstanceEntity instanceEntity;

    @Fake
    String realmName;
    @Fake
    String storageName;
    @Fake
    ActivityEventType eventType;
    @Fake
    String handlerName;
    @Fake
    String hashPathItem;
    @Fake
    String spool;

    PendingActivityJob pendingActivityJob;
    RealmStorageConfiguredEnv configuredEnv;

    @Autowired
    FlatJobKitEngine jobKitEngine;
    @Autowired
    PendingActivityService pas;
    @Autowired
    ActivityHandler activityHandler;

    @BeforeEach
    void init() {
        configuredEnv = new RealmStorageConfiguredEnv(realmName, storageName, realm, storage);

        when(activityHandler.isEnabled()).thenReturn(true);
        when(activityHandler.getHandlerName()).thenReturn(handlerName);
        when(activityHandler.getSupportedStorageStateClasses()).thenReturn(Set.of(ONLINE));
        when(activityHandler.getLimitPolicy()).thenReturn(FULL_PREVIEW);
        when(activityHandler.canHandle(any(), eq(eventType), eq(configuredEnv))).thenReturn(true);

        when(configuration.getRealmNames()).thenReturn(Set.of(realmName));
        when(configuration.getRealmByName(realmName)).thenReturn(Optional.ofNullable(realm));
        when(configuration.getRealmAndStorage(realmName, storageName)).thenReturn(configuredEnv);
        when(configuration.isActivatedActivityHandler(eq(realmName), anyString())).thenReturn(false);
        when(configuration.isActivatedActivityHandler(realmName, handlerName)).thenReturn(true);

        when(realm.spoolProcessAsset()).thenReturn("spool");
        when(file.isDirectory()).thenReturn(false);
        when(mediaAssetService.getFromWatchfolder(realmName, storageName, file)).thenReturn(fileEntity);
        when(mediaAssetService.resetDetectedMetadatas(any())).thenReturn(List.of(fileEntity));

        when(fileEntity.isDirectory()).thenReturn(false);
        when(fileEntity.getRealm()).thenReturn(realmName);
        when(fileEntity.getStorage()).thenReturn(storageName);
        when(fileEntity.getHashPath()).thenReturn(hashPathItem);

        when(storage.getStorageStateClass()).thenReturn(ONLINE);
        when(storage.activityLimit()).thenReturn(FULL_PREVIEW);

        when(pendingActivityDao.havePendingActivities(any())).thenReturn(false);
        when(instanceDao.getSelfInstance()).thenReturn(instanceEntity);
    }

    @AfterEach
    void ends() {
        verifyNoMoreInteractions(
                pendingActivityDao,
                instanceDao,
                activityHandler,
                mediaAssetService,
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
            when(activityHandler.canHandle(any(), eq(eventType), eq(configuredEnv))).thenReturn(false);
            when(fileService.resolveHashPaths(hashPaths, Set.of(DAS), Set.of(ONLINE), realmName, recursive))
                    .thenReturn(List.of(subFileEntry));
        }

        @AfterEach
        void ends() {
            verify(activityHandler, atLeastOnce()).isEnabled();
        }

        @Test
        void testEmpty() {
            final Set<String> emptySet = Set.of();
            assertThrows(IllegalArgumentException.class,
                    () -> pas.startsActivities(realmName, emptySet, recursive, eventType));
            verify(activityHandler, atLeastOnce()).getLimitPolicy();
        }

        @Test
        void testNoStorages() {
            when(fileService.resolveHashPaths(hashPaths, Set.of(DAS), Set.of(ONLINE), realmName, recursive))
                    .thenThrow(IllegalStateException.class);

            assertThrows(IllegalStateException.class,
                    () -> pas.startsActivities(realmName, hashPaths, recursive, eventType));

            verify(fileService, times(1))
                    .resolveHashPaths(hashPaths, Set.of(DAS), Set.of(ONLINE), realmName, recursive);
            verify(activityHandler, atLeast(0)).getSupportedStorageStateClasses();
            verify(activityHandler, atLeast(0)).getLimitPolicy();
        }

        @Test
        void testFile() {
            when(fileEntity.isDirectory()).thenReturn(false);
            when(realm.getStorageNames(anySet(), anySet()))
                    .thenReturn(Set.of(storageName));
            when(subFileEntry.isDirectory()).thenReturn(false);
            when(subFileEntry.getRealm()).thenReturn(realmName);
            when(subFileEntry.getStorage()).thenReturn(storageName);

            pas.startsActivities(realmName, hashPaths, recursive, eventType);

            verify(fileService, times(1))
                    .resolveHashPaths(hashPaths, Set.of(DAS), Set.of(ONLINE), realmName, recursive);
            verify(activityHandler, atLeast(0)).getSupportedStorageStateClasses();
            verify(activityHandler, atLeastOnce()).getLimitPolicy();
            verify(activityHandler, times(1)).canHandle(fileEntity, eventType, configuredEnv);
            verify(activityHandler, atLeastOnce()).getHandlerName();
            verify(mediaAssetService, times(1)).resetDetectedMetadatas(List.of(subFileEntry));
            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getStorage();
            verify(subFileEntry, atLeastOnce()).isDirectory();
            verify(storage, atLeastOnce()).getStorageStateClass();
            verify(storage, atLeastOnce()).activityLimit();
            verify(configuration, atLeastOnce()).isActivatedActivityHandler(realmName, handlerName);
        }

        @Test
        void testDirectory_withFiles() {
            when(fileEntity.isDirectory()).thenReturn(true);
            when(subFileEntry.isDirectory()).thenReturn(false);
            when(subFileEntry.getRealm()).thenReturn(realmName);
            when(subFileEntry.getStorage()).thenReturn(storageName);

            pas.startsActivities(realmName, hashPaths, recursive, eventType);

            verify(fileService, times(1))
                    .resolveHashPaths(hashPaths, Set.of(DAS), Set.of(ONLINE), realmName, recursive);
            verify(activityHandler, atLeast(0)).getSupportedStorageStateClasses();
            verify(activityHandler, atLeast(0)).getLimitPolicy();
            verify(activityHandler, atLeastOnce()).getHandlerName();
            verify(activityHandler, times(1)).canHandle(fileEntity, eventType, configuredEnv);
            verify(mediaAssetService, times(1)).resetDetectedMetadatas(List.of(subFileEntry));
            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getStorage();
            verify(subFileEntry, atLeastOnce()).isDirectory();
            verify(storage, atLeastOnce()).getStorageStateClass();
            verify(storage, atLeastOnce()).activityLimit();
            verify(configuration, atLeastOnce()).isActivatedActivityHandler(realmName, handlerName);
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
            verify(activityHandler, atLeast(0)).getSupportedStorageStateClasses();
            verify(activityHandler, atLeast(0)).getLimitPolicy();
            verify(activityHandler, atLeastOnce()).getHandlerName();
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
            verify(realm, atLeastOnce()).spoolProcessAsset();
            verify(mediaAssetService, times(1)).getFromWatchfolder(realmName, storageName, file);
            verify(mediaAssetService, times(1)).resetDetectedMetadatas(List.of(fileEntity));
            verify(mediaAssetService, times(1)).updateIndexer(fileEntity);
            verify(activityHandler, times(1)).canHandle(fileEntity, eventType, configuredEnv);
            verify(activityHandler, atLeastOnce()).getHandlerName();
            verify(activityHandler, atLeast(0)).getSupportedStorageStateClasses();
            verify(activityHandler, atLeast(0)).getLimitPolicy();
            verify(activityHandler, atLeastOnce()).isEnabled();
            verify(fileEntity, atLeastOnce()).isDirectory();
            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getStorage();
            verify(fileEntity, atLeastOnce()).getName();
            verify(pendingActivityDao, times(2)).declateActivities(anyList(), eq(Optional.ofNullable(instanceEntity)));
            verify(pendingActivityDao, times(1)).haveDeclaredActivity(fileEntity, activityHandler);
            verify(pendingActivityDao, times(1)).havePendingActivities(fileEntity);
            verify(instanceDao, times(2)).getSelfInstance();
            verify(configuration, times(1)).getRealmAndStorage(realmName, storageName);
            verify(storage, atLeastOnce()).getStorageStateClass();
            verify(storage, atLeastOnce()).activityLimit();
        }

        @Test
        void testStartsActivities_disabledConfActivityLimitPolicy() {
            when(storage.activityLimit()).thenReturn(DISABLED);

            pas.startsActivities(realmName, storageName, realm, Set.of(file), eventType);

            verify(file, atLeastOnce()).isDirectory();
            verify(mediaAssetService, times(1)).getFromWatchfolder(realmName, storageName, file);
            verify(mediaAssetService, times(1)).resetDetectedMetadatas(List.of(fileEntity));
            verify(activityHandler, atLeastOnce()).getHandlerName();
            verify(activityHandler, atLeastOnce()).isEnabled();
            verify(fileEntity, atLeastOnce()).isDirectory();
            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getStorage();
            verify(configuration, times(1)).getRealmAndStorage(realmName, storageName);
            verify(storage, atLeastOnce()).activityLimit();
        }

        @Test
        void testStartsActivities_lowerConfActivityLimitPolicy() {
            when(storage.activityLimit()).thenReturn(TYPE_EXTRACTION);

            System.out.println(activityHandler.getLimitPolicy());

            pas.startsActivities(realmName, storageName, realm, Set.of(file), eventType);

            verify(file, atLeastOnce()).isDirectory();
            verify(mediaAssetService, times(1)).getFromWatchfolder(realmName, storageName, file);
            verify(mediaAssetService, times(1)).resetDetectedMetadatas(List.of(fileEntity));
            verify(activityHandler, atLeastOnce()).getHandlerName();
            verify(activityHandler, atLeast(1)).getLimitPolicy();
            verify(activityHandler, atLeastOnce()).isEnabled();
            verify(fileEntity, atLeastOnce()).isDirectory();
            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getStorage();
            verify(configuration, times(1)).getRealmAndStorage(realmName, storageName);
            verify(storage, atLeastOnce()).activityLimit();
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
        pendingActivityJob = new PendingActivityJob(
                configuredEnv,
                fileEntity,
                activityHandler,
                eventType,
                new HashSet<>(Set.of(handlerName)),
                handlerName,
                pendingActivityDao, pas);

        pas.continueAssetActivity(pendingActivityJob);

        verify(activityHandler, atLeastOnce()).getHandlerName();
        verify(activityHandler, atLeastOnce()).isEnabled();
        verify(pendingActivityDao, times(1)).declateActivities(anyList(), eq(Optional.ofNullable(instanceEntity)));
        verify(pendingActivityDao, times(1)).havePendingActivities(fileEntity);
        verify(instanceDao, times(1)).getSelfInstance();
        verify(realm, atLeastOnce()).spoolProcessAsset();
        verify(storage, atLeastOnce()).activityLimit();
        verify(fileEntity, atLeastOnce()).getRealm();
        verify(configuration, atLeastOnce()).isActivatedActivityHandler(realmName, handlerName);
        verify(mediaAssetService, times(1)).updateIndexer(fileEntity);
    }

    @Test
    void testContinueAssetActivity_stillHavePendingActivities() {
        when(pendingActivityDao.havePendingActivities(any())).thenReturn(true);

        pendingActivityJob = new PendingActivityJob(
                configuredEnv,
                fileEntity,
                activityHandler,
                eventType,
                new HashSet<>(Set.of(handlerName)),
                handlerName,
                pendingActivityDao, pas);

        pas.continueAssetActivity(pendingActivityJob);

        verify(pendingActivityDao, times(1)).havePendingActivities(fileEntity);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testRestartPendingActivities_empty(final boolean firstBoot) {
        when(pendingActivityDao.restartCurrentInstancePendingActivities(anyBoolean())).thenReturn(Map.of());

        pas.restartPendingActivities(firstBoot);

        verify(pendingActivityDao, times(1)).restartCurrentInstancePendingActivities(firstBoot);
    }

    @Nested
    class RestartPendingActivities {

        @Fake
        boolean firstBoot;
        @Fake
        int activityFileId;
        @Fake
        String unknownHander;

        @BeforeEach
        void init() {
            when(pendingActivityDao.restartCurrentInstancePendingActivities(firstBoot))
                    .thenReturn(Map.of(fileEntity, Set.of(entity)));
            when(pendingActivityDao.haveDeclaredActivity(fileEntity, activityHandler)).thenReturn(false);
            when(entity.getHandlerName()).thenReturn(handlerName);
            when(entity.getPreviousHandlers()).thenReturn(handlerName);
            when(entity.getEventType()).thenReturn(eventType.name());
        }

        @AfterEach
        void end() {
            verify(pendingActivityDao, times(1)).restartCurrentInstancePendingActivities(firstBoot);
            verify(entity, atLeastOnce()).getHandlerName();
            verify(entity, atLeastOnce()).getPreviousHandlers();

            verify(activityHandler, atLeastOnce()).getHandlerName();
            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getStorage();
            verify(configuration, atLeastOnce()).isActivatedActivityHandler(realmName, handlerName);
        }

        @Test
        void test() {
            pas.restartPendingActivities(firstBoot);

            verify(pendingActivityDao, times(1))
                    .haveDeclaredActivity(fileEntity, activityHandler);
            verify(pendingActivityDao, times(1)).declateActivities(anyList(), eq(Optional.ofNullable(instanceEntity)));
            verify(pendingActivityDao, times(1)).havePendingActivities(fileEntity);
            verify(instanceDao, times(1)).getSelfInstance();
            verify(entity, atLeastOnce()).getEventType();
            verify(realm, atLeastOnce()).spoolProcessAsset();
            verify(fileEntity, atLeastOnce()).getName();
            verify(activityHandler, atLeast(0)).getSupportedStorageStateClasses();
            verify(activityHandler, atLeastOnce()).isEnabled();
            verify(activityHandler, atLeast(0)).getLimitPolicy();
            verify(storage, atLeastOnce()).activityLimit();
            verify(mediaAssetService, times(1)).updateIndexer(fileEntity);
        }

        @Test
        void test_unknownHander() {
            when(entity.getHandlerName()).thenReturn(unknownHander);
            pas.restartPendingActivities(firstBoot);
            verify(activityHandler, atLeast(0)).getSupportedStorageStateClasses();
            verify(activityHandler, atLeastOnce()).isEnabled();
            verify(activityHandler, atLeast(0)).getLimitPolicy();
        }

    }

    @Test
    void testCleanupFiles() {
        pas.cleanupFiles(realmName, storageName, realm, Set.of(lostedFile));
        verify(mediaAssetService, times(1)).purgeAssetArtefacts(realmName, storageName, lostedFile);
    }

}
