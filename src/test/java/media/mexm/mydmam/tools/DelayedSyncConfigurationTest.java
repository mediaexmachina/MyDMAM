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
 * Copyright (C) Media ex Machina 2026
 *
 */
package media.mexm.mydmam.tools;

import static java.time.Duration.ZERO;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class DelayedSyncConfigurationTest {

	private static final Duration ONE_HOUR = Duration.ofHours(1);
	DelayedSyncConfiguration dsc;

	@Test
	void testDelayedSyncConfiguration_maxEntries() {
		new DelayedSyncConfiguration(1000, ONE_HOUR);
		new DelayedSyncConfiguration(2, ONE_HOUR);

		assertThrows(IllegalArgumentException.class, () -> new DelayedSyncConfiguration(1, ONE_HOUR));
		assertThrows(IllegalArgumentException.class, () -> new DelayedSyncConfiguration(0, ONE_HOUR));
		assertThrows(IllegalArgumentException.class, () -> new DelayedSyncConfiguration(-1, ONE_HOUR));
	}

	@Test
	void testDelayedSyncConfiguration_maxDelay() {
		assertThrows(IllegalArgumentException.class, () -> new DelayedSyncConfiguration(1, ZERO));
	}

}
