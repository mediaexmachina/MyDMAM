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
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.tools.ImageMagick;

@Service
@Slf4j
public class RenderedFilesProducerServiceImpl implements RenderedFilesProducerService {

    @Autowired
    MimeTypeDetector mimeTypeDetector;
    @Autowired
    MyDMAMConfigurationProperties configuration;
    @Autowired
    ImageMagick imageMagick;

    @Override
    public File makeWorkingFile(final String fileName,
                                final MediaAsset asset,
                                final RealmStorageConfiguredEnv storedOn) {
        return storedOn.realm().makeWorkingFile(asset.getFile().getId() + "-" + fileName);
    }

    @Override
    public void assetDeclareRenderedStaticFile(final MediaAsset asset,
                                               final File workingFile,
                                               final String name,
                                               final boolean toGzip,
                                               final int index,
                                               final String previewType) throws IOException {
        asset.declareRenderedStaticFile(workingFile, name, toGzip, mimeTypeDetector, index, previewType);
    }

    @Override
    public void makeImageThumbnails(final MediaAsset asset,
                                    final RealmStorageConfiguredEnv storedOn,
                                    final File sourceFile,
                                    final boolean isImageTypeAlpha,
                                    final int index) throws IOException {
        if (imageMagick.isEnabled() == false) {
            throw new IllegalCallerException("ImageMagick is disabled, cancel image production");
        }
        final var thumbnailConf = configuration.renderedSpecs().thumbnail();

        final var heroThumbnailFile = makeWorkingFile("hero-thumbnail.webp", asset, storedOn);
        final var cartridgeThumbnailFile = makeWorkingFile("cartridge-thumbnail.webp", asset, storedOn);
        final var iconThumbnailFile = makeWorkingFile("icon-thumbnail.webp", asset, storedOn);

        if (isImageTypeAlpha) {
            imageMagick.convertImage(thumbnailConf.heroAlphaCmd(), sourceFile, heroThumbnailFile);
            imageMagick.convertImage(thumbnailConf.cartridgeAlphaCmd(), sourceFile, cartridgeThumbnailFile);
            imageMagick.convertImage(thumbnailConf.iconAlphaCmd(), sourceFile, iconThumbnailFile);
        } else {
            imageMagick.convertImage(thumbnailConf.heroCmd(), sourceFile, heroThumbnailFile);
            imageMagick.convertImage(thumbnailConf.cartridgeCmd(), sourceFile, cartridgeThumbnailFile);
            imageMagick.convertImage(thumbnailConf.iconCmd(), sourceFile, iconThumbnailFile);
        }

        asset.declareRenderedStaticFile(
                heroThumbnailFile,
                "hero-thumbnail.webp", false, mimeTypeDetector, index, "hero-thumbnail");
        asset.declareRenderedStaticFile(
                cartridgeThumbnailFile,
                "cartridge-thumbnail.webp", false, mimeTypeDetector, index, "cartridge-thumbnail");
        asset.declareRenderedStaticFile(
                iconThumbnailFile,
                "icon-thumbnail.webp", false, mimeTypeDetector, index, "icon-thumbnail");

    }

}
