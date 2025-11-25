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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.util.HashSet;
import java.util.List;
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
import media.mexm.mydmam.configuration.PathIndexingRealm;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.PendingActivityEntity;
import media.mexm.mydmam.repository.PendingActivityDao;
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

	@Mock
	PathIndexingRealm realm;
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
		when(file.isDirectory()).thenReturn(false);
		when(mediaAssetService.getFromWatchfolder(realmName, storageName, file)).thenReturn(asset);
		when(internalObjectMapper.writeValueAsString(Set.of(handlerName))).thenReturn(previousHandlersNames);
		when(aboutInstance.getPendingActivityHostName()).thenReturn(hostName);
		when(aboutInstance.getPid()).thenReturn(pid);
		when(asset.getHashPath()).thenReturn(hashPathItem);
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
		verify(mediaAssetService, times(1)).getFromWatchfolder(realmName, storageName, file);
		verify(realm, atLeastOnce()).spoolProcessAsset();
		verify(activityHandler, times(1)).canHandle(asset, eventType);
		verify(activityHandler, atLeastOnce()).getHandlerName();
		verify(asset, atLeastOnce()).getHashPath();
		verify(asset, atLeastOnce()).getName();
		verify(internalObjectMapper, times(1)).writeValueAsString(Set.of(handlerName));
		verify(aboutInstance, atLeastOnce()).getPendingActivityHostName();
		verify(aboutInstance, atLeastOnce()).getPid();
		verify(pendingActivityDao, times(1))
				.declateActivity(hashPathItem, activityHandler, eventType, previousHandlersNames, hostName, pid);

		verify(activityHandler, times(1)).handle(asset, eventType);
		verify(pendingActivityDao, times(1)).endsActivity(hashPathItem, activityHandler);
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
		verify(aboutInstance, atLeastOnce()).getPendingActivityHostName();
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
		verify(aboutInstance, atLeastOnce()).getPendingActivityHostName();
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
			when(mediaAssetService.getFromFileEntry(fileEntity)).thenReturn(asset);
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
			verify(aboutInstance, atLeastOnce()).getPendingActivityHostName();
			verify(entity, atLeastOnce()).getEventType();
			verify(entity, atLeastOnce()).getFile();
			verify(entity, atLeastOnce()).getPreviousHandlers();
			verify(entity, atLeastOnce()).getHandlerName();
			verify(fileEntity, atLeastOnce()).getRealm();
			verify(internalObjectMapper, times(1)).readValue(previousHandlersNames, TYPE_LIST_STRING);
			verify(mediaAssetService, times(1)).getFromFileEntry(fileEntity);
			verify(activityHandler, atLeastOnce()).getHandlerName();
		}

	}

	@Test
	void testCleanupFiles() {
		pas.cleanupFiles(realmName, storageName, realm, Set.of(lostedFile));
		verify(mediaAssetService, times(1)).purgeAssetArtefacts(realmName, storageName, lostedFile);
	}

}
