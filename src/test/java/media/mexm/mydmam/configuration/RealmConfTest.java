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
package media.mexm.mydmam.configuration;

import static java.util.function.Predicate.not;
import static media.mexm.mydmam.dto.StorageCategory.DAS;
import static media.mexm.mydmam.dto.StorageCategory.EXTERNAL;
import static media.mexm.mydmam.dto.StorageCategory.NAS;
import static media.mexm.mydmam.dto.StorageStateClass.NEARLINE;
import static media.mexm.mydmam.dto.StorageStateClass.OFFLINE;
import static media.mexm.mydmam.dto.StorageStateClass.ONLINE;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.apache.commons.io.FileUtils.touch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import media.mexm.mydmam.dto.StorageCategory;
import media.mexm.mydmam.dto.StorageStateClass;
import media.mexm.mydmam.tools.DelayedSyncConfiguration;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class RealmConfTest {

	@Mock
	Duration mockTimeBetweenScans;
	@Mock
	PathIndexingStorage piStorage;
	@Mock
	DelayedSyncConfiguration delayedSyncConfiguration;

	@Fake
	String spool;
	@Fake
	String storageName;
	@Fake
	String realmName;
	@Fake(min = 1, max = 10000)
	long duration;

	Duration timeBetweenScans;
	File workingDirectory;
	RealmConf conf;

	@BeforeEach
	void init() {
		workingDirectory = new File(getTempDirectory(), "mydmam-" + getClass().getSimpleName());
		conf = new RealmConf(
				Map.of(new TechnicalName(storageName), piStorage),
				mockTimeBetweenScans,
				spool,
				workingDirectory,
				delayedSyncConfiguration);
	}

	@AfterEach
	void ends() throws IOException {
		Mockito.reset(mockTimeBetweenScans);
		if (workingDirectory.exists()) {
			forceDelete(workingDirectory);
		}
	}

	@Test
	void testStoragesStream() {
		final var result = conf.storages().entrySet().stream().toList();
		assertThat(result).size().isEqualTo(1);
		final var entry = result.get(0);

		assertEquals(new TechnicalName(storageName), entry.getKey());
		assertEquals(piStorage, entry.getValue());
	}

	@Test
	void testStoragesStream_empty() {
		conf = new RealmConf(
				Map.of(),
				mockTimeBetweenScans,
				spool,
				null,
				null);

		final var result = conf.storages().entrySet().stream().toList();
		assertThat(result).isEmpty();
	}

	@Test
	void testStoragesStream_null() {
		conf = new RealmConf(null, mockTimeBetweenScans, spool, null, null);

		final var result = conf.storages().entrySet().stream().toList();
		assertThat(result).isEmpty();
	}

	@Test
	void testGetValidWorkingDirectory_empty() {
		conf = new RealmConf(null, mockTimeBetweenScans, spool, null, null);
		assertThat(conf.workingDirectory()).isNull();
	}

	@Test
	void testGetValidWorkingDirectory_exists() {
		assertThat(conf.workingDirectory()).isEqualTo(workingDirectory);
	}

	@Test
	void testGetValidWorkingDirectory_notExists() throws IOException {
		if (workingDirectory.exists()) {
			forceDelete(workingDirectory);
		}
		assertThat(conf.workingDirectory()).isEqualTo(workingDirectory);
	}

	@Test
	void testGetValidWorkingDirectory_invalid() throws IOException {
		if (workingDirectory.exists()) {
			forceDelete(workingDirectory);
		}
		touch(workingDirectory);
		final var map = Map.of(new TechnicalName(storageName), piStorage);
		assertThrows(UncheckedIOException.class, () -> new RealmConf(
				map,
				mockTimeBetweenScans,
				spool,
				workingDirectory,
				delayedSyncConfiguration));
	}

	@Test
	void testTimeBetweenScans() {
		final var map = Map.of(new TechnicalName(storageName), piStorage);

		conf = new RealmConf(map, null, spool, workingDirectory, delayedSyncConfiguration);
		assertThat(conf.timeBetweenScans()).isNull();

		timeBetweenScans = Duration.ofMillis(duration);
		conf = new RealmConf(map, timeBetweenScans, spool, workingDirectory, delayedSyncConfiguration);
		assertThat(conf.timeBetweenScans()).isEqualTo(Duration.ofMillis(duration));

		timeBetweenScans = Duration.ZERO;
		assertThrows(IllegalArgumentException.class,
				() -> new RealmConf(map, timeBetweenScans, spool, workingDirectory, delayedSyncConfiguration));
		timeBetweenScans = Duration.ofMillis(-duration);
		assertThrows(IllegalArgumentException.class,
				() -> new RealmConf(map, timeBetweenScans, spool, workingDirectory, delayedSyncConfiguration));
	}

	@Test
	void testGetOnlineDASStorageNames() {
		when(piStorage.getCategory()).thenReturn(NAS);
		when(piStorage.getStorageStateClass()).thenReturn(NEARLINE);
		assertThat(conf.getOnlineDASStorageNames()).isEmpty();

		when(piStorage.getCategory()).thenReturn(EXTERNAL);
		assertThat(conf.getOnlineDASStorageNames()).isEmpty();

		when(piStorage.getCategory()).thenReturn(DAS);
		assertThat(conf.getOnlineDASStorageNames()).isEmpty();

		when(piStorage.getStorageStateClass()).thenReturn(OFFLINE);
		assertThat(conf.getOnlineDASStorageNames()).isEmpty();

		when(piStorage.getStorageStateClass()).thenReturn(ONLINE);
		assertThat(conf.getOnlineDASStorageNames()).containsExactly(storageName);

		verify(piStorage, atLeastOnce()).getCategory();
		verify(piStorage, atLeastOnce()).getStorageStateClass();
	}

	@Nested
	class StorageNames {

		@Fake
		StorageCategory sCategory;
		@Fake
		StorageStateClass sClasses;

		StorageCategory notSCategory;
		StorageStateClass notSClasses;

		@BeforeEach
		void init() {
			notSCategory = Stream.of(StorageCategory.values()).filter(not(f -> f.equals(sCategory))).findFirst().get();
			notSClasses = Stream.of(StorageStateClass.values()).filter(not(f -> f.equals(sClasses))).findFirst().get();

			when(piStorage.getCategory()).thenReturn(sCategory);
			when(piStorage.getStorageStateClass()).thenReturn(sClasses);
		}

		@AfterEach
		void ends() {
			verify(piStorage, atLeast(0)).getCategory();
			verify(piStorage, atLeast(0)).getStorageStateClass();
		}

		@Test
		void testAll_default() {
			assertThat(conf.getStorageNames(Set.of(), Set.of())).containsExactly(storageName);
		}

		@Test
		void testAll_set() {
			assertThat(conf.getStorageNames(
					Set.of(DAS, NAS, EXTERNAL),
					Set.of(ONLINE, NEARLINE, OFFLINE)))
							.containsExactly(storageName);
		}

		@Test
		void testBad_all() {
			assertThat(conf.getStorageNames(Set.of(notSCategory), Set.of(notSClasses))).isEmpty();
		}

		@Test
		void testBad_category() {
			assertThat(conf.getStorageNames(Set.of(notSCategory), Set.of())).isEmpty();
		}

		@Test
		void testBad_classes() {
			assertThat(conf.getStorageNames(Set.of(), Set.of(notSClasses))).isEmpty();
		}

	}

	@Test
	void testGetStorageByName() {
		assertThat(conf.getStorageByName(storageName)).contains(piStorage);
		assertThat(conf.getStorageByName("NOPE")).isEmpty();
	}

}
