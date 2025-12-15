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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;

@ExtendWith(MockToolsExtendsJunit.class)
class PathIndexingStorageTest {

	@Mock
	ObservedFolder scan;

	@Fake
	String spoolScans;
	@Fake
	int maxDeep;
	@Fake(min = 1, max = 10000)
	long duration;

	PathIndexingStorage pis;
	Duration timeBetweenScans;

	@Test
	void testTimeBetweenScans() {
		pis = new PathIndexingStorage(scan, maxDeep, null, spoolScans);
		assertThat(pis.timeBetweenScans()).isNull();

		timeBetweenScans = Duration.ofMillis(duration);
		pis = new PathIndexingStorage(scan, maxDeep, timeBetweenScans, spoolScans);
		assertThat(pis.timeBetweenScans()).isEqualTo(Duration.ofMillis(duration));

		timeBetweenScans = Duration.ZERO;
		assertThrows(IllegalArgumentException.class,
				() -> new PathIndexingStorage(scan, maxDeep, timeBetweenScans, spoolScans));
		timeBetweenScans = Duration.ofMillis(-duration);
		assertThrows(IllegalArgumentException.class,
				() -> new PathIndexingStorage(scan, maxDeep, timeBetweenScans, spoolScans));

	}

}
