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
package media.mexm.mydmam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.entity.FileEntity;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class FlatMetadataThesaurusServiceTest {

    @Mock
    FileEntity fileEntity;
    @Mock
    ActivityHandler activityHandler;

    @Fake
    String handlerName;
    @Fake
    String value;
    @Fake
    String badValue;
    @Fake(min = 1, max = 10000)
    int layer;
    @Fake
    String mimeType;

    @Autowired
    FlatMetadataThesaurusService metadataThesaurusService;

    @BeforeEach
    void init() {
        metadataThesaurusService.reset();
        when(activityHandler.getMetadataOriginName()).thenReturn(handlerName);
    }

    @AfterEach
    void ends() {
        verify(fileEntity, atLeast(0)).getRealm();
        verify(fileEntity, atLeast(0)).getHashPath();
    }

    @Test
    void testReset() {
        metadataThesaurusService.getMimeType(fileEntity);
        assertThrows(AssertionError.class, () -> metadataThesaurusService.check());
        metadataThesaurusService.reset();
        metadataThesaurusService.check();
    }

    @Test
    void testCheckFileEntity() {
        metadataThesaurusService.check();
        assertThrows(AssertionError.class, () -> metadataThesaurusService.check(fileEntity));
        metadataThesaurusService.getMimeType(fileEntity);
        metadataThesaurusService.check(fileEntity).check();
    }

    @Test
    void testCheckActivityHandler() {
        metadataThesaurusService.check();
        assertThrows(AssertionError.class, () -> metadataThesaurusService.check(activityHandler));
        metadataThesaurusService.getThesaurus(activityHandler, fileEntity);
        metadataThesaurusService.check(activityHandler).check(fileEntity).check();
    }

    @Test
    void testGetThesaurus() {
        final var t = metadataThesaurusService.getThesaurus(activityHandler, fileEntity);
        t.dublinCore().format().set(value);
        assertThat(metadataThesaurusService.getTestThesaurus().dublinCore().format().get()).contains(value);
        metadataThesaurusService.check(activityHandler).check(fileEntity).check();
    }

    @Test
    void testGetAssertThesaurus() {
        final var assertThesaurusFormat = metadataThesaurusService.getAssertThesaurus().dublinCore().format();

        metadataThesaurusService.getThesaurus(activityHandler, fileEntity).dublinCore().format().set(value);

        assertThesaurusFormat.set(value);

        assertThrows(AssertionError.class, () -> assertThesaurusFormat.set(badValue));
        assertThrows(UnsupportedOperationException.class, assertThesaurusFormat::getAll);
        assertThrows(UnsupportedOperationException.class, assertThesaurusFormat::getAllInt);
        metadataThesaurusService.check(activityHandler).check(fileEntity).check();

        metadataThesaurusService.getThesaurus(activityHandler, fileEntity).dublinCore().format().set(value);
        assertThat(assertThesaurusFormat.get()).contains(value);
        metadataThesaurusService.check(activityHandler).check(fileEntity).check();
    }

    @Test
    void testGetReadOnlyThesaurus() {
        metadataThesaurusService.getTestThesaurus().dublinCore().format().set(value);
        final var t = metadataThesaurusService.getReadOnlyThesaurus(fileEntity);
        final var format = t.dublinCore().format();
        assertThat(format.get()).contains(value);
        assertThrows(UnsupportedOperationException.class, () -> format.set(value));
    }

    @Test
    void testGetValueLayerThesaurus() {
        final var t = metadataThesaurusService.getThesaurus(activityHandler, fileEntity);
        t.dublinCore().format().set(0, value);
        t.dublinCore().format().set(1, badValue);
        t.dublinCore().format().set(2, mimeType);

        assertThat(metadataThesaurusService.getTestThesaurus().dublinCore().format().getAll())
                .hasSize(3)
                .containsEntry(0, value)
                .containsEntry(1, badValue)
                .containsEntry(2, mimeType);
        metadataThesaurusService.check(activityHandler).check(fileEntity).check();
    }

    @Test
    void testGetSetMimeType() {
        assertThat(metadataThesaurusService.getMimeType(fileEntity)).isEmpty();
        metadataThesaurusService.setMimeType(activityHandler, fileEntity, mimeType);
        assertThat(metadataThesaurusService.getMimeType(fileEntity)).contains(mimeType);
        metadataThesaurusService.assertMimeTypeEquals(mimeType);
        metadataThesaurusService.check(activityHandler).check(fileEntity).check();
    }

    @Test
    void testAssertMimeTypeEquals_nope() {
        metadataThesaurusService.setMimeType(activityHandler, fileEntity, mimeType);
        metadataThesaurusService.assertMimeTypeEquals(mimeType);
        assertThrows(AssertionFailedError.class, () -> metadataThesaurusService.assertMimeTypeEquals(badValue));
    }
}
