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

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.component.ImageMagick;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;

@Service
@Slf4j
public class MediaRenderedFilesUtilsServiceImpl implements MediaRenderedFilesUtilsService {

    @Autowired
    MyDMAMConfigurationProperties configuration;
    @Autowired
    MediaAssetService mediaAssetService;
    @Autowired
    ImageMagick imageMagick;
    @Autowired
    MimeTypeDetector mimeTypeDetector;

    @Override
    public void makeImageThumbnails(final FileEntity file,
                                    final RealmStorageConfiguredEnv storedOn,
                                    final File sourceFile,
                                    final boolean isImageTypeAlpha,
                                    final int index) throws IOException {
        if (imageMagick.isEnabled() == false) {
            throw new IllegalCallerException("ImageMagick is disabled, cancel image production");
        }
        final var thumbnailConf = configuration.renderedSpecs().thumbnail();
        final var heroThumbnailFile = storedOn.makeWorkingFile("hero-thumbnail.webp", file);
        final var cartridgeThumbnailFile = storedOn.makeWorkingFile("cartridge-thumbnail.webp", file);
        final var iconThumbnailFile = storedOn.makeWorkingFile("icon-thumbnail.webp", file);

        if (isImageTypeAlpha) {
            imageMagick.convertImage(thumbnailConf.heroAlphaCmd(), sourceFile, heroThumbnailFile);
            imageMagick.convertImage(thumbnailConf.cartridgeAlphaCmd(), sourceFile, cartridgeThumbnailFile);
            imageMagick.convertImage(thumbnailConf.iconAlphaCmd(), sourceFile, iconThumbnailFile);
        } else {
            imageMagick.convertImage(thumbnailConf.heroCmd(), sourceFile, heroThumbnailFile);
            imageMagick.convertImage(thumbnailConf.cartridgeCmd(), sourceFile, cartridgeThumbnailFile);
            imageMagick.convertImage(thumbnailConf.iconCmd(), sourceFile, iconThumbnailFile);
        }

        mediaAssetService.declareRenderedStaticFile(
                file,
                heroThumbnailFile,
                "hero-thumbnail.webp",
                false,
                index,
                "hero-thumbnail");

        mediaAssetService.declareRenderedStaticFile(
                file,
                cartridgeThumbnailFile,
                "cartridge-thumbnail.webp",
                false,
                index,
                "cartridge-thumbnail");

        mediaAssetService.declareRenderedStaticFile(
                file,
                iconThumbnailFile,
                "icon-thumbnail.webp",
                false,
                index,
                "icon-thumbnail");
    }

}
