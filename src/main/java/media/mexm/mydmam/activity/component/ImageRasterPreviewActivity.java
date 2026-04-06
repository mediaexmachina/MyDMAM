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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.component.ImageMagick;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnical;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.repository.FileMetadataDao;
import media.mexm.mydmam.service.MediaAssetService;
import media.mexm.mydmam.service.MediaRenderedFilesUtilsService;
import media.mexm.mydmam.service.MetadataThesaurusService;

@Component
@Slf4j
public class ImageRasterPreviewActivity implements ActivityHandler {

    @Autowired
    ImageMagick imageMagick;
    @Autowired
    MediaAssetService mediaAssetService;
    @Autowired
    FileMetadataDao fileMetadataDao;
    @Autowired
    MediaRenderedFilesUtilsService mediaRenderedFilesUtilsService;
    @Autowired
    MetadataThesaurusService metadataThesaurusService;

    @Override
    public boolean isEnabled() {
        return imageMagick.isEnabled();
    }

    private boolean hasResolution(final FileEntity file) {
        final var def = metadataThesaurusService.getReader(MtdThesaurusDefTechnical.class, file, 0);
        return def.width().intValue(-1) > 0
               && def.height().intValue(-1) > 0;
    }

    @Override
    public boolean canHandle(final FileEntity file,
                             final ActivityEventType eventType,
                             final RealmStorageConfiguredEnv storedOn) {
        return storedOn.isDAS()
               && storedOn.haveWorkingDir()
               && storedOn.haveRenderedDir()
               && metadataThesaurusService.getMimeType(file)
                       .map(mimeType -> imageMagick.getManagedRasterMimeTypes().contains(mimeType))
                       .orElse(false)
               && hasResolution(file);
    }

    @Override
    public void handle(final FileEntity file,
                       final ActivityEventType eventType,
                       final RealmStorageConfiguredEnv storedOn) throws Exception {
        final var assetFile = storedOn.getLocalInternalFile(file);
        final var isImageTypeAlpha = metadataThesaurusService.getReader(MtdThesaurusDefTechnical.class, file, 0)
                .type()
                .value()
                .orElse("")
                .toLowerCase()
                .contains("alpha");

        mediaRenderedFilesUtilsService.makeImageThumbnails(file, storedOn, assetFile, isImageTypeAlpha, 0);
    }

}
