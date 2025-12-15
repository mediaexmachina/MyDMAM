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
package media.mexm.mydmam.pathindexing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.service.PathIndexerService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;

@ExtendWith(MockToolsExtendsJunit.class)
class RealmStorageWatchedFilesDbTest {

	@Fake
	String realmName;
	@Fake
	String storageName;

	@Mock
	PathIndexingStorage storage;
	@Mock
	ObservedFolder scan;
	@Mock
	PathIndexerService pathIndexerService;
	@Mock
	AbstractFileSystemURL fileSystem;
	@Mock
	Set<CachedFileAttributes> foundedFiles;

	RealmStorageWatchedFilesDb db;

	@BeforeEach
	void init() {
		when(storage.makeObservedFolder(any(), any())).thenReturn(scan);
		when(storage.maxDeep()).thenReturn(0);

		db = new RealmStorageWatchedFilesDb(pathIndexerService, realmName, storageName, storage);

		verify(storage, atLeastOnce()).makeObservedFolder(realmName, storageName);
		verify(storage, atLeastOnce()).maxDeep();
		verify(scan, times(1)).createFileSystem();
	}

	@AfterEach
	void ends() {
		reset(scan);
	}

	@Test
	void testSetup() {// NOSONAR S2699
		db.setup(scan, null);
	}

	@Test
	void testUpdate() {
		db.update(scan, fileSystem);
		verify(pathIndexerService, times(1))
				.updateFoundedFiles(notNull(), eq(realmName), eq(storageName), eq(scan), eq(fileSystem));
	}

	@Test
	void testReset() {
		db.reset(scan, foundedFiles);
		verify(pathIndexerService, times(1))
				.resetFoundedFiles(realmName, storageName, scan, foundedFiles);

	}

}
