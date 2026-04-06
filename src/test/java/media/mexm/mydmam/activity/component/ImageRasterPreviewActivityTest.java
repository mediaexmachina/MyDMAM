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

import static java.io.File.createTempFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.io.File;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.FlatMetadataThesaurusService;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.component.ImageMagick;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnical;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.service.MediaAssetService;
import media.mexm.mydmam.service.MediaRenderedFilesUtilsService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class ImageRasterPreviewActivityTest {

    @MockitoBean
    ImageMagick imageMagick;
    @MockitoBean
    MediaAssetService mediaAssetService;
    @MockitoBean
    MediaRenderedFilesUtilsService mediaRenderedFilesUtilsService;

    @Mock
    FileEntity file;
    @Mock
    ActivityEventType eventType;
    @Mock
    RealmStorageConfiguredEnv storedOn;
    @Mock
    PathIndexingStorage storage;

    @Fake
    String mimeType;
    @Fake
    String typeClassifer;

    File assetFile;

    @Autowired
    FlatMetadataThesaurusService metadataThesaurusService;
    @Autowired
    ImageRasterPreviewActivity irpa;

    @BeforeEach
    void init() {
        when(imageMagick.getManagedRasterMimeTypes()).thenReturn(Set.of(mimeType));
        metadataThesaurusService.reset();
    }

    @AfterEach
    void ends() {
        metadataThesaurusService.endChecks(file);
        verifyNoMoreInteractions(imageMagick, mediaAssetService, mediaRenderedFilesUtilsService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testIsEnabled(final boolean enabled) {
        when(imageMagick.isEnabled()).thenReturn(enabled);
        assertEquals(enabled, irpa.isEnabled());
        verify(imageMagick, times(1)).isEnabled();
    }

    @Test
    void testCanHandle() {
        when(storedOn.isDAS()).thenReturn(false);
        when(storedOn.haveWorkingDir()).thenReturn(false);
        when(storedOn.haveRenderedDir()).thenReturn(false);

        metadataThesaurusService.setMimeType("nope/nope");

        assertFalse(irpa.canHandle(file, eventType, storedOn));

        when(storedOn.isDAS()).thenReturn(true);
        assertFalse(irpa.canHandle(file, eventType, storedOn));

        when(storedOn.haveWorkingDir()).thenReturn(true);
        assertFalse(irpa.canHandle(file, eventType, storedOn));

        when(storedOn.haveRenderedDir()).thenReturn(true);
        assertFalse(irpa.canHandle(file, eventType, storedOn));

        metadataThesaurusService.addResponse(MtdThesaurusDefTechnical.class, 1).width();
        metadataThesaurusService.addResponse(MtdThesaurusDefTechnical.class, 1).height();

        assertFalse(irpa.canHandle(file, eventType, storedOn));

        metadataThesaurusService.setMimeType(mimeType);

        assertTrue(irpa.canHandle(file, eventType, storedOn));

        verify(storedOn, atLeastOnce()).isDAS();
        verify(storedOn, atLeastOnce()).haveWorkingDir();
        verify(storedOn, atLeastOnce()).haveRenderedDir();
        verify(imageMagick, atLeastOnce()).getManagedRasterMimeTypes();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testHandle(final boolean isImageTypeAlpha) throws Exception {
        assetFile = createTempFile("mydmam-" + getClass().getSimpleName(), "assetFile");
        if (isImageTypeAlpha) {
            typeClassifer = typeClassifer + "Alpha";
        }

        when(storedOn.getLocalInternalFile(file)).thenReturn(assetFile);

        metadataThesaurusService.addResponse(MtdThesaurusDefTechnical.class, typeClassifer).type();

        irpa.handle(file, eventType, storedOn);

        verify(mediaRenderedFilesUtilsService, times(1))
                .makeImageThumbnails(file, storedOn, assetFile, isImageTypeAlpha, 0);
        verify(storedOn, atLeastOnce()).getLocalInternalFile(file);
    }

}
