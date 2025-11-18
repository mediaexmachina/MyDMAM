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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class MyDMAMConfigurationPropertiesTest {

	@Mock
	PathIndexingConf pathindexing;
	@Mock
	PathIndexingRealm realm;

	@Fake
	String realmName;
	@Fake
	String otherRealm;

	MyDMAMConfigurationProperties c;

	@BeforeEach
	void init() {
		c = new MyDMAMConfigurationProperties(pathindexing);
	}

	@Test
	void testGetRealmNames_empty() {
		assertThat(c.getRealmNames()).isEmpty();
		verify(pathindexing, times(1)).realms();
	}

	@Test
	void testGetRealmNames() {
		when(pathindexing.realms()).thenReturn(Map.of(realmName, realm));
		assertThat(c.getRealmNames()).containsOnly(realmName);
		verify(pathindexing, times(1)).realms();
	}

	@Test
	void testGetRealmByName_contains() {
		when(pathindexing.realms()).thenReturn(Map.of(realmName, realm));
		assertThat(c.getRealmByName(realmName)).contains(realm);
		verify(pathindexing, times(1)).realms();
	}

	@Test
	void testGetRealmByName_notContains() {
		when(pathindexing.realms()).thenReturn(Map.of(realmName, realm));
		assertThat(c.getRealmByName(otherRealm)).isEmpty();
		verify(pathindexing, times(1)).realms();
	}

}
