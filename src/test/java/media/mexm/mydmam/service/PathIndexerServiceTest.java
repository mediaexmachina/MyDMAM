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

import static media.mexm.mydmam.activity.ActivityEventType.NEW_FOUNDED_FILE;
import static media.mexm.mydmam.activity.ActivityEventType.UPDATED_FILE;
import static media.mexm.mydmam.audittrail.AuditTrailObjectType.FILE;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.audittrail.AuditTrailBatchInsertObject;
import media.mexm.mydmam.audittrail.RealmAuditTrail;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.component.PathIndexer;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.FileAttributesReference;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "FlatJobKit" })
class PathIndexerServiceTest {

	@Autowired
	FlatJobKitEngine jobKitEngine;
	@Autowired
	PathIndexerService pis;

	@MockitoBean
	AuditTrail auditTrail;
	@MockitoBean
	PathIndexer pathIndexer;
	@MockitoBean
	PendingActivityService pendingActivityService;

	@Mock
	Duration scanTime;
	@Mock
	WatchedFiles scanResult;
	@Mock
	RealmAuditTrail realmAuditTrail;
	@Mock
	FileAttributesReference item;
	@Mock
	CachedFileAttributes itemAdd;
	@Mock
	CachedFileAttributes itemUpdate;

	@Captor
	ArgumentCaptor<Collection<AuditTrailBatchInsertObject>> auditTrailBatchInsertObjectsCaptor;

	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	String event;
	@Fake
	String path;

	PathIndexingRealm realm;
	PathIndexingStorage storage;
	ObservedFolder scan;

	@AfterEach
	void ends() {
		assertTrue(jobKitEngine.isEmptyActiveServicesList());
		assertEquals(0, jobKitEngine.getEndEventsList().size());

		verifyNoMoreInteractions(auditTrail, pathIndexer, pendingActivityService);
	}

	@Test
	void testOnAfterScan() {
		when(auditTrail.getAuditTrailByRealm(realmName)).thenReturn(Optional.ofNullable(realmAuditTrail));
		when(itemAdd.getPath()).thenReturn("");
		when(itemUpdate.getPath()).thenReturn("");
		when(scanResult.founded()).thenReturn(Set.of(itemAdd));
		when(scanResult.updated()).thenReturn(Set.of(itemUpdate));

		/**
		 * CANT TEST THIS!
		 */
		when(scanResult.losted()).thenReturn(Set.of());

		pis.onAfterScan(realmName, storageName, realm, storage, scan, scanTime, scanResult);

		verify(auditTrail, times(1)).getAuditTrailByRealm(realmName);
		verify(realmAuditTrail, times(1)).asyncPersist(eq("pathindex"), eq("founded"), anyList());
		verify(realmAuditTrail, times(1)).asyncPersist(eq("pathindex"), eq("updated"), anyList());
		verify(scanResult, atLeastOnce()).founded();
		verify(scanResult, atLeastOnce()).losted();
		verify(scanResult, atLeastOnce()).updated();
		verify(itemAdd, atLeastOnce()).getPath();
		verify(itemUpdate, atLeastOnce()).getPath();

		verify(pendingActivityService, times(1))
				.startsActivities(realmName, storageName, realm, Set.of(itemAdd), NEW_FOUNDED_FILE);
		verify(pendingActivityService, times(1))
				.cleanupFiles(realmName, storageName, realm, Set.of());
		verify(pendingActivityService, times(1))
				.startsActivities(realmName, storageName, realm, Set.of(itemUpdate), UPDATED_FILE);
	}

	@Test
	void testFileActivitytoAuditTrail_empty() {// NOSONAR S2699
		pis.fileActivitytoAuditTrail(realmName, storageName, realmAuditTrail, event, Set.of());
	}

	@Test
	void testFileActivitytoAuditTrail() {
		when(item.getPath()).thenReturn(path);

		pis.fileActivitytoAuditTrail(realmName, storageName, realmAuditTrail, event, Set.of(item));

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

}
