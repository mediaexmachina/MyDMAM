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

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.util.Optional;

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
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusClassifier;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntry;
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
        when(activityHandler.getHandlerName()).thenReturn(handlerName);
    }

    @AfterEach
    void ends() {
        verify(fileEntity, atLeast(0)).getRealm();
        verify(fileEntity, atLeast(0)).getHashPath();
    }

    @MetadataThesaurusClassifier(value = "classifier")
    public interface MtdThesaurusDef {
        MetadataThesaurusEntry data();
    }

    @Test
    void testEndChecks() {
        metadataThesaurusService.endChecks(fileEntity);

        metadataThesaurusService.getWriter(activityHandler, fileEntity, MtdThesaurusDef.class).set(value).data();
        assertThrows(AssertionFailedError.class, () -> metadataThesaurusService.endChecks(fileEntity));
        metadataThesaurusService.reset();

        metadataThesaurusService.getReader(MtdThesaurusDef.class, fileEntity).data();
        metadataThesaurusService.reset();

        metadataThesaurusService.addResponse(MtdThesaurusDef.class, value).data();
        assertThrows(AssertionError.class, () -> metadataThesaurusService.endChecks(fileEntity));

        verify(activityHandler, atLeastOnce()).getHandlerName();
    }

    @Test
    void testCheckIfAdded() {
        metadataThesaurusService.getWriter(activityHandler, fileEntity, MtdThesaurusDef.class).set(value).data();
        metadataThesaurusService.checkIfAdded(MtdThesaurusDef.class, value).data();
        verify(activityHandler, atLeastOnce()).getHandlerName();
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testCheckIfAdded_withLayer() {
        metadataThesaurusService.getWriter(activityHandler, fileEntity, MtdThesaurusDef.class).set(layer, value).data();
        metadataThesaurusService.checkIfAdded(MtdThesaurusDef.class, layer, value).data();
        verify(activityHandler, atLeastOnce()).getHandlerName();
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testCheckIfAdded_badLayer() {
        metadataThesaurusService.getWriter(activityHandler, fileEntity, MtdThesaurusDef.class).set(value).data();
        final var added = metadataThesaurusService.checkIfAdded(MtdThesaurusDef.class, layer, value);
        assertThrows(AssertionFailedError.class, added::data);
        verify(activityHandler, atLeastOnce()).getHandlerName();
    }

    @Test
    void testCheckIfAdded_notFound() {
        final var added = metadataThesaurusService.checkIfAdded(MtdThesaurusDef.class, value);
        assertThrows(AssertionFailedError.class, added::data);
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testCheckIfAdded_moreThanOneValue() {
        final var writer = metadataThesaurusService.getWriter(activityHandler, fileEntity, MtdThesaurusDef.class);
        writer.set(value).data();
        writer.set(badValue).data();

        final var added = metadataThesaurusService.checkIfAdded(MtdThesaurusDef.class, value);
        assertThrows(AssertionFailedError.class, added::data);
        verify(activityHandler, atLeastOnce()).getHandlerName();
    }

    @Test
    void testCheckIfAdded_badValue() {
        metadataThesaurusService.getWriter(activityHandler, fileEntity, MtdThesaurusDef.class).set(value).data();
        final var added = metadataThesaurusService.checkIfAdded(MtdThesaurusDef.class, badValue);
        assertThrows(AssertionFailedError.class, added::data);
        verify(activityHandler, atLeastOnce()).getHandlerName();
    }

    @Test
    void testAddResponse() {
        metadataThesaurusService.addResponse(MtdThesaurusDef.class, value).data();
        assertThat(metadataThesaurusService.getReader(MtdThesaurusDef.class, fileEntity).data().value())
                .contains(value);
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testAddResponse_optional() {
        metadataThesaurusService.addResponse(MtdThesaurusDef.class, Optional.ofNullable(value)).data();
        assertThat(metadataThesaurusService.getReader(MtdThesaurusDef.class, fileEntity).data().value())
                .contains(value);
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testAddResponse_empty() {
        metadataThesaurusService.addResponse(MtdThesaurusDef.class, value).data();
        metadataThesaurusService.addResponse(MtdThesaurusDef.class, empty()).data();
        assertThat(metadataThesaurusService.getReader(MtdThesaurusDef.class, fileEntity).data().value()).isEmpty();
    }

    @Test
    void testSetMimeType() {
        metadataThesaurusService.setMimeType(mimeType);
        assertThat(metadataThesaurusService.getMimeType(fileEntity)).contains(mimeType);
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testGetMimeType() {
        assertThat(metadataThesaurusService.getMimeType(fileEntity)).isEmpty();
    }

    @Test
    void testGetReader() {
        assertThat(metadataThesaurusService.getReader(MtdThesaurusDef.class, fileEntity).data().value())
                .isEmpty();
        assertThat(metadataThesaurusService.getReader(MtdThesaurusDef.class, fileEntity, layer).data().value())
                .isEmpty();

        metadataThesaurusService.addResponse(MtdThesaurusDef.class, value).data();
        assertThat(metadataThesaurusService.getReader(MtdThesaurusDef.class, fileEntity).data().value())
                .contains(value);

        metadataThesaurusService.addResponse(MtdThesaurusDef.class, layer, value).data();
        assertThat(metadataThesaurusService.getReader(MtdThesaurusDef.class, fileEntity, layer).data().value())
                .contains(value);
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testGetValue_empty() {
        assertThat(metadataThesaurusService.getValue(fileEntity,
                new MetadataThesaurusEntry("classifier", "data", empty()))).isEmpty();
        assertThat(metadataThesaurusService.getValue(fileEntity, layer,
                new MetadataThesaurusEntry("classifier", "data", empty()))).isEmpty();
    }

    @Test
    void testGetValue_withData() {
        metadataThesaurusService.addResponse(MtdThesaurusDef.class, value).data();
        assertThat(metadataThesaurusService.getValue(fileEntity,
                new MetadataThesaurusEntry("classifier", "data", empty()))).contains(value);
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testGetValue_withDataWithLayer() {
        metadataThesaurusService.addResponse(MtdThesaurusDef.class, layer, value).data();
        assertThat(metadataThesaurusService.getValue(fileEntity, layer,
                new MetadataThesaurusEntry("classifier", "data", empty()))).contains(value);
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testGetWriter() {
        final var w = metadataThesaurusService.getWriter(activityHandler, fileEntity, MtdThesaurusDef.class);
        assertNotNull(w);
        assertNull(w.set(value).data());
        metadataThesaurusService.checkIfAdded(MtdThesaurusDef.class, value).data();
        verify(activityHandler, atLeastOnce()).getHandlerName();
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testGetWriter_withLayer() {
        final var w = metadataThesaurusService.getWriter(activityHandler, fileEntity, MtdThesaurusDef.class);
        assertNotNull(w);
        assertNull(w.set(layer, value).data());
        metadataThesaurusService.checkIfAdded(MtdThesaurusDef.class, layer, value).data();
        verify(activityHandler, atLeastOnce()).getHandlerName();
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testGetWriter_empty() {
        final var w = metadataThesaurusService.getWriter(activityHandler, fileEntity, MtdThesaurusDef.class);
        assertNotNull(w);
        w.set(value);
        verify(activityHandler, atLeastOnce()).getHandlerName();
        metadataThesaurusService.endChecks(fileEntity);
    }

}
