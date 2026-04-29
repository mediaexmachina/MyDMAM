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
package media.mexm.mydmam.service;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import media.mexm.mydmam.repository.FileMetadataDao;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class MetadataThesaurusServiceTest {

    @Mock
    FileMetadataDao fileMetadataDao;
    @Mock
    AuditTrail auditTrail;
    @Mock
    FileEntity fileEntity;
    @Mock
    ActivityHandler handler;

    @Captor
    ArgumentCaptor<FileMetadataEntity> fileMetadataEntityCaptor;

    @Fake
    int layer;
    @Fake
    String value;
    @Fake
    String origin;
    @Fake
    String realm;

    MetadataThesaurusServiceImpl mts;

    String classifier = "dc";
    String key = "format";

    @BeforeEach
    void init() {
        when(handler.getMetadataOriginName()).thenReturn(origin);
        when(auditTrail.getAuditTrailByRealm(realm)).thenReturn(empty());
        when(fileEntity.getRealm()).thenReturn(realm);
        when(fileMetadataDao.getMetadataValue(fileEntity, layer, classifier, key))
                .thenReturn(Optional.ofNullable(value));
        when(fileMetadataDao.getMetadataValue(fileEntity, 0, classifier, key))
                .thenReturn(Optional.ofNullable(value));
        when(fileMetadataDao.getMetadataLayersValues(fileEntity, classifier, key))
                .thenReturn(Map.of(layer, value));

        mts = new MetadataThesaurusServiceImpl(fileMetadataDao, auditTrail);
    }

    @Test
    void testGetThesaurus() {
        final var thesaurus = mts.getThesaurus(handler, fileEntity);
        assertNotNull(thesaurus);

        assertThat(thesaurus.dublinCore().format().get(layer)).contains(value);
        verify(fileMetadataDao, times(1))
                .getMetadataValue(fileEntity, layer, classifier, key);

        assertThat(thesaurus.dublinCore().format().getAll()).hasSize(1).containsEntry(layer, value);
        verify(fileMetadataDao, times(1))
                .getMetadataLayersValues(fileEntity, classifier, key);

        thesaurus.dublinCore().format().set(layer, value);
        verify(fileMetadataDao, times(1))
                .addUpdateEntry(eq(fileEntity), fileMetadataEntityCaptor.capture());

        final var added = fileMetadataEntityCaptor.getValue();
        assertThat(added.getOrigin()).isEqualTo(origin);
        assertThat(added.getClassifier()).isEqualTo(classifier);
        assertThat(added.getKey()).isEqualTo(key);
        assertThat(added.getLayer()).isEqualTo(layer);
        assertThat(added.getValue()).isEqualTo(value);
        assertThat(added.getFile()).isEqualTo(fileEntity);

        verify(auditTrail, times(1)).getAuditTrailByRealm(realm);
        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileEntity, atLeast(0)).getHashPath();
        verify(handler, times(1)).getMetadataOriginName();
    }

    @Test
    void testSetMimeType() {
        mts.setMimeType(handler, fileEntity, value);

        verify(fileMetadataDao, times(1))
                .addUpdateEntry(eq(fileEntity), fileMetadataEntityCaptor.capture());

        final var added = fileMetadataEntityCaptor.getValue();
        assertThat(added.getOrigin()).isEqualTo(origin);
        assertThat(added.getClassifier()).isEqualTo(classifier);
        assertThat(added.getKey()).isEqualTo(key);
        assertThat(added.getLayer()).isZero();
        assertThat(added.getValue()).isEqualTo(value);
        assertThat(added.getFile()).isEqualTo(fileEntity);

        verify(auditTrail, times(1)).getAuditTrailByRealm(realm);
        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileEntity, atLeast(0)).getHashPath();
        verify(handler, times(1)).getMetadataOriginName();
    }

    @Test
    void testGetReadOnlyThesaurus() {
        final var thesaurus = mts.getReadOnlyThesaurus(fileEntity);
        assertNotNull(thesaurus);

        assertThat(thesaurus.dublinCore().format().get(layer)).contains(value);
        verify(fileMetadataDao, times(1))
                .getMetadataValue(fileEntity, layer, classifier, key);
    }

    @Test
    void testGetMimeType() {
        assertThat(mts.getMimeType(fileEntity)).contains(value);

        verify(fileMetadataDao, times(1))
                .getMetadataValue(fileEntity, 0, classifier, key);
    }

}
