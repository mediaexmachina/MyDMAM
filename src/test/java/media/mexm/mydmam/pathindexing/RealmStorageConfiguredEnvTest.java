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
package media.mexm.mydmam.pathindexing;

import static media.mexm.mydmam.dto.StorageCategory.DAS;
import static media.mexm.mydmam.dto.StorageCategory.EXTERNAL;
import static media.mexm.mydmam.dto.StorageCategory.NAS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.configuration.RealmConf;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class RealmStorageConfiguredEnvTest {

	@Mock
	RealmConf realm;
	@Mock
	PathIndexingStorage storage;
	@Fake
	String realmName;
	@Fake
	String storageName;

	RealmStorageConfiguredEnv env;

	@BeforeEach
	void init() {
		env = new RealmStorageConfiguredEnv(realmName, storageName, realm, storage);
	}

	@Test
	void testIsDAS() {
		when(storage.getCategory()).thenReturn(DAS, NAS, EXTERNAL);

		assertTrue(env.isDAS());
		assertFalse(env.isDAS());
		assertFalse(env.isDAS());

		verify(storage, times(3)).getCategory();
	}

	@Test
	void testHaveWorkingDir() {
		when(realm.workingDirectory()).thenReturn(new File("."), (File) null);

		assertTrue(env.haveWorkingDir());
		assertFalse(env.haveWorkingDir());

		verify(realm, times(2)).workingDirectory();
	}

	@Test
	void testHaveRenderedDir() {
		when(realm.renderedMetadataDirectory()).thenReturn(new File("."), (File) null);

		assertTrue(env.haveRenderedDir());
		assertFalse(env.haveRenderedDir());

		verify(realm, times(2)).renderedMetadataDirectory();
	}

}
