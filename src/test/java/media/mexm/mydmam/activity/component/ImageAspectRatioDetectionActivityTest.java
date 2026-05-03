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
import static java.util.function.Predicate.not;
import static java.util.stream.Stream.iterate;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.computeDisplayAspectRatio;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.PageOrientation.LANDSCAPE;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.PageOrientation.PORTRAIT;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.PageOrientation.SQUARE;
import static net.datafaker.Faker.instance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import media.mexm.mydmam.FlatMetadataThesaurusService;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class ImageAspectRatioDetectionActivityTest {

    @Mock
    FileEntity fileEntity;
    @Mock
    ActivityEventType eventType;
    @Mock
    RealmStorageConfiguredEnv storedOn;
    @Fake(min = 10, max = 100)
    int low;
    @Fake(min = 101, max = 1000)
    int high;

    @Autowired
    FlatMetadataThesaurusService metadataThesaurusService;
    @Autowired
    ImageAspectRatioDetectionActivity iarda;

    @BeforeEach
    void init() {
        metadataThesaurusService.reset();
    }

    @AfterEach
    void ends() {
        metadataThesaurusService.check();
    }

    @Test
    void testGetLimitPolicy() {
        assertEquals(FILE_INFORMATION, iarda.getLimitPolicy());
    }

    @Test
    void testGetSupportedStorageStateClasses() {
        assertThat(iarda.getSupportedStorageStateClasses()).isEmpty();
    }

    @Test
    void testGetMetadataOriginName() {
        assertThat(iarda.getMetadataOriginName()).isEqualTo("mydmam-internal");
    }

    @Test
    void testCanHandle_noSet() {
        assertFalse(iarda.canHandle(fileEntity, eventType, storedOn));
        metadataThesaurusService.check(iarda).check(fileEntity);
    }

    @Test
    void testCanHandle_onlyHeight() {
        metadataThesaurusService.getTestThesaurus().technicalImage().height().set(high);
        assertFalse(iarda.canHandle(fileEntity, eventType, storedOn));
        metadataThesaurusService.check(iarda).check(fileEntity);
    }

    @Test
    void testCanHandle() {
        metadataThesaurusService.getTestThesaurus().technicalImage().height().set(high);
        metadataThesaurusService.getTestThesaurus().technicalImage().width().set(high);
        assertTrue(iarda.canHandle(fileEntity, eventType, storedOn));
        metadataThesaurusService.check(iarda).check(fileEntity);
    }

    @Test
    void testHandle_SQUARE() {
        metadataThesaurusService.getTestThesaurus().technicalImage().height().set(high);
        metadataThesaurusService.getTestThesaurus().technicalImage().width().set(high);

        iarda.handle(fileEntity, eventType, storedOn);

        metadataThesaurusService.getAssertThesaurus().technicalImage().aspectRatio().set(1f);
        metadataThesaurusService.getAssertThesaurus().technicalImage().imageAspectFormat().set(SQUARE);
        metadataThesaurusService.getAssertThesaurus().technicalImage().displayAspectRatio().set("1:1");
        metadataThesaurusService.getAssertThesaurus().technicalImage().sampleAspectRatio().set("1:1");
        metadataThesaurusService.check(iarda).check(fileEntity);
    }

    @Test
    void testHandle_LANDSCAPE() {
        metadataThesaurusService.getTestThesaurus().technicalImage().height().set(low);
        metadataThesaurusService.getTestThesaurus().technicalImage().width().set(high);

        iarda.handle(fileEntity, eventType, storedOn);

        metadataThesaurusService.getAssertThesaurus().technicalImage().aspectRatio()
                .set(round(high * 1000.0 / low) / 1000.0);
        metadataThesaurusService.getAssertThesaurus().technicalImage().imageAspectFormat().set(LANDSCAPE);
        metadataThesaurusService.getAssertThesaurus().technicalImage().displayAspectRatio()
                .set(computeDisplayAspectRatio(high, low));
        metadataThesaurusService.getAssertThesaurus().technicalImage().sampleAspectRatio().set("1:1");

        metadataThesaurusService.check(iarda).check(fileEntity);
    }

    @Test
    void testHandle_PORTRAIT() {
        metadataThesaurusService.getTestThesaurus().technicalImage().height().set(high);
        metadataThesaurusService.getTestThesaurus().technicalImage().width().set(low);

        iarda.handle(fileEntity, eventType, storedOn);

        metadataThesaurusService.getAssertThesaurus().technicalImage().aspectRatio()
                .set(round(low * 1000.0 / high) / 1000.0);
        metadataThesaurusService.getAssertThesaurus().technicalImage().imageAspectFormat().set(PORTRAIT);
        metadataThesaurusService.getAssertThesaurus().technicalImage().displayAspectRatio()
                .set(computeDisplayAspectRatio(low, high));
        metadataThesaurusService.getAssertThesaurus().technicalImage().sampleAspectRatio().set("1:1");
        metadataThesaurusService.check(iarda).check(fileEntity);
    }

    @Test
    void testComputeDisplayAspectRatio() {
        assertThat(computeDisplayAspectRatio(10, 20)).isEqualTo("1:2");
        final var w = primeGenerate();
        final var h = primeGenerate();
        assertThat(computeDisplayAspectRatio(w, h)).isEqualTo(w + ":" + h);
    }

    /**
     * https://stackoverflow.com/questions/20435289/prime-number-generator-logic
     */
    public static int primeGenerate() {
        final var series = instance().random().nextInt(100, 1000);
        final Set<Integer> set = new TreeSet<>();
        return iterate(1, i -> ++i)
                .filter(i -> i % 2 != 0)
                .filter(i -> {
                    set.add(i);
                    return 0 == set.stream()
                            .filter(p -> p != 1)
                            .filter(not(p -> Objects.equals(p, i)))
                            .filter(p -> i % p == 0)
                            .count();
                })
                .skip(series)
                .findFirst()
                .orElseThrow();
    }
}
