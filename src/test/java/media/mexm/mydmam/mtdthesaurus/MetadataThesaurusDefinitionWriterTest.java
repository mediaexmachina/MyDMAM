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
package media.mexm.mydmam.mtdthesaurus;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusDefinitionWriter.WritedLayerValue;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class MetadataThesaurusDefinitionWriterTest {

    @Mock
    Object object;
    @Fake
    int layer;
    @Fake
    String value;
    @Fake
    int number;

    MetadataThesaurusDefinitionWriter<Object> w;

    @BeforeEach
    void init() {
        w = new MetadataThesaurusDefinitionWriter<>();
    }

    @Test
    void testSetInstance() {
        w.setInstance(object);
        assertThrows(IllegalArgumentException.class, () -> w.setInstance(object));
    }

    @Test
    void testGetSet() {
        w.setInstance(object);

        assertThat(w.get()).isEmpty();
        assertEquals(object, w.set(layer, value));
        assertThat(w.get()).contains(new WritedLayerValue(layer, value));
        assertThat(w.get()).isEmpty();
    }

    @Test
    void testGetSet_defaultLayer() {
        w.setInstance(object);

        assertThat(w.get()).isEmpty();
        assertEquals(object, w.set(value));
        assertThat(w.get()).contains(new WritedLayerValue(0, value));
        assertThat(w.get()).isEmpty();
    }

    @Test
    void testSet_null() {
        w.setInstance(object);

        assertEquals(object, w.set(null));
        assertThat(w.get()).isEmpty();
    }

    @Test
    void testSet_optionalEmpty() {
        w.setInstance(object);

        assertEquals(object, w.set(empty()));
        assertThat(w.get()).isEmpty();
    }

    @Test
    void testSet_optionalValue() {
        w.setInstance(object);

        assertEquals(object, w.set(Optional.ofNullable(value)));
        assertThat(w.get()).contains(new WritedLayerValue(0, value));
    }

    @Test
    void testSet_number() {
        w.setInstance(object);

        assertEquals(object, w.set(number));
        assertThat(w.get()).contains(new WritedLayerValue(0, String.valueOf(number)));
    }

    @Test
    void testSet_noInstance() {
        assertThrows(IllegalArgumentException.class, () -> w.set(value));
    }

}
