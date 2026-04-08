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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

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
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusClassifier;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntry;
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
    ActivityHandler activityHandler;

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

    @MetadataThesaurusClassifier("classifier")
    public interface TechDef {
        MetadataThesaurusEntry key();

        MetadataThesaurusEntry emptyResult();
    }

    MetadataThesaurusServiceImpl mts;

    @BeforeEach
    void init() {
        when(fileMetadataDao.getMetadataValue(
                eq(fileEntity),
                anyInt(),
                eq("classifier"),
                eq("key"))).thenReturn(Optional.ofNullable(value));
        when(fileMetadataDao.getMetadataValue(
                eq(fileEntity),
                anyInt(),
                eq("classifier"),
                eq("empty-result"))).thenReturn(empty());
        when(activityHandler.getMetadataOriginName()).thenReturn(origin);
        when(auditTrail.getAuditTrailByRealm(realm)).thenReturn(empty());
        when(fileEntity.getRealm()).thenReturn(realm);

        mts = new MetadataThesaurusServiceImpl(fileMetadataDao, auditTrail);
    }

    @Test
    void testGetReader() {
        final var reader = mts.getReader(TechDef.class, fileEntity);
        assertNotNull(reader);
        assertThat(reader.key().value()).contains(value);
        assertThat(reader.emptyResult().value()).isEmpty();

        verify(fileMetadataDao, times(1))
                .getMetadataValue(fileEntity, 0, "classifier", "key");
        verify(fileMetadataDao, times(1))
                .getMetadataValue(fileEntity, 0, "classifier", "empty-result");
    }

    @Test
    void testGetReader_withLayer() {
        final var reader = mts.getReader(TechDef.class, fileEntity, layer);
        assertNotNull(reader);
        assertThat(reader.key().value()).contains(value);
        assertThat(reader.emptyResult().value()).isEmpty();

        verify(fileMetadataDao, times(1))
                .getMetadataValue(fileEntity, layer, "classifier", "key");
        verify(fileMetadataDao, times(1))
                .getMetadataValue(fileEntity, layer, "classifier", "empty-result");
    }

    @Test
    void testGetValue() {
        final var result = mts.getValue(fileEntity, new MetadataThesaurusEntry("classifier", "key", empty()));
        assertThat(result).contains(value);
        verify(fileMetadataDao, times(1))
                .getMetadataValue(fileEntity, 0, "classifier", "key");
    }

    @Test
    void testGetValue_layer() {
        final var result = mts.getValue(fileEntity, layer, new MetadataThesaurusEntry("classifier", "key", empty()));
        assertThat(result).contains(value);
        verify(fileMetadataDao, times(1))
                .getMetadataValue(fileEntity, layer, "classifier", "key");
    }

    @Test
    void testGetWriter() {
        final var writer = mts.getWriter(activityHandler, fileEntity, TechDef.class);
        assertNotNull(writer);
        assertNull(writer.set(layer, value).key());

        verify(activityHandler, times(1)).getMetadataOriginName();
        verify(fileMetadataDao, times(1)).addUpdateEntry(eq(fileEntity), fileMetadataEntityCaptor.capture());

        final var entity = fileMetadataEntityCaptor.getValue();
        assertThat(entity.getFile()).isEqualTo(fileEntity);
        assertThat(entity.getOrigin()).isEqualTo(origin);
        assertThat(entity.getClassifier()).isEqualTo("classifier");
        assertThat(entity.getLayer()).isEqualTo(layer);
        assertThat(entity.getKey()).isEqualTo("key");
        assertThat(entity.getValue()).isEqualTo(value);

        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileEntity, atLeast(0)).getHashPath();
        verify(auditTrail, times(1)).getAuditTrailByRealm(realm);
    }

    @Test
    void testGetWriter_empty() {
        final var writer = mts.getWriter(activityHandler, fileEntity, TechDef.class);
        assertNotNull(writer);
        assertNull(writer.set(layer, empty()).key());

        verify(activityHandler, times(1)).getMetadataOriginName();
    }

    @Test
    void testGetWriter_noLayer() {
        final var writer = mts.getWriter(activityHandler, fileEntity, TechDef.class);
        assertNotNull(writer);
        assertNull(writer.set(value).key());

        verify(activityHandler, times(1)).getMetadataOriginName();
        verify(fileMetadataDao, times(1)).addUpdateEntry(eq(fileEntity), fileMetadataEntityCaptor.capture());

        final var entity = fileMetadataEntityCaptor.getValue();
        assertThat(entity.getFile()).isEqualTo(fileEntity);
        assertThat(entity.getOrigin()).isEqualTo(origin);
        assertThat(entity.getClassifier()).isEqualTo("classifier");
        assertThat(entity.getLayer()).isZero();
        assertThat(entity.getKey()).isEqualTo("key");
        assertThat(entity.getValue()).isEqualTo(value);

        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileEntity, atLeast(0)).getHashPath();
        verify(auditTrail, times(1)).getAuditTrailByRealm(realm);
    }

    @Test
    void testGetMimeType() {
        when(fileMetadataDao.getMetadataValue(
                fileEntity,
                0,
                "file-format",
                "mime-type")).thenReturn(Optional.ofNullable(value));

        assertThat(mts.getMimeType(fileEntity)).contains(value);
        verify(fileMetadataDao, times(1))
                .getMetadataValue(fileEntity, 0, "file-format", "mime-type");
    }

}
