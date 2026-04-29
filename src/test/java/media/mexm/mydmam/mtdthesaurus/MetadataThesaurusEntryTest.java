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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class MetadataThesaurusEntryTest {

    @Fake
    String classifier;
    @Fake
    String parent;
    @Fake
    String key;

    @Test
    void testMetadataThesaurusEntry() {
        assertThat(new MetadataThesaurusEntryImpl(classifier, parent, key))
                .isEqualTo(new MetadataThesaurusEntryImpl(classifier, parent + "." + key, empty()));

        assertThat(new MetadataThesaurusEntryImpl(classifier, "", key))
                .isEqualTo(new MetadataThesaurusEntryImpl(classifier, key, empty()));

    }

}
