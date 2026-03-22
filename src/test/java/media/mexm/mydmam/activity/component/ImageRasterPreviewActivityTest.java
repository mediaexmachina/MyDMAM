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
import static media.mexm.mydmam.asset.FileMetadataResolutionTrait.MTD_TECHNICAL_CLASSIFIER;
import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.Optional;
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

import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.service.RenderedFilesProducerService;
import media.mexm.mydmam.tools.ImageMagick;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class ImageRasterPreviewActivityTest {

    @MockitoBean
    ImageMagick imageMagick;
    @MockitoBean
    RenderedFilesProducerService renderedFilesProducerService;

    @Mock
    MediaAsset asset;
    @Mock
    ActivityEventType eventType;
    @Mock
    RealmStorageConfiguredEnv storedOn;
    @Fake
    String mimeType;
    @Mock
    PathIndexingStorage storage;

    @Autowired
    ImageRasterPreviewActivity irpa;

    @BeforeEach
    void init() {
        when(imageMagick.getManagedRasterMimeTypes()).thenReturn(Set.of(mimeType));
    }

    @AfterEach
    void ends() {
        verifyNoMoreInteractions(imageMagick, renderedFilesProducerService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testIsEnabled(final boolean enabled) {
        when(imageMagick.isEnabled()).thenReturn(enabled);
        assertEquals(enabled, irpa.isEnabled());
        verify(imageMagick, times(1)).isEnabled();
    }

    @Test
    void testGetManagedMimeTypes() {
        assertThat(irpa.getManagedMimeTypes()).isEqualTo(Set.of(mimeType));
        verify(imageMagick, times(1)).getManagedRasterMimeTypes();
    }

    @Test
    void testCanHandle() {
        when(storedOn.isDAS()).thenReturn(false);
        when(storedOn.haveWorkingDir()).thenReturn(false);
        when(storedOn.haveRenderedDir()).thenReturn(false);
        when(asset.getMimeType()).thenReturn(Optional.ofNullable("nope/nope"));
        when(asset.hasResolution()).thenReturn(false);

        assertFalse(irpa.canHandle(asset, eventType, storedOn));

        when(storedOn.isDAS()).thenReturn(true);
        assertFalse(irpa.canHandle(asset, eventType, storedOn));

        when(storedOn.haveWorkingDir()).thenReturn(true);
        assertFalse(irpa.canHandle(asset, eventType, storedOn));

        when(storedOn.haveRenderedDir()).thenReturn(true);
        assertFalse(irpa.canHandle(asset, eventType, storedOn));

        when(asset.hasResolution()).thenReturn(true);
        assertFalse(irpa.canHandle(asset, eventType, storedOn));

        when(asset.getMimeType()).thenReturn(Optional.ofNullable(mimeType));
        assertTrue(irpa.canHandle(asset, eventType, storedOn));

        verify(storedOn, atLeastOnce()).isDAS();
        verify(storedOn, atLeastOnce()).haveWorkingDir();
        verify(storedOn, atLeastOnce()).haveRenderedDir();
        verify(asset, atLeastOnce()).getMimeType();
        verify(asset, atLeastOnce()).hasResolution();
        verify(imageMagick, atLeastOnce()).getManagedRasterMimeTypes();
    }

    File assetFile;
    @Fake
    String typeClassifer;

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testHandle(final boolean isImageTypeAlpha) throws Exception {
        assetFile = createTempFile("mydmam-" + getClass().getSimpleName(), "assetFile");
        if (isImageTypeAlpha) {
            typeClassifer = typeClassifer + "Alpha";
        }

        when(storedOn.storage()).thenReturn(storage);
        when(asset.getLocalInternalFile(storage)).thenReturn(assetFile);
        when(asset.getMetadataValue(MTD_TECHNICAL_CLASSIFIER, "type"))
                .thenReturn(Optional.ofNullable(typeClassifer));

        irpa.handle(asset, eventType, storedOn);

        verify(renderedFilesProducerService, times(1))
                .makeImageThumbnails(asset, storedOn, assetFile, isImageTypeAlpha, 0);
        verify(storedOn, times(1)).storage();
        verify(asset, times(1)).getLocalInternalFile(storage);
        verify(asset, times(1)).getMetadataValue(MTD_TECHNICAL_CLASSIFIER, "type");
    }

}
