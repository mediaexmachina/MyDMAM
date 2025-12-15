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

import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.apache.commons.io.FileUtils.touch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class PathIndexingRealmTest {

	@Mock
	Duration mockTimeBetweenScans;
	@Mock
	PathIndexingStorage piStorage;

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
	PathIndexingRealm conf;

	@BeforeEach
	void init() {
		workingDirectory = new File(getTempDirectory(), "mydmam-" + getClass().getSimpleName());
		conf = new PathIndexingRealm(
				Map.of(new TechnicalName(storageName), piStorage),
				mockTimeBetweenScans,
				spool,
				workingDirectory);
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
		conf = new PathIndexingRealm(
				Map.of(),
				mockTimeBetweenScans,
				spool,
				null);

		final var result = conf.storages().entrySet().stream().toList();
		assertThat(result).isEmpty();
	}

	@Test
	void testStoragesStream_null() {
		conf = new PathIndexingRealm(null, mockTimeBetweenScans, spool, null);

		final var result = conf.storages().entrySet().stream().toList();
		assertThat(result).isEmpty();
	}

	@Test
	void testGetValidWorkingDirectory_empty() {
		conf = new PathIndexingRealm(null, mockTimeBetweenScans, spool, null);
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
		assertThrows(UncheckedIOException.class, () -> new PathIndexingRealm(
				map,
				mockTimeBetweenScans,
				spool,
				workingDirectory));
	}

	@Test
	void testTimeBetweenScans() {
		final var map = Map.of(new TechnicalName(storageName), piStorage);

		conf = new PathIndexingRealm(map, null, spool, workingDirectory);
		assertThat(conf.timeBetweenScans()).isNull();

		timeBetweenScans = Duration.ofMillis(duration);
		conf = new PathIndexingRealm(map, timeBetweenScans, spool, workingDirectory);
		assertThat(conf.timeBetweenScans()).isEqualTo(Duration.ofMillis(duration));

		timeBetweenScans = Duration.ZERO;
		assertThrows(IllegalArgumentException.class,
				() -> new PathIndexingRealm(map, timeBetweenScans, spool, workingDirectory));
		timeBetweenScans = Duration.ofMillis(-duration);
		assertThrows(IllegalArgumentException.class,
				() -> new PathIndexingRealm(map, timeBetweenScans, spool, workingDirectory));
	}

}
