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

import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.service.PathIndexerService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;

@ExtendWith(MockToolsExtendsJunit.class)
class RealmStorageFolderActivityTest {

	@Mock
	PathIndexerService pathIndexerService;
	@Mock
	RealmConf realm;
	@Mock
	PathIndexingStorage storage;
	@Mock
	ObservedFolder observedFolder;
	@Mock
	WatchedFiles scanResult;
	@Mock
	Duration scanTime;

	@Fake
	String realmName;
	@Fake
	String storageName;

	String path;

	RealmStorageFolderActivity fa;

	@BeforeEach
	void init() {
		fa = new RealmStorageFolderActivity(pathIndexerService, realmName, realm, storageName, storage);
	}

	@Test
	void testOnAfterScan() throws IOException {
		fa.onAfterScan(observedFolder, scanTime, scanResult);
		verify(pathIndexerService, times(1))
				.onAfterScan(realmName, storageName, realm, storage, observedFolder, scanTime, scanResult);
	}

}
