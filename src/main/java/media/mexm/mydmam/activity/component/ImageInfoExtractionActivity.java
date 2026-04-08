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
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefFileFormat;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnical;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.service.MediaAssetService;
import media.mexm.mydmam.service.MetadataThesaurusService;

@Component
@Slf4j
public class ImageInfoExtractionActivity implements ActivityHandler {

    @Autowired
    ImageMagick imageMagick;
    @Autowired
    MediaAssetService mediaAssetService;
    @Autowired
    MetadataThesaurusService metadataThesaurusService;

    @Override
    public String getMetadataOriginName() {
        return "imagemagick";
    }

    @Override
    public boolean isEnabled() {
        return imageMagick.isEnabled();
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
                       .orElse(false);
    }

    @Override
    public void handle(final FileEntity file,
                       final ActivityEventType eventType,
                       final RealmStorageConfiguredEnv storedOn) throws Exception {
        final var assetFile = storedOn.getLocalInternalFile(file);
        final var workingFile = storedOn.makeWorkingFile("identify.json", file);

        final var jsonNode = imageMagick.extractIdentifyJsonFile(
                assetFile,
                workingFile);

        final var version = jsonNode.read("$.version", String.class).orElse("<unset>");
        if (version.equals("1.0") == false) {
            throw new IllegalArgumentException("Can't support JSON version " + version);
        }

        mediaAssetService.declareRenderedStaticFile(
                file, workingFile, "identify.json", true, 0, "image-format");

        metadataThesaurusService.getWriter(this, file, MtdThesaurusDefFileFormat.class)
                .set(jsonNode.read("$.image.mimeType", String.class))
                .mimeType();

        final var techWriter = metadataThesaurusService.getWriter(this, file, MtdThesaurusDefTechnical.class);
        techWriter.set(jsonNode.read("$.image.geometry.width", Integer.class)).width();
        techWriter.set(jsonNode.read("$.image.geometry.height", Integer.class)).height();

        final var orientation = jsonNode.read("$.image.orientation", String.class).orElse("undefined");
        if (orientation.equalsIgnoreCase("undefined") == false) {
            techWriter.set(orientation).orientation();
        }
        techWriter.set(jsonNode.read("$.image.colorspace", String.class)).colorspace();
        techWriter.set(jsonNode.read("$.image.type", String.class).map(String::toLowerCase)).type();
    }

}
