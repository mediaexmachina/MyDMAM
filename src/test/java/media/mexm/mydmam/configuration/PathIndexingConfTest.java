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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import media.mexm.mydmam.component.AuditTrail;
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
	@Mock
	AuditTrail auditTrail;

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

}
