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

import static media.mexm.mydmam.configuration.PathIndexingStorage.DEFAULT_MAX_DEEP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
	@Fake(min = 1, max = 1000)
	int maxDeep;
	@Mock
	Duration timeBetweenScans;
	@Fake
	String spoolScans;

	PathIndexingStorage conf;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(timeBetweenScans);
	}

	@Test
	void testGetDefaultMaxDeep() {
		conf = new PathIndexingStorage(scan, maxDeep, timeBetweenScans, spoolScans);
		assertThat(conf.getDefaultMaxDeep()).isEqualTo(maxDeep);
	}

	@Test
	void testGetDefaultMaxDeep_zero() {
		conf = new PathIndexingStorage(scan, 0, timeBetweenScans, spoolScans);
		assertThat(conf.getDefaultMaxDeep()).isEqualTo(DEFAULT_MAX_DEEP);
	}

}
