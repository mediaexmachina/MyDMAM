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

import static java.io.File.createTempFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.FlatMetadataThesaurusService;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.component.ImageMagick;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefDublinCore;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnical;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.service.MediaAssetService;
import media.mexm.mydmam.tools.JsonPathHelper;
import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class ImageInfoExtractionActivityTest {

    @MockitoBean
    ImageMagick imageMagick;
    @MockitoBean
    MediaAssetService mediaAssetService;
    @Mock
    FileEntity file;
    @Mock
    ActivityEventType eventType;
    @Mock
    RealmStorageConfiguredEnv storedOn;
    @Fake
    String mimeType;

    @Autowired
    FlatMetadataThesaurusService metadataThesaurusService;
    @Autowired
    ImageInfoExtractionActivity iiea;

    @BeforeEach
    void init() {
        when(imageMagick.getManagedRasterMimeTypes()).thenReturn(Set.of(mimeType));
    }

    @AfterEach
    void ends() {
        verifyNoMoreInteractions(imageMagick, mediaAssetService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testIsEnabled(final boolean enabled) {
        when(imageMagick.isEnabled()).thenReturn(enabled);
        assertEquals(enabled, iiea.isEnabled());
        verify(imageMagick, times(1)).isEnabled();
    }

    @Test
    void testGetMetadataOriginName() {
        assertThat(iiea.getMetadataOriginName()).isEqualTo("imagemagick");
    }

    @Test
    void testCanHandle() {
        when(storedOn.isDAS()).thenReturn(false);
        when(storedOn.haveWorkingDir()).thenReturn(false);
        when(storedOn.haveRenderedDir()).thenReturn(false);

        metadataThesaurusService.setMimeType("nope/nope");

        assertFalse(iiea.canHandle(file, eventType, storedOn));

        when(storedOn.isDAS()).thenReturn(true);
        assertFalse(iiea.canHandle(file, eventType, storedOn));

        when(storedOn.haveWorkingDir()).thenReturn(true);
        assertFalse(iiea.canHandle(file, eventType, storedOn));

        when(storedOn.haveRenderedDir()).thenReturn(true);
        assertFalse(iiea.canHandle(file, eventType, storedOn));

        metadataThesaurusService.setMimeType(mimeType);

        assertTrue(iiea.canHandle(file, eventType, storedOn));

        verify(storedOn, atLeastOnce()).isDAS();
        verify(storedOn, atLeastOnce()).haveWorkingDir();
        verify(storedOn, atLeastOnce()).haveRenderedDir();

        metadataThesaurusService.endChecks(file);

        verify(imageMagick, atLeastOnce()).getManagedRasterMimeTypes();
    }

    @Nested
    class Handle {

        @Mock
        PathIndexingStorage storage;
        @Mock
        RealmConf realm;
        @Mock
        FileEntity file;
        @Mock
        JsonPathHelper jsonNode;

        @Fake(min = 1, max = 10000)
        int fileId;
        @Fake
        String mimeType;

        @Fake
        String imageMimeType;
        @Fake(min = 1, max = 100)
        int width;
        @Fake(min = 1, max = 100)
        int height;
        @Fake
        String colorspace;
        @Fake
        String orientation;
        @Fake
        String imageType;

        File assetFile;
        File workingFile;

        @BeforeEach
        void init() throws IOException {
            assetFile = createTempFile("mydmam-" + getClass().getSimpleName(), "assetFile");
            workingFile = createTempFile("mydmam-" + getClass().getSimpleName(), "workingFile");
            writeStringToFile(workingFile, Faker.instance().lorem().paragraph(5), UTF_8);

            when(storedOn.storage()).thenReturn(storage);
            when(storedOn.realm()).thenReturn(realm);
            when(storedOn.makeWorkingFile(any(), eq(file))).thenReturn(workingFile);

            when(storedOn.getLocalInternalFile(file)).thenReturn(assetFile);
            when(realm.makeWorkingFile(any())).thenReturn(workingFile);
            when(file.getId()).thenReturn(fileId);
            when(imageMagick.extractIdentifyJsonFile(assetFile, workingFile))
                    .thenReturn(jsonNode);

            when(jsonNode.read("$.version", String.class))
                    .thenReturn(Optional.ofNullable("1.0"));
            when(jsonNode.read("$.image.mimeType", String.class))
                    .thenReturn(Optional.ofNullable(imageMimeType));
            when(jsonNode.read("$.image.geometry.width", Integer.class))
                    .thenReturn(Optional.ofNullable(width));
            when(jsonNode.read("$.image.geometry.height", Integer.class))
                    .thenReturn(Optional.ofNullable(height));
            when(jsonNode.read("$.image.colorspace", String.class))
                    .thenReturn(Optional.ofNullable(colorspace));
            when(jsonNode.read("$.image.orientation", String.class))
                    .thenReturn(Optional.ofNullable(orientation));
            when(jsonNode.read("$.image.type", String.class))
                    .thenReturn(Optional.ofNullable(imageType));

            metadataThesaurusService.reset();
        }

        @AfterEach
        void ends() {
            metadataThesaurusService.endChecks(file);
            deleteQuietly(assetFile);
            deleteQuietly(workingFile);
        }

        @Test
        void testHandle() throws Exception {
            iiea.handle(file, eventType, storedOn);

            verify(storedOn, times(1)).makeWorkingFile("identify.json", file);
            verify(storedOn, atLeastOnce()).getLocalInternalFile(file);
            verify(imageMagick, times(1)).extractIdentifyJsonFile(assetFile, workingFile);
            verify(mediaAssetService, times(1))
                    .declareRenderedStaticFile(file, workingFile, "identify.json", true, 0, "image-format");
            verify(jsonNode, atLeastOnce()).read(anyString(), eq(String.class));
            verify(jsonNode, atLeastOnce()).read(anyString(), eq(Integer.class));

            metadataThesaurusService.checkIfAdded(MtdThesaurusDefDublinCore.class, imageMimeType).format();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefTechnical.class, width).width();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefTechnical.class, height).height();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefTechnical.class, orientation).orientation();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefTechnical.class, colorspace).colorspace();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefTechnical.class, imageType.toLowerCase()).type();

            verify(file, atLeastOnce()).getRealm();
            verify(file, atLeastOnce()).getHashPath();
        }

        @Test
        void testHandle_badVersion() throws Exception {
            when(jsonNode.read("$.version", String.class)).thenReturn(empty());

            assertThrows(IllegalArgumentException.class,
                    () -> iiea.handle(file, eventType, storedOn));

            verify(storedOn, times(1)).makeWorkingFile("identify.json", file);
            verify(storedOn, atLeastOnce()).getLocalInternalFile(file);
            verify(imageMagick, times(1)).extractIdentifyJsonFile(assetFile, workingFile);
            assertThat(workingFile).exists();
            verify(jsonNode, atLeastOnce()).read(anyString(), eq(String.class));
        }

    }

}
