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

import static java.lang.Math.abs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import media.mexm.mydmam.tools.AllowBlockLists;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class MyDMAMConfigurationPropertiesTest {

	@Mock
	InfraConf pathindexing;
	@Mock
	RealmConf realm;
	@Mock
	PathIndexingStorage storage;
	@Mock
	MagickConf magick;
	@Mock
	AllowBlockLists activityHandlers;
	@Mock
	AllowBlockLists realmActivityHandlers;

	@Fake
	String realmName;
	@Fake
	String otherRealm;
	@Fake
	String instancename;
	@Fake
	String auditTrailSpoolName;
	@Fake
	String asyncAPISpoolName;
	@Fake
	boolean explainSearchResults;
	@Fake
	int resetBatchSizeIndexer;
	@Fake
	int dirListMaxSize;
	@Fake
	int searchResultMaxSize;
	@Fake
	long pendingActivityMaxAgeGraceRestartDuration;
	@Fake
	String storageName;

	Duration pendingActivityMaxAgeGraceRestart;

	MyDMAMConfigurationProperties c;

	@BeforeEach
	void init() {
		pendingActivityMaxAgeGraceRestart = Duration.ofMillis(abs(pendingActivityMaxAgeGraceRestartDuration));

		c = new MyDMAMConfigurationProperties(
				pathindexing,
				instancename,
				auditTrailSpoolName,
				asyncAPISpoolName,
				explainSearchResults,
				resetBatchSizeIndexer,
				dirListMaxSize,
				searchResultMaxSize,
				pendingActivityMaxAgeGraceRestart,
				magick,
				activityHandlers);
	}

	@Test
	void testNoInstancename() {
		c = new MyDMAMConfigurationProperties(
				pathindexing,
				null,
				auditTrailSpoolName,
				asyncAPISpoolName,
				explainSearchResults,
				resetBatchSizeIndexer,
				dirListMaxSize,
				searchResultMaxSize,
				pendingActivityMaxAgeGraceRestart,
				magick,
				activityHandlers);
		assertThat(c.instancename()).isNotEmpty();
	}

	@Test
	void testBadPendingActivityMaxAgeGraceRestart() {
		pendingActivityMaxAgeGraceRestart = Duration.ofMillis(-abs(pendingActivityMaxAgeGraceRestartDuration));
		assertThrows(IllegalStateException.class, () -> new MyDMAMConfigurationProperties(
				pathindexing,
				instancename,
				auditTrailSpoolName,
				asyncAPISpoolName,
				explainSearchResults,
				resetBatchSizeIndexer,
				dirListMaxSize,
				searchResultMaxSize,
				pendingActivityMaxAgeGraceRestart,
				magick,
				activityHandlers));

		pendingActivityMaxAgeGraceRestart = Duration.ZERO;
		assertThrows(IllegalStateException.class, () -> new MyDMAMConfigurationProperties(
				pathindexing,
				instancename,
				auditTrailSpoolName,
				asyncAPISpoolName,
				explainSearchResults,
				resetBatchSizeIndexer,
				dirListMaxSize,
				searchResultMaxSize,
				pendingActivityMaxAgeGraceRestart,
				magick,
				activityHandlers));
	}

	@Test
	void testGetRealmNames_empty() {
		assertThat(c.getRealmNames()).isEmpty();
		verify(pathindexing, times(1)).realms();
	}

	@Test
	void testGetRealmNames() {
		when(pathindexing.realms()).thenReturn(Map.of(new TechnicalName(realmName), realm));
		assertThat(c.getRealmNames()).containsOnly(realmName);
		verify(pathindexing, times(1)).realms();
	}

	@Test
	void testGetRealmByName_contains() {
		when(pathindexing.realms()).thenReturn(Map.of(new TechnicalName(realmName), realm));
		assertThat(c.getRealmByName(realmName)).contains(realm);
		verify(pathindexing, times(1)).realms();
	}

	@Test
	void testGetRealmByName_notContains() {
		when(pathindexing.realms()).thenReturn(Map.of(new TechnicalName(realmName), realm));
		assertThat(c.getRealmByName(otherRealm)).isEmpty();
		verify(pathindexing, times(1)).realms();
	}

	@Test
	void testGetRealmAndStorage() {
		when(pathindexing.realms()).thenReturn(Map.of(new TechnicalName(realmName), realm));
		assertThrows(IllegalArgumentException.class, () -> c.getRealmAndStorage(otherRealm, storageName));

		when(realm.getStorageByName(storageName)).thenReturn(Optional.empty());
		assertThrows(IllegalArgumentException.class, () -> c.getRealmAndStorage(realmName, storageName));

		when(realm.getStorageByName(storageName)).thenReturn(Optional.ofNullable(storage));
		final var confEnv = c.getRealmAndStorage(realmName, storageName);
		assertNotNull(confEnv);
		assertEquals(confEnv.realm(), realm);
		assertEquals(confEnv.realmName(), realmName);
		assertEquals(confEnv.storage(), storage);
		assertEquals(confEnv.storageName(), storageName);

		verify(pathindexing, atLeastOnce()).realms();
		verify(realm, atLeastOnce()).getStorageByName(storageName);
	}

	@Fake
	String handlerName;

	@Test
	void testIsActivatedActivityHandler_global() {
		when(activityHandlers.pass(handlerName)).thenReturn(false);
		assertFalse(c.isActivatedActivityHandler(realmName, handlerName));
		verify(activityHandlers, times(1)).pass(handlerName);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testIsActivatedActivityHandler_realm(final boolean pass) {
		when(activityHandlers.pass(handlerName)).thenReturn(true);
		when(realm.activityHandlers()).thenReturn(realmActivityHandlers);
		when(realmActivityHandlers.pass(handlerName)).thenReturn(pass);
		when(pathindexing.realms()).thenReturn(Map.of(new TechnicalName(realmName), realm));

		assertEquals(pass, c.isActivatedActivityHandler(realmName, handlerName));

		verify(activityHandlers, times(1)).pass(handlerName);
		verify(realm, atLeastOnce()).activityHandlers();
		verify(realmActivityHandlers, times(1)).pass(handlerName);
		verify(pathindexing, times(1)).realms();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testIsActivatedActivityHandler_noGlobal(final boolean pass) {
		c = new MyDMAMConfigurationProperties(
				pathindexing,
				instancename,
				auditTrailSpoolName,
				asyncAPISpoolName,
				explainSearchResults,
				resetBatchSizeIndexer,
				dirListMaxSize,
				searchResultMaxSize,
				pendingActivityMaxAgeGraceRestart,
				magick,
				null);

		when(realm.activityHandlers()).thenReturn(realmActivityHandlers);
		when(realmActivityHandlers.pass(handlerName)).thenReturn(pass);
		when(pathindexing.realms()).thenReturn(Map.of(new TechnicalName(realmName), realm));

		assertEquals(pass, c.isActivatedActivityHandler(realmName, handlerName));

		verify(realm, atLeastOnce()).activityHandlers();
		verify(realmActivityHandlers, times(1)).pass(handlerName);
		verify(pathindexing, times(1)).realms();
	}

	@Test
	void testIsActivatedActivityHandler_nothingSet() {
		c = new MyDMAMConfigurationProperties(
				pathindexing,
				instancename,
				auditTrailSpoolName,
				asyncAPISpoolName,
				explainSearchResults,
				resetBatchSizeIndexer,
				dirListMaxSize,
				searchResultMaxSize,
				pendingActivityMaxAgeGraceRestart,
				magick,
				null);

		when(pathindexing.realms()).thenReturn(Map.of(new TechnicalName(realmName), realm));

		assertTrue(c.isActivatedActivityHandler(realmName, handlerName));

		verify(realm, atLeastOnce()).activityHandlers();
		verify(pathindexing, times(1)).realms();
	}

}
