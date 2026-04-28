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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

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
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalImage;
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
        metadataThesaurusService.endChecks(fileEntity);
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
    }

    @Test
    void testCanHandle_onlyHeight() {
        metadataThesaurusService.addResponse(MtdThesaurusDefTechnicalImage.class, high).height();
        assertFalse(iarda.canHandle(fileEntity, eventType, storedOn));
    }

    @Test
    void testCanHandle() {
        metadataThesaurusService.addResponse(MtdThesaurusDefTechnicalImage.class, high).height();
        metadataThesaurusService.addResponse(MtdThesaurusDefTechnicalImage.class, high).width();
        assertTrue(iarda.canHandle(fileEntity, eventType, storedOn));
    }

    @Test
    void testHandle_SQUARE() {
        metadataThesaurusService.addResponse(MtdThesaurusDefTechnicalImage.class, high).width();
        metadataThesaurusService.addResponse(MtdThesaurusDefTechnicalImage.class, high).height();

        iarda.handle(fileEntity, eventType, storedOn);

        metadataThesaurusService.checkIfAdded(MtdThesaurusDefTechnicalImage.class, 1f).aspectRatio();
        metadataThesaurusService.checkIfAdded(MtdThesaurusDefTechnicalImage.class, SQUARE).imageAspectFormat();

        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileEntity, atLeastOnce()).getHashPath();
    }

    @Test
    void testHandle_LANDSCAPE() {
        metadataThesaurusService.addResponse(MtdThesaurusDefTechnicalImage.class, high).width();
        metadataThesaurusService.addResponse(MtdThesaurusDefTechnicalImage.class, low).height();

        iarda.handle(fileEntity, eventType, storedOn);

        metadataThesaurusService.checkIfAdded(MtdThesaurusDefTechnicalImage.class,
                round(high * 1000.0 / low) / 1000.0).aspectRatio();
        metadataThesaurusService.checkIfAdded(MtdThesaurusDefTechnicalImage.class, LANDSCAPE).imageAspectFormat();

        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileEntity, atLeastOnce()).getHashPath();
    }

    @Test
    void testHandle_PORTRAIT() {
        metadataThesaurusService.addResponse(MtdThesaurusDefTechnicalImage.class, low).width();
        metadataThesaurusService.addResponse(MtdThesaurusDefTechnicalImage.class, high).height();

        iarda.handle(fileEntity, eventType, storedOn);

        metadataThesaurusService.checkIfAdded(MtdThesaurusDefTechnicalImage.class,
                round(low * 1000.0 / high) / 1000.0).aspectRatio();
        metadataThesaurusService.checkIfAdded(MtdThesaurusDefTechnicalImage.class, PORTRAIT).imageAspectFormat();

        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileEntity, atLeastOnce()).getHashPath();
    }

}
