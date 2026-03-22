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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.io.File;
import java.io.IOException;

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

import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.asset.RenderedFileSpecs;
import media.mexm.mydmam.asset.RenderedFileSpecs.ThumbnailIMCmd;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.tools.ImageMagick;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class RenderedFilesProducerServiceTest {

    @MockitoBean
    MimeTypeDetector mimeTypeDetector;
    @MockitoBean
    MyDMAMConfigurationProperties configuration;
    @MockitoBean
    ImageMagick imageMagick;

    @Mock
    RenderedFileSpecs renderedFileSpecs;
    @Mock
    ThumbnailIMCmd thumbnailIMCmd;
    @Mock
    MediaAsset asset;
    @Mock
    RealmStorageConfiguredEnv storedOn;
    @Mock
    RealmConf realm;
    @Mock
    FileEntity fileEntity;

    @Fake
    String fileName;
    @Fake
    int id;
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

    String workingFileName;
    File workingFile;
    File heroWorkingFile;
    File cartridgeWorkingFile;
    File iconWorkingFile;

    @Autowired
    RenderedFilesProducerService rfp;

    @BeforeEach
    void init() {
        workingFileName = id + "-" + fileName;
        workingFile = new File(fileName);
        heroWorkingFile = new File("hero-" + fileName);
        cartridgeWorkingFile = new File("cartridge-" + fileName);
        iconWorkingFile = new File("icon-" + fileName);

        when(configuration.renderedSpecs()).thenReturn(renderedFileSpecs);
        when(renderedFileSpecs.thumbnail()).thenReturn(thumbnailIMCmd);
        when(storedOn.realm()).thenReturn(realm);
        when(asset.getFile()).thenReturn(fileEntity);
        when(fileEntity.getId()).thenReturn(id);
        when(imageMagick.isEnabled()).thenReturn(true);

        when(realm.makeWorkingFile(workingFileName))
                .thenReturn(workingFile);
        when(realm.makeWorkingFile(id + "-" + "hero-thumbnail.webp"))
                .thenReturn(heroWorkingFile);
        when(realm.makeWorkingFile(id + "-" + "cartridge-thumbnail.webp"))
                .thenReturn(cartridgeWorkingFile);
        when(realm.makeWorkingFile(id + "-" + "icon-thumbnail.webp"))
                .thenReturn(iconWorkingFile);

        when(thumbnailIMCmd.heroAlphaCmd()).thenReturn(heroCmd);
        when(thumbnailIMCmd.cartridgeAlphaCmd()).thenReturn(cartridgeCmd);
        when(thumbnailIMCmd.iconAlphaCmd()).thenReturn(iconCmd);
        when(thumbnailIMCmd.heroCmd()).thenReturn(heroCmd);
        when(thumbnailIMCmd.cartridgeCmd()).thenReturn(cartridgeCmd);
        when(thumbnailIMCmd.iconCmd()).thenReturn(iconCmd);

    }

    @Test
    void testMakeWorkingFile() {
        final var wf = rfp.makeWorkingFile(fileName, asset, storedOn);
        assertThat(wf).isNotNull().isEqualTo(workingFile);

        verify(storedOn, times(1)).realm();
        verify(realm, times(1)).makeWorkingFile(workingFileName);
        verify(asset, times(1)).getFile();
        verify(fileEntity, times(1)).getId();
    }

    @Fake
    boolean toGzip;
    @Fake
    String previewType;

    @Test
    void testAssetDeclareRenderedStaticFile() throws IOException {
        rfp.assetDeclareRenderedStaticFile(asset, workingFile, fileName, toGzip, index, previewType);
        verify(asset, times(1))
                .declareRenderedStaticFile(workingFile, fileName, toGzip, mimeTypeDetector, index, previewType);

    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testMakeImageThumbnails(final boolean isImageTypeAlpha) throws IOException {
        final var sourceFile = new File(sourceFileName);

        rfp.makeImageThumbnails(asset, storedOn, sourceFile, isImageTypeAlpha, index);

        verify(imageMagick, times(1)).convertImage(heroCmd, sourceFile, heroWorkingFile);
        verify(imageMagick, times(1)).convertImage(cartridgeCmd, sourceFile, cartridgeWorkingFile);
        verify(imageMagick, times(1)).convertImage(iconCmd, sourceFile, iconWorkingFile);

        verify(asset, times(1)).declareRenderedStaticFile(
                heroWorkingFile,
                "hero-thumbnail.webp",
                false,
                mimeTypeDetector,
                index,
                "hero-thumbnail");
        verify(asset, times(1)).declareRenderedStaticFile(
                cartridgeWorkingFile,
                "cartridge-thumbnail.webp",
                false,
                mimeTypeDetector,
                index,
                "cartridge-thumbnail");
        verify(asset, times(1)).declareRenderedStaticFile(
                iconWorkingFile,
                "icon-thumbnail.webp",
                false,
                mimeTypeDetector,
                index,
                "icon-thumbnail");

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

        verify(storedOn, atLeast(3)).realm();
        verify(realm, times(1)).makeWorkingFile(id + "-" + "hero-thumbnail.webp");
        verify(realm, times(1)).makeWorkingFile(id + "-" + "cartridge-thumbnail.webp");
        verify(realm, times(1)).makeWorkingFile(id + "-" + "icon-thumbnail.webp");
        verify(asset, atLeast(3)).getFile();
        verify(fileEntity, atLeast(3)).getId();
    }

    @Test
    void testMakeImageThumbnails_noImageMagick() {
        when(imageMagick.isEnabled()).thenReturn(false);
        assertThrows(IllegalCallerException.class,
                () -> rfp.makeImageThumbnails(asset, storedOn, null, false, index));
        verify(imageMagick, times(1)).isEnabled();
    }

}
