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

import static java.lang.Float.parseFloat;
import static java.lang.Math.round;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.PageOrientation.LANDSCAPE;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.PageOrientation.PORTRAIT;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.PageOrientation.SQUARE;
import static org.apache.commons.lang3.math.Fraction.getReducedFraction;

import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.ActivityLimitPolicy;
import media.mexm.mydmam.dto.StorageStateClass;
import media.mexm.mydmam.entity.FileEntity;
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
        final var imageReader = metadataThesaurusService.getThesaurus(this, fileEntity).technicalImage();
        return imageReader.imageAspectFormat().get().isEmpty()
               && imageReader.height().get().isPresent()
               && imageReader.width().get().isPresent();
    }

    public enum PageOrientation {
        SQUARE,
        PORTRAIT,
        LANDSCAPE;
    }

    public static String computeDisplayAspectRatio(final int width, final int height) {
        final var fraction = getReducedFraction(width, height);
        return fraction.getNumerator() + ":" + fraction.getDenominator();
    }

    private static Stream<Integer> union(final Set<Integer> l, final Set<Integer> r) {
        return l.stream().filter(r::contains);
    }

    @Override
    public void handle(final FileEntity fileEntity,
                       final ActivityEventType eventType,
                       final RealmStorageConfiguredEnv storedOn) {
        final var thesaurus = metadataThesaurusService.getThesaurus(this, fileEntity);
        final var technicalImage = thesaurus.technicalImage();

        final var heightbyLayer = technicalImage.height().getAll();
        final var widthbyLayer = technicalImage.width().getAll();

        union(heightbyLayer.keySet(), widthbyLayer.keySet())
                .forEach(layer -> {
                    final var height = parseFloat(heightbyLayer.get(layer));
                    final var width = parseFloat(widthbyLayer.get(layer));

                    if (height < 1 || width < 1) {
                        return;
                    }

                    if (technicalImage.displayAspectRatio().get(layer).isEmpty()) {
                        technicalImage.displayAspectRatio().set(layer, computeDisplayAspectRatio(
                                Math.round(width),
                                Math.round(height)));
                    }

                    if (technicalImage.sampleAspectRatio().get(layer).isEmpty()) {
                        technicalImage.sampleAspectRatio().set(layer, "1:1");
                    }

                    technicalImage.aspectRatio().set(layer, aspectRatio(width, height));
                    technicalImage.imageAspectFormat().set(layer, getPageOrientation(width, height));
                });
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
