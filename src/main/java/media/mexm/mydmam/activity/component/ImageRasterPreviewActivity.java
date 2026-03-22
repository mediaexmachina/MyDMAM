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

import static media.mexm.mydmam.asset.FileMetadataResolutionTrait.MTD_TECHNICAL_CLASSIFIER;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.asset.ManagedMimeTrait;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.service.RenderedFilesProducerService;
import media.mexm.mydmam.tools.ImageMagick;

@Component
@Slf4j
public class ImageRasterPreviewActivity implements ActivityHandler, ManagedMimeTrait {

    @Autowired
    ImageMagick imageMagick;
    @Autowired
    RenderedFilesProducerService renderedFilesProducerService;

    @Override
    public Set<String> getManagedMimeTypes() {
        return imageMagick.getManagedRasterMimeTypes();
    }

    @Override
    public boolean isEnabled() {
        return imageMagick.isEnabled();
    }

    @Override
    public boolean canHandle(final MediaAsset asset,
                             final ActivityEventType eventType,
                             final RealmStorageConfiguredEnv storedOn) {
        return storedOn.isDAS()
               && storedOn.haveWorkingDir()
               && storedOn.haveRenderedDir()
               && canHandleMimeType(asset)
               && asset.hasResolution();
    }

    @Override
    public void handle(final MediaAsset asset,
                       final ActivityEventType eventType,
                       final RealmStorageConfiguredEnv storedOn) throws Exception {
        final var assetFile = asset.getLocalInternalFile(storedOn.storage());
        final var isImageTypeAlpha = asset.getMetadataValue(MTD_TECHNICAL_CLASSIFIER, "type")
                .orElse("")
                .toLowerCase()
                .contains("alpha");

        renderedFilesProducerService.makeImageThumbnails(asset, storedOn, assetFile, isImageTypeAlpha, 0);
    }

}
