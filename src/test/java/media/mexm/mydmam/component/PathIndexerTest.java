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
package media.mexm.mydmam.component;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.PathIndexingConf;
import media.mexm.mydmam.pathindexing.RealmStorageFolderActivity;
import media.mexm.mydmam.service.PathIndexerService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.jobkit.watchfolder.Watchfolders;

@ExtendWith(MockToolsExtendsJunit.class)
class PathIndexerTest {

	@Fake
	String realm;
	@Fake
	String storage;
	@Fake
	String falseRealm;
	@Fake
	String falseStorage;
	@Fake
	String spoolEvents;

	@Mock
	MyDMAMConfigurationProperties configuration;

	@Mock
	PathIndexerService pathIndexerService;
	@Mock
	PathIndexingConf pathIndexingConf;
	@Mock
	RealmStorageFolderActivity realmStorageFolderActivity;
	@Mock
	Watchfolders watchfolders;
	@Mock
	ObservedFolder observedFolder;
	@Mock
	WatchedFiles watchedFiles;
	@Mock
	AuditTrail auditTrail;

	FlatJobKitEngine jobKitEngine;
	PathIndexer pi;

	@BeforeEach
	void init() {
		jobKitEngine = new FlatJobKitEngine();
	}

	@Nested
	class WithWatchfolder {

		@BeforeEach
		void init() {
			when(configuration.pathindexing()).thenReturn(pathIndexingConf);
			when(pathIndexingConf.makeWatchfolders(notNull())).thenReturn(Map.of(realmStorageFolderActivity,
					watchfolders));

			pi = new PathIndexer(configuration, jobKitEngine, pathIndexerService, auditTrail);
			verify(configuration, atLeastOnce()).pathindexing();
			verify(pathIndexingConf, times(1)).makeWatchfolders(pi);
		}

		@Test
		void testStarts() {
			pi.starts();
			verify(watchfolders, times(1)).startScans();
		}

		@Test
		void testEnds() {
			pi.ends();
			verify(watchfolders, times(1)).stopScans();
		}

		@Test
		void testScanNow_withResults() throws IOException {
			when(realmStorageFolderActivity.realmName()).thenReturn(realm);
			when(realmStorageFolderActivity.storageName()).thenReturn(storage);
			when(pathIndexingConf.getSpoolEvents()).thenReturn(spoolEvents);
			when(watchfolders.manualScan()).thenReturn(Map.of(observedFolder, watchedFiles));

			pi.scanNow(realm, storage);

			verify(realmStorageFolderActivity, atLeastOnce()).realmName();
			verify(realmStorageFolderActivity, atLeastOnce()).storageName();
			verify(pathIndexingConf, atLeastOnce()).getSpoolEvents();
			verify(watchfolders, times(1)).manualScan();
			verify(realmStorageFolderActivity, times(1)).onAfterScan(eq(observedFolder), notNull(), eq(watchedFiles));
		}

		@Test
		void testScanNow_withoutResults() {
			when(realmStorageFolderActivity.realmName()).thenReturn(realm);
			when(realmStorageFolderActivity.storageName()).thenReturn(storage);
			when(pathIndexingConf.getSpoolEvents()).thenReturn(spoolEvents);
			when(watchfolders.manualScan()).thenReturn(Map.of());

			pi.scanNow(realm, storage);

			verify(realmStorageFolderActivity, atLeastOnce()).realmName();
			verify(realmStorageFolderActivity, atLeastOnce()).storageName();
			verify(pathIndexingConf, atLeastOnce()).getSpoolEvents();
			verify(watchfolders, times(1)).manualScan();
		}

		@Test
		void testScanNow_badRealm() {
			when(realmStorageFolderActivity.realmName()).thenReturn(falseRealm);

			pi.scanNow(realm, storage);

			verify(realmStorageFolderActivity, atLeastOnce()).realmName();
		}

		@Test
		void testScanNow_badStorage() {
			when(realmStorageFolderActivity.realmName()).thenReturn(realm);
			when(realmStorageFolderActivity.storageName()).thenReturn(falseStorage);

			pi.scanNow(realm, storage);

			verify(realmStorageFolderActivity, atLeastOnce()).realmName();
			verify(realmStorageFolderActivity, atLeastOnce()).storageName();
		}

		@AfterEach
		void ends() {
			assertTrue(jobKitEngine.isEmptyActiveServicesList());
		}

	}

	@Nested
	class WithoutWatchfolder {

		@BeforeEach
		void init() {
			pi = new PathIndexer(configuration, jobKitEngine, pathIndexerService, auditTrail);
			verify(configuration, atLeastOnce()).pathindexing();
		}

		@Test
		void testStarts() { // NOSONAR S2699
			pi.starts();
		}

		@Test
		void testEnds() { // NOSONAR S2699
			pi.ends();
		}

		@Test
		void testScanNow() {// NOSONAR S2699
			pi.scanNow(realm, storage);
		}

		@AfterEach
		void ends() {
			assertTrue(jobKitEngine.isEmptyActiveServicesList());
		}
	}

}
