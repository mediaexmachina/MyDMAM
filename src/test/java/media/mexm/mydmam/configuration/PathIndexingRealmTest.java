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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class PathIndexingRealmTest {

	@Mock
	Duration timeBetweenScans;
	@Fake
	String spoolScans;
	@Fake
	String storageName;
	@Mock
	PathIndexingStorage piStorage;

	PathIndexingRealm conf;

	@BeforeEach
	void init() {
		conf = new PathIndexingRealm(
				Map.of(storageName, piStorage),
				timeBetweenScans,
				spoolScans);
	}

	@Test
	void testStoragesStream() {
		final var result = conf.storagesStream().toList();
		assertThat(result).size().isEqualTo(1);
		final var entry = result.get(0);

		assertEquals(storageName, entry.getKey());
		assertEquals(piStorage, entry.getValue());
	}

	@Test
	void testStoragesStream_empty() {
		conf = new PathIndexingRealm(
				Map.of(),
				timeBetweenScans,
				spoolScans);

		final var result = conf.storagesStream().toList();
		assertThat(result).isEmpty();
	}

	@Test
	void testStoragesStream_null() {
		conf = new PathIndexingRealm(null, timeBetweenScans, spoolScans);

		final var result = conf.storagesStream().toList();
		assertThat(result).isEmpty();
	}

}
