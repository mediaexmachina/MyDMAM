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

import static media.mexm.mydmam.audittrail.AuditTrailObjectType.FILE;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import media.mexm.mydmam.audittrail.AuditTrailBatchInsertObject;
import media.mexm.mydmam.audittrail.RealmAuditTrail;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.component.PathIndexer;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.FileAttributesReference;

@ExtendWith(MockToolsExtendsJunit.class)
class RealmStorageFolderActivityTest {

	@Mock
	PathIndexer indexer;
	@Mock
	PathIndexingRealm realm;
	@Mock
	PathIndexingStorage storage;
	@Mock
	ObservedFolder observedFolder;
	@Mock
	WatchedFiles scanResult;
	@Mock
	Duration scanTime;
	@Mock
	AuditTrail auditTrail;
	@Mock
	RealmAuditTrail realmAuditTrail;
	@Mock
	FileAttributesReference item;

	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	String event;
	@Fake
	String path;

	@Captor
	ArgumentCaptor<Collection<AuditTrailBatchInsertObject>> auditTrailBatchInsertObjectsCaptor;

	RealmStorageFolderActivity fa;

	@BeforeEach
	void init() {
		fa = new RealmStorageFolderActivity(indexer, realmName, realm, storageName, storage);
	}

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

}
