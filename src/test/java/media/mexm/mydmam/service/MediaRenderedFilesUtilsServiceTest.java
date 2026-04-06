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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.asset.MetadataExtractorHandler;
import media.mexm.mydmam.asset.RenderedFileSpecs;
import media.mexm.mydmam.asset.RenderedFileSpecs.ThumbnailIMCmd;
import media.mexm.mydmam.audittrail.RealmAuditTrail;
import media.mexm.mydmam.component.ImageMagick;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class MediaRenderedFilesUtilsServiceTest {

    @MockitoBean
    MyDMAMConfigurationProperties configuration;
    @MockitoBean
    MimeTypeDetector mimeTypeDetector;
    @MockitoBean
    ImageMagick imageMagick;
    @MockitoBean
    MediaAssetService mediaAssetService;

    @Mock
    FileEntity fileEntity;
    @Mock
    FileMetadataEntity fileMetadataEntity;
    @Mock
    RealmAuditTrail realmAuditTrail;
    @Mock
    RenderedFileSpecs renderedFileSpecs;
    @Mock
    ThumbnailIMCmd thumbnailIMCmd;
    @Mock
    RealmStorageConfiguredEnv storedOn;
    @Mock
    MetadataExtractorHandler mtdHander;

    @Captor
    ArgumentCaptor<Collection<FileMetadataEntity>> fileMetadataEntitiesCaptor;

    @Fake
    String fileName;
    @Fake
    int index;
    @Fake
    String heroCmd;
    @Fake
    String cartridgeCmd;
    @Fake
    String iconCmd;
    @Fake
    String sourceFileName;
    @Fake
    boolean toGzip;
    @Fake
    String previewType;
    @Fake
    String originHandler;
    @Fake
    String classifier;
    @Fake
    int layer;

    File heroWorkingFile;
    File cartridgeWorkingFile;
    File iconWorkingFile;

    @Autowired
    MediaRenderedFilesUtilsService mrfu;

    @BeforeEach
    void init() {
        when(configuration.renderedSpecs()).thenReturn(renderedFileSpecs);
    }

    @AfterEach
    void ends() {
        verifyNoMoreInteractions(
                mediaAssetService,
                mimeTypeDetector,
                imageMagick);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testMakeImageThumbnails(final boolean isImageTypeAlpha) throws IOException {
        when(imageMagick.isEnabled()).thenReturn(true);
        heroWorkingFile = new File("hero-" + fileName);
        cartridgeWorkingFile = new File("cartridge-" + fileName);
        iconWorkingFile = new File("icon-" + fileName);
        when(renderedFileSpecs.thumbnail()).thenReturn(thumbnailIMCmd);
        when(thumbnailIMCmd.heroAlphaCmd()).thenReturn(heroCmd);
        when(thumbnailIMCmd.cartridgeAlphaCmd()).thenReturn(cartridgeCmd);
        when(thumbnailIMCmd.iconAlphaCmd()).thenReturn(iconCmd);
        when(thumbnailIMCmd.heroCmd()).thenReturn(heroCmd);
        when(thumbnailIMCmd.cartridgeCmd()).thenReturn(cartridgeCmd);
        when(thumbnailIMCmd.iconCmd()).thenReturn(iconCmd);
        when(storedOn.makeWorkingFile("hero-thumbnail.webp", fileEntity)).thenReturn(heroWorkingFile);
        when(storedOn.makeWorkingFile("cartridge-thumbnail.webp", fileEntity)).thenReturn(cartridgeWorkingFile);
        when(storedOn.makeWorkingFile("icon-thumbnail.webp", fileEntity)).thenReturn(iconWorkingFile);

        final var sourceFile = new File(sourceFileName);

        mrfu.makeImageThumbnails(fileEntity, storedOn, sourceFile, isImageTypeAlpha, index);

        verify(imageMagick, times(1)).convertImage(heroCmd, sourceFile, heroWorkingFile);
        verify(imageMagick, times(1)).convertImage(cartridgeCmd, sourceFile, cartridgeWorkingFile);
        verify(imageMagick, times(1)).convertImage(iconCmd, sourceFile, iconWorkingFile);

        verify(mediaAssetService, times(1)).declareRenderedStaticFile(
                fileEntity,
                heroWorkingFile,
                "hero-thumbnail.webp",
                false,
                index,
                "hero-thumbnail");
        verify(mediaAssetService, times(1)).declareRenderedStaticFile(
                fileEntity,
                cartridgeWorkingFile,
                "cartridge-thumbnail.webp",
                false,
                index,
                "cartridge-thumbnail");
        verify(mediaAssetService, times(1)).declareRenderedStaticFile(
                fileEntity,
                iconWorkingFile,
                "icon-thumbnail.webp",
                false,
                index,
                "icon-thumbnail");

        verify(storedOn, times(1)).makeWorkingFile("hero-thumbnail.webp", fileEntity);
        verify(storedOn, times(1)).makeWorkingFile("cartridge-thumbnail.webp", fileEntity);
        verify(storedOn, times(1)).makeWorkingFile("icon-thumbnail.webp", fileEntity);

        if (isImageTypeAlpha) {
            verify(thumbnailIMCmd, times(1)).heroAlphaCmd();
            verify(thumbnailIMCmd, times(1)).cartridgeAlphaCmd();
            verify(thumbnailIMCmd, times(1)).iconAlphaCmd();
        } else {
            verify(thumbnailIMCmd, times(1)).heroCmd();
            verify(thumbnailIMCmd, times(1)).cartridgeCmd();
            verify(thumbnailIMCmd, times(1)).iconCmd();
        }

        verify(imageMagick, times(1)).isEnabled();
        verify(configuration, times(1)).renderedSpecs();
        verify(renderedFileSpecs, times(1)).thumbnail();
    }

    @Test
    void testMakeImageThumbnails_noImageMagick() {
        when(imageMagick.isEnabled()).thenReturn(false);
        assertThrows(IllegalCallerException.class,
                () -> mrfu.makeImageThumbnails(fileEntity, storedOn, null, false, index));
        verify(imageMagick, times(1)).isEnabled();
    }

}
