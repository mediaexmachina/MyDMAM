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
package media.mexm.mydmam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.audittrail.RealmAuditTrail;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.component.PathIndexer;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.PathIndexingConf;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.repository.FileRepository;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "FlatJobKit" })
class PathIndexerServiceTest {

	@Autowired
	FlatJobKitEngine jobKitEngine;
	@Autowired
	PathIndexerService pis;

	@MockitoBean
	FileRepository fileRepository;
	@MockitoBean
	AuditTrail auditTrail;
	@MockitoBean
	MyDMAMConfigurationProperties configuration;
	@MockitoBean
	PathIndexer pathIndexer;

	@Mock
	PathIndexingConf pathIndexingConf;
	@Mock
	Duration scanTime;
	@Mock
	WatchedFiles scanResult;

	@Fake
	String spool;
	@Fake
	String realmName;
	@Fake
	String storageName;

	PathIndexingRealm realm;
	PathIndexingStorage storage;
	ObservedFolder scan;

	@AfterEach
	void ends() {
		assertTrue(jobKitEngine.isEmptyActiveServicesList());
		assertEquals(0, jobKitEngine.getEndEventsList().size());

		verifyNoMoreInteractions(fileRepository, auditTrail, configuration, pathIndexer);
	}

	@Test
	void testMakeWatchfolders_empty() {
		final var result = pis.makeWatchfolders();
		assertTrue(result.isEmpty());
		verify(configuration, times(1)).pathindexing();
	}

	@Test
	void testMakeWatchfolders() {
		scan = new ObservedFolder();
		scan.setTargetFolder(new File(".").getAbsolutePath());
		scan.setLabel("test");

		storage = new PathIndexingStorage(scan, 0, Duration.ZERO, spool);
		realm = new PathIndexingRealm(Map.of(storageName, storage), Duration.ZERO, spool, null);

		when(configuration.pathindexing()).thenReturn(pathIndexingConf);
		when(pathIndexingConf.getSpoolEvents()).thenReturn(spool);
		when(pathIndexingConf.realms()).thenReturn(Map.of(realmName, realm));

		final var result = pis.makeWatchfolders();
		assertThat(result).size().isEqualTo(1);

		final var realmStorageFolderActivity = result.keySet().iterator().next();
		assertNotNull(realmStorageFolderActivity.pathIndexerService());
		assertEquals(realmName, realmStorageFolderActivity.realmName());
		assertEquals(realm, realmStorageFolderActivity.realm());
		assertEquals(storageName, realmStorageFolderActivity.storageName());
		assertEquals(storage, realmStorageFolderActivity.storage());

		verify(configuration, atLeastOnce()).pathindexing();
		verify(pathIndexingConf, atLeastOnce()).getSpoolEvents();
		verify(pathIndexingConf, atLeastOnce()).realms();
		verify(pathIndexingConf, atLeastOnce()).timeBetweenScans();
	}

	@Mock
	RealmAuditTrail realmAuditTrail;

	@Test
	void testOnAfterScan() {
		when(auditTrail.getAuditTrailByRealm(realmName)).thenReturn(Optional.ofNullable(realmAuditTrail));
		when(scanResult.founded()).thenReturn(Set.of());
		when(scanResult.losted()).thenReturn(Set.of());
		when(scanResult.updated()).thenReturn(Set.of());

		pis.onAfterScan(realmName, storageName, realm, storage, scan, scanTime, scanResult);

		verify(auditTrail, times(1)).getAuditTrailByRealm(realmName);
		verify(scanResult, atLeastOnce()).founded();
		verify(scanResult, atLeastOnce()).losted();
		verify(scanResult, atLeastOnce()).updated();
	}

	@Test
	void testFileActivitytoAuditTrail() {
		// TODO test
	}

	@Test
	void testUpdateFoundedFiles() {
		// TODO (2) test
	}

	@Test
	void testResetFoundedFiles() {
		// TODO (2) test
	}

	/*
	@Mock
	PathIndexer indexer;
	@Mock
	AuditTrail auditTrail;
	@Mock
	FileAttributesReference item;


	String event;
	@Fake

	@Captor
	ArgumentCaptor<Collection<AuditTrailBatchInsertObject>> auditTrailBatchInsertObjectsCaptor;


		@Test
	void testOnAfterScan() throws IOException {
		when(indexer.getAuditTrail()).thenReturn(auditTrail);
		when(auditTrail.getAuditTrailByRealm(realmName)).thenReturn(Optional.ofNullable(realmAuditTrail));
		when(scanResult.founded()).thenReturn(Set.of());
		when(scanResult.losted()).thenReturn(Set.of());
		when(scanResult.updated()).thenReturn(Set.of());
	
		fa.onAfterScan(observedFolder, scanTime, scanResult);
	
		verify(indexer, times(1)).getAuditTrail();
		verify(auditTrail, times(1)).getAuditTrailByRealm(realmName);
		verify(scanResult, times(1)).founded();
		verify(scanResult, times(1)).losted();
		verify(scanResult, times(1)).updated();
	}
	
	
	@Test
	void testToAuditTrail_empty() {
		fa.toAuditTrail(realmAuditTrail, event, Set.of());
		Mockito.verifyNoInteractions(realmAuditTrail);
	}
	
	@Test
	void testToAuditTrail() {
		when(item.getPath()).thenReturn(path);
	
		fa.toAuditTrail(realmAuditTrail, event, Set.of(item));
	
		verify(realmAuditTrail, times(1))
				.asyncPersist(eq("pathindex"), eq(event), auditTrailBatchInsertObjectsCaptor.capture());
		verify(item, atLeast(1)).getPath();
	
		final var auditTrailBatchInsertObjects = auditTrailBatchInsertObjectsCaptor.getValue();
		assertThat(auditTrailBatchInsertObjects).size().isEqualTo(1);
		final var auditTrailBatchInsertObject = auditTrailBatchInsertObjects.iterator().next();
	
		assertEquals(hashPath(realmName, storageName, path),
				auditTrailBatchInsertObject.objectReference());
		assertEquals(FILE, auditTrailBatchInsertObject.objectType());
		assertEquals(item, auditTrailBatchInsertObject.objectPayload());
	}
	 *
	 * */
}
