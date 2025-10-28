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
package media.mexm.mydmam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.internal.util.MockUtil.isMock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.jobkit.engine.JobKitEngine;

class MockService {

	@Configuration
	@Profile({ "FlatJobKit" })
	static class FlatJobKit {

		@Bean
		@Primary
		FlatJobKitEngine flatJobKitEngine() {
			return new FlatJobKitEngine();
		}

	}

	/*
	 * =========
	 * TEST ZONE
	 * =========
	 */

	@SpringBootTest
	@ActiveProfiles({ "FlatJobKit" })
	static class TestFlatJobKit {

		@Autowired
		JobKitEngine jobKitEngine;
		@Autowired
		FlatJobKitEngine flatJobKitEngine;

		@Test
		void test() {
			assertFalse(isMock(jobKitEngine));
			assertEquals(jobKitEngine, flatJobKitEngine);
			assertTrue(flatJobKitEngine.isEmptyActiveServicesList());
			assertEquals(0, flatJobKitEngine.getEndEventsList().size());
		}
	}

}
