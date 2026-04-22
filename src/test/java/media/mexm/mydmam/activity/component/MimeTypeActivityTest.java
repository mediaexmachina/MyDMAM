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
package media.mexm.mydmam.activity.component;

import static media.mexm.mydmam.activity.ActivityLimitPolicy.TYPE_EXTRACTION;
import static media.mexm.mydmam.audittrail.AuditTrailObjectType.FILE_MIME_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.FlatMetadataThesaurusService;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefDublinCore;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class MimeTypeActivityTest {

    @MockitoBean
    MimeTypeDetector mimeTypeDetector;
    @MockitoBean
    AuditTrail auditTrail;

    @Mock
    ActivityEventType eventType;
    @Mock
    RealmStorageConfiguredEnv storedOn;
    @Mock
    PathIndexingStorage pathIndexingStorage;
    @Mock
    FileEntity fileEntity;
    @Fake
    String mimeType;
    @Fake
    String realm;
    @Fake
    String hashPath;

    @Autowired
    FlatMetadataThesaurusService metadataThesaurusService;

    @Autowired
    MimeTypeActivity mta;

    File internalFile;

    @Test
    void testGetLimitPolicy() {
        assertEquals(TYPE_EXTRACTION, mta.getLimitPolicy());
    }

    @Test
    void testCanHandle() {
        when(storedOn.isDAS()).thenReturn(true);
        assertTrue(mta.canHandle(fileEntity, eventType, storedOn));

        when(storedOn.isDAS()).thenReturn(false);
        assertFalse(mta.canHandle(fileEntity, eventType, storedOn));

        verify(storedOn, times(2)).isDAS();
    }

    @Test
    void testHandle() throws Exception {
        metadataThesaurusService.reset();
        internalFile = new File("<nothing>");
        when(storedOn.storage()).thenReturn(pathIndexingStorage);
        when(storedOn.getLocalInternalFile(fileEntity)).thenReturn(internalFile);
        when(mimeTypeDetector.getMimeType(internalFile)).thenReturn(mimeType);
        when(fileEntity.getHashPath()).thenReturn(hashPath);
        when(fileEntity.getRealm()).thenReturn(realm);

        mta.handle(fileEntity, eventType, storedOn);

        verify(mimeTypeDetector, times(1)).getMimeType(internalFile);
        verify(storedOn, atLeastOnce()).getLocalInternalFile(fileEntity);

        metadataThesaurusService.checkIfAdded(MtdThesaurusDefDublinCore.class, mimeType).format();
        metadataThesaurusService.endChecks(fileEntity);

        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileEntity, atLeastOnce()).getHashPath();
        verify(auditTrail, times(1)).asyncPersistForRealm(
                realm, "mime-type", "direct-extracted-from-file", FILE_MIME_TYPE, hashPath, mimeType);
    }

}
