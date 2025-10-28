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
package media.mexm.mydmam.configuration;

import static media.mexm.mydmam.configuration.PathIndexingConf.correctName;
import static media.mexm.mydmam.entity.FileEntity.MAX_NAME_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.time.Duration;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import media.mexm.mydmam.component.PathIndexer;
import media.mexm.mydmam.service.PathIndexerService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFileScanner;

@ExtendWith(MockToolsExtendsJunit.class)
class PathIndexingConfTest {

	@Fake
	String rawname;
	@Fake
	String fieldName;

	@Test
	void testCorrectName() {
		assertThat(correctName(rawname + "\u00a0 \t_\r \n" + rawname + "a  ?  ééé€_A?a-a12+d" + rawname, fieldName))
				.isEqualTo(rawname + "___" + rawname + "a_eee__A_a-a12_d" + rawname);

		final var longString = StringUtils.repeat(rawname, MAX_NAME_SIZE);
		assertThat(correctName(longString, fieldName)).hasSize(MAX_NAME_SIZE);

		assertThrows(NullPointerException.class, () -> correctName(null, fieldName));
		assertThrows(IllegalArgumentException.class, () -> correctName("", fieldName));
	}

	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	String spoolScans;
	@Fake
	String spoolEvents;
	@Fake(min = 11, max = 100)
	int maxDeep;
	@Fake(min = 1, max = 10000)
	long duration;
	Duration timeBetweenScans;

	@Mock
	PathIndexingRealm piRealm;
	@Mock
	PathIndexingStorage piStorage;
	@Mock
	PathIndexer indexer;
	@Mock
	PathIndexerService pathIndexerService;
	@Mock
	ObservedFolder scan;

	@Captor
	ArgumentCaptor<WatchedFileScanner> watchedFileScannerCaptor;

	FlatJobKitEngine jobKitEngine;
	PathIndexingConf conf;

	@BeforeEach
	void init() {
		jobKitEngine = new FlatJobKitEngine();
		timeBetweenScans = Duration.ofMillis(duration);

		conf = new PathIndexingConf(
				Map.of(realmName, piRealm),
				timeBetweenScans,
				spoolScans,
				spoolEvents);

		when(indexer.getJobKitEngine()).thenReturn(jobKitEngine);
		when(indexer.getPathIndexerService()).thenReturn(pathIndexerService);
		when(piRealm.storagesStream())
				.thenReturn(Map.of(storageName, piStorage).entrySet().stream());
		when(piStorage.scan()).thenReturn(scan);
		when(piStorage.getDefaultMaxDeep()).thenReturn(maxDeep);
		when(scan.isRecursive()).thenReturn(true);
	}

	@Test
	void testGetSpoolEvents_setted() {
		assertEquals(spoolEvents, conf.getSpoolEvents());
	}

	@Test
	void testGetSpoolEvents_default() {
		conf = new PathIndexingConf(Map.of(realmName, piRealm), timeBetweenScans, spoolScans, null);
		assertEquals("pathindexing", conf.getSpoolEvents());
		conf = new PathIndexingConf(Map.of(realmName, piRealm), timeBetweenScans, spoolScans, "");
		assertEquals("pathindexing", conf.getSpoolEvents());
	}

	@Test
	void testMakeWatchfolders() {
		final var result = conf.makeWatchfolders(indexer);

		assertNotNull(result);
		assertThat(result).isNotNull().hasSize(1);
		final var result1st = result.entrySet().stream().findFirst().get();
		final var fa = result1st.getKey();
		assertEquals(indexer, fa.indexer());
		assertEquals(piRealm, fa.realm());
		assertEquals(realmName, fa.realmName());
		assertEquals(piStorage, fa.storage());
		assertEquals(storageName, fa.storageName());

		result1st.getValue().startScans();
		jobKitEngine.runAllServicesOnce();

		verify(indexer, atLeastOnce()).getJobKitEngine();
		verify(indexer, atLeastOnce()).getPathIndexerService();
		verify(piRealm, times(1)).storagesStream();
		verify(piRealm, atLeastOnce()).spoolScans();
		verify(piRealm, atLeastOnce()).timeBetweenScans();
		verify(piStorage, atLeastOnce()).scan();
		verify(piStorage, atLeastOnce()).spoolScans();
		verify(piStorage, atLeastOnce()).timeBetweenScans();
		verify(piStorage, atLeastOnce()).getDefaultMaxDeep();

		verify(pathIndexerService, times(1)).updateFoundedFiles(
				watchedFileScannerCaptor.capture(), eq(realmName), eq(storageName), eq(scan), any());

		verify(scan, atLeast(1)).createFileSystem();
		assertEquals(maxDeep, watchedFileScannerCaptor.getValue().getMaxDeep());

		assertFalse(jobKitEngine.isEmptyActiveServicesList());
		Mockito.reset(scan);
	}

}
