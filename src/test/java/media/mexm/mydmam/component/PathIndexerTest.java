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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.InfraConf;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.configuration.TechnicalName;
import media.mexm.mydmam.service.PathIndexerService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;

@ExtendWith(MockToolsExtendsJunit.class)
class PathIndexerTest {

	@Fake
	String realm;
	@Fake
	String storage;
	@Fake
	String spoolEvents;

	@Mock
	PathIndexerService pathIndexerService;
	@Mock
	MyDMAMConfigurationProperties configuration;
	@Mock
	InfraConf infra;

	private FlatJobKitEngine jobKitEngine;
	private PathIndexer pi;
	private Duration duration;

	@BeforeEach
	void init() {
		jobKitEngine = new FlatJobKitEngine();
		duration = Duration.ofHours(1);
	}

	@Nested
	class WithWatchfolder {

		ObservedFolder scan;

		PathIndexingStorage piStorage;
		RealmConf piRealm;

		@BeforeEach
		void init() {
			scan = new ObservedFolder();
			scan.setTargetFolder(new File(".").getAbsolutePath());
			scan.setLabel("test");

			piStorage = new PathIndexingStorage(scan, 0, duration, spoolEvents);
			piRealm = new RealmConf(Map.of(new TechnicalName(storage), piStorage),
					duration, spoolEvents, null);

			when(configuration.infra()).thenReturn(infra);
			when(infra.spoolEvents()).thenReturn(spoolEvents);
			when(infra.realms()).thenReturn(Map.of(new TechnicalName(realm), piRealm));

			pi = new PathIndexer(jobKitEngine, pathIndexerService, configuration);
			pi.startScans();
		}

		@AfterEach
		void ends() {
			verify(configuration, atLeastOnce()).infra();
			verify(infra, atLeastOnce()).spoolEvents();
			verify(infra, atLeastOnce()).realms();
			verify(infra, atLeastOnce()).timeBetweenScans();
			assertFalse(jobKitEngine.isEmptyActiveServicesList());

			assertThat(jobKitEngine.getEndEventsList()).size().isGreaterThan(0);
		}

		@Test
		void testGetWatchfolders() {
			final var result = pi.getWatchfolders();
			assertThat(result).size().isEqualTo(1);

			final var realmStorageFolderActivity = result.keySet().iterator().next();
			assertNotNull(realmStorageFolderActivity.pathIndexerService());
			assertEquals(realm, realmStorageFolderActivity.realmName());
			assertEquals(piRealm, realmStorageFolderActivity.realm());
			assertEquals(storage, realmStorageFolderActivity.storageName());
			assertEquals(piStorage, realmStorageFolderActivity.storage());
		}

		@Test
		void testScanNow() {
			pi.scanNow(realm, storage);

			verify(configuration, atLeastOnce()).infra();
			verify(infra, atLeastOnce()).spoolEvents();
			verify(pathIndexerService, times(1)).updateFoundedFiles(
					any(), eq(realm), eq(storage), any(), any());
		}

		@Test
		void testScanNow_badRealm_badStorage() {
			pi.scanNow(realm, storage);

			verify(pathIndexerService, times(1)).updateFoundedFiles(
					any(), eq(realm), eq(storage), any(), any());
		}

	}

	@Nested
	class WithoutWatchfolder {

		@BeforeEach
		void init() {
			pi = new PathIndexer(jobKitEngine, pathIndexerService, configuration);
			pi.startScans();
		}

		@Test
		void testScanNow() {
			pi.scanNow(realm, storage);
			verify(configuration, times(1)).infra();
		}

		@AfterEach
		void ends() {
			assertTrue(jobKitEngine.isEmptyActiveServicesList());
			assertThat(jobKitEngine.getEndEventsList()).isEmpty();
		}
	}

}
