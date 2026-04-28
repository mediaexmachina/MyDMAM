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
import static org.apache.commons.lang3.math.Fraction.getReducedFraction;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.ActivityLimitPolicy;
import media.mexm.mydmam.dto.StorageStateClass;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalImage;
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
        final var imageReader = metadataThesaurusService.getReader(MtdThesaurusDefTechnicalImage.class, fileEntity);
        return imageReader.imageAspectFormat().value().isEmpty()
               && imageReader.height().value().isPresent()
               && imageReader.width().value().isPresent();
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
        final var reader = metadataThesaurusService.getReader(MtdThesaurusDefTechnicalImage.class, fileEntity);
        final var imageWriter = metadataThesaurusService.getWriter(this, fileEntity,
                MtdThesaurusDefTechnicalImage.class);

        final var height = reader.height().value().map(Float::parseFloat).orElseThrow();
        final var width = reader.width().value().map(Float::parseFloat).orElseThrow();

        if (height < 1 || width < 1) {
            return;
        }

        if (reader.displayAspectRatio().value().isEmpty()) {
            final var fraction = getReducedFraction(Math.round(width), Math.round(height));
            imageWriter.set(fraction.getNumerator() + ":" + fraction.getDenominator()).displayAspectRatio();
        }

        if (reader.sampleAspectRatio().value().isEmpty()) {
            imageWriter.set("1:1").sampleAspectRatio();
        }

        imageWriter.set(aspectRatio(width, height)).aspectRatio();
        imageWriter.set(getPageOrientation(width, height)).imageAspectFormat();
    }

    static double aspectRatio(final float width, final float height) {
        return round(width / height * 1000.0) / 1000.0;
    }

    static PageOrientation getPageOrientation(final float width, final float height) {
        final var aspect = Float.compare(width, height);
        if (aspect == 0) {
            return SQUARE;
        } else if (aspect > 0) {
            return LANDSCAPE;
        } else {
            return PORTRAIT;
        }
    }

}
