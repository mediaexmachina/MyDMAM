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
package media.mexm.mydmam.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.indexer.RealmIndexer;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.repository.PendingActivityDao;
import media.mexm.mydmam.service.PendingActivityService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class PendingActivityJobTest {

	@Mock
	MediaAsset asset;
	@Mock
	ActivityHandler activityHandler;
	@Mock
	ActivityHandler newActivityHandler;
	@Mock
	PendingActivityDao pendingActivityDao;
	@Mock
	PendingActivityService pendingActivityService;
	@Mock
	RealmIndexer realmIndexer;
	@Mock
	RealmConf realm;
	@Mock
	PathIndexingStorage storage;
	@Mock
	HandlingResult handlingResult;
	@Mock
	FileEntity file;

	@Fake
	ActivityEventType eventType;
	@Fake
	String spoolName;
	@Fake
	String previousHandler;
	@Fake
	String handlerName;
	@Fake
	String assetName;
	@Fake
	String assetHashPath;
	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	String previousHandlersJson;

	Set<String> previousHandlers;
	PendingActivityJob job;
	RealmStorageConfiguredEnv configuredEnv;
	Exception e;
	Optional<RealmIndexer> oIndexer;

	@BeforeEach
	void init() throws Exception {
		oIndexer = Optional.ofNullable(realmIndexer);
		configuredEnv = new RealmStorageConfiguredEnv(realmName, storageName, realm, storage);

		when(asset.getHashPath()).thenReturn(assetHashPath);
		when(asset.getFile()).thenReturn(file);
		when(activityHandler.getHandlerName()).thenReturn(handlerName);
		when(activityHandler.handle(asset, eventType, configuredEnv)).thenReturn(handlingResult);
		when(pendingActivityDao.haveDeclaredActivity(file, activityHandler)).thenReturn(true);
		when(asset.getName()).thenReturn(assetName);
		when(realm.spoolProcessAsset()).thenReturn(spoolName);

		previousHandlers = new HashSet<>(Set.of(previousHandler));
		job = new PendingActivityJob(
				configuredEnv,
				asset,
				activityHandler,
				eventType,
				previousHandlers,
				previousHandlersJson,
				pendingActivityDao,
				pendingActivityService,
				oIndexer);
		e = new Exception("For tests purpose");
	}

	@Test
	void testEvolve() {
		final var evolved = job.evolve(newActivityHandler, previousHandlers, previousHandlersJson);
		assertEquals(configuredEnv, evolved.configuredEnv());
		assertEquals(asset, evolved.asset());
		assertEquals(newActivityHandler, evolved.activityHandler());
		assertEquals(eventType, evolved.eventType());
		assertEquals(previousHandlers, evolved.previousHandlers());
		assertEquals(pendingActivityDao, evolved.pendingActivityDao());
		assertEquals(pendingActivityService, evolved.pendingActivityService());
	}

	@Test
	void testRun() throws Exception {
		job.run();

		verify(activityHandler, times(1)).handle(asset, eventType, configuredEnv);
		verify(activityHandler, atLeastOnce()).getHandlerName();
		verify(asset, atLeastOnce()).getFile();
		verify(asset, atLeastOnce()).commit(Optional.ofNullable(realmIndexer));
		verify(pendingActivityDao, times(1)).haveDeclaredActivity(file, activityHandler);
		verify(pendingActivityDao, times(1)).endsActivity(file, activityHandler);
		verify(pendingActivityService, times(1)).continueAssetActivity(job);
	}

	@Test
	void testRun_butNoDeclared() throws Exception {
		when(pendingActivityDao.haveDeclaredActivity(file, activityHandler)).thenReturn(false);
		job.run();

		verify(pendingActivityDao, times(1)).haveDeclaredActivity(file, activityHandler);
		verify(activityHandler, times(1)).getHandlerName();
		verify(asset, times(1)).getFile();
		verify(pendingActivityService, times(1)).continueAssetActivity(job);
	}

	@Test
	void testOnJobFail() {
		job.onJobFail(e);
		verify(asset, times(1)).getFile();
		verify(pendingActivityDao, times(1)).endsActivity(file, activityHandler);
	}

	@Test
	void testGetJobName() {
		assertThat(job.getJobName()).contains(handlerName, assetName);

		verify(activityHandler, times(1)).getHandlerName();
		verify(asset, times(1)).getName();
	}

	@Test
	void testGetJobSpoolname() {
		assertEquals(spoolName, job.getJobSpoolname());
		verify(realm, times(1)).spoolProcessAsset();
	}

}
