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

import static java.lang.Math.round;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.PageOrientation.LANDSCAPE;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.PageOrientation.PORTRAIT;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.PageOrientation.SQUARE;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.ActivityLimitPolicy;
import media.mexm.mydmam.dto.StorageStateClass;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnical;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.service.MetadataThesaurusService;

@Component
@Slf4j
public class ImageAspectRatioDetectionActivity implements ActivityHandler {

    @Autowired
    MetadataThesaurusService metadataThesaurusService;

    @Override
    public Set<StorageStateClass> getSupportedStorageStateClasses() {
        return Set.of();
    }

    @Override
    public ActivityLimitPolicy getLimitPolicy() {
        return FILE_INFORMATION;
    }

    @Override
    public String getMetadataOriginName() {
        return "mydmam-internal";
    }

    @Override
    public boolean canHandle(final FileEntity fileEntity,
                             final ActivityEventType eventType,
                             final RealmStorageConfiguredEnv storedOn) {
        final var reader = metadataThesaurusService.getReader(MtdThesaurusDefTechnical.class, fileEntity);
        return reader.height().value().isPresent()
               && reader.width().value().isPresent();
    }

    public enum PageOrientation {
        SQUARE,
        PORTRAIT,
        LANDSCAPE;
    }

    @Override
    public void handle(final FileEntity fileEntity,
                       final ActivityEventType eventType,
                       final RealmStorageConfiguredEnv storedOn) {
        final var reader = metadataThesaurusService.getReader(MtdThesaurusDefTechnical.class, fileEntity);
        final var writer = metadataThesaurusService.getWriter(this, fileEntity, MtdThesaurusDefTechnical.class);

        final var height = reader.height().value().map(Float::parseFloat).orElseThrow();
        final var width = reader.width().value().map(Float::parseFloat).orElseThrow();

        // TODO manage ffprobe results sampleAspectRatio() + displayAspectRatio() on layers

        final var dar = width / height;
        writer.set(round(dar * 1000.0) / 1000.0).aspectRatio();

        final var aspect = Float.compare(width, height);
        if (aspect == 0) {
            writer.set(SQUARE).imageAspectFormat();
        } else if (aspect > 0) {
            writer.set(LANDSCAPE).imageAspectFormat();
        } else {
            writer.set(PORTRAIT).imageAspectFormat();
        }
    }

}
