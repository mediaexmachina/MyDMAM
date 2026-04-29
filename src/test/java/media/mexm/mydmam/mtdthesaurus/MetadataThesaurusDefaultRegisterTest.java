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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class MetadataThesaurusDefaultRegisterTest {

    class Impl implements MetadataThesaurusRegister {

        Set<Class<?>> called = new HashSet<>();

        @Override
        public <T> T makeInstance(final Class<T> fromClass) {
            called.add(fromClass);
            return null;
        }

    }

    @Test
    void testDefaultRegister() {
        final var i = new Impl();
        i.defaultRegister();
        assertThat(i.called)
                .containsExactlyInAnyOrder(
                        MtdThesaurusDefTechnical.class,
                        MtdThesaurusDefChapter.class,
                        MtdThesaurusDefTechnicalAudio.class,
                        MtdThesaurusDefTechnicalStream.class,
                        MtdThesaurusDefTechnicalImage.class,
                        MtdThesaurusDefTechnicalTransportStream.class,
                        MtdThesaurusDefTechnicalVideo.class,
                        MtdThesaurusDefTechnicalContainer.class,
                        MtdThesaurusDefTechnicalMXF.class,
                        MtdThesaurusDefPDF.class,
                        MtdThesaurusDefDCMI.class,
                        MtdThesaurusDefDublinCore.class,
                        MtdThesaurusDefXMP.class);
    }
}
