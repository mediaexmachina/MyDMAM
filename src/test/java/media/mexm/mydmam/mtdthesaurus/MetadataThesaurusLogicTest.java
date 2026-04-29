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
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.nameFormatter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetadataThesaurusLogicTest {

    MetadataThesaurusLogic mtl;

    @BeforeEach
    void init() {
        mtl = new MetadataThesaurusLogic();
    }

    @Test
    void testNameFormatter() {
        assertEquals("name", nameFormatter("name"));
        assertEquals("key-name", nameFormatter("keyName"));
        assertEquals("name", nameFormatter("Name"));
        assertEquals("name", nameFormatter("_name"));
        assertEquals("name", nameFormatter("_Name"));
        assertEquals("nam-e", nameFormatter("namE"));
        assertEquals("test", nameFormatter("TEST"));
        assertEquals("abc", nameFormatter("ABC"));
        assertEquals("ab", nameFormatter("AB"));
        assertEquals("a", nameFormatter("A"));
        assertEquals("name-test-name", nameFormatter("nameTESTname"));
        assertEquals("name-abc-name", nameFormatter("nameABCname"));
        assertEquals("name-ab-name", nameFormatter("nameABname"));
        assertEquals("name-aname", nameFormatter("nameAname"));
    }

    @MetadataThesaurusClassifier(value = "classifier")
    public interface TestThesaurus {
        MetadataThesaurusEntry width();

    }

    @Test
    void testMakeInstance() {
        final var width = mtl.makeInstance(TestThesaurus.class).width();
        assertThat(width).isNotNull().isEqualTo(new MetadataThesaurusEntryImpl("classifier", "width", empty()));
    }

    @MetadataThesaurusClassifier(value = "classifier-bis")
    public interface TestThesaurusBis {
        MetadataThesaurusEntry height();
    }

    @Test
    void testMakeInstance_hashCodeEquals() {
        final var e0 = mtl.makeInstance(TestThesaurus.class);
        final var e1 = mtl.makeInstance(TestThesaurus.class);
        assertEquals(e0, e1);
        assertEquals(e0.hashCode(), e1.hashCode());

        final var e2 = mtl.makeInstance(TestThesaurusBis.class);
        assertNotEquals(e0, e2);
        assertNotEquals(e0.hashCode(), e2.hashCode());
    }

    @MetadataThesaurusClassifier(value = "")
    public interface TestInvalidThesaurusNoClassifier {
        MetadataThesaurusEntry width();
    }

    @Test
    void testMakeInstance_noClassifier() {
        assertThrows(IllegalArgumentException.class, () -> mtl.makeInstance(TestInvalidThesaurusNoClassifier.class));
    }

    @MetadataThesaurusClassifier(value = "classifier")
    public interface TestThesaurusSameClassifier {
        MetadataThesaurusEntry width();
    }

    @Test
    void testMakeInstance_sameClassifier() {
        mtl.makeInstance(TestThesaurus.class);
        final var width2 = mtl.makeInstance(TestThesaurusSameClassifier.class).width();
        assertThat(width2).isNotNull().isEqualTo(new MetadataThesaurusEntryImpl("classifier", "width", empty()));

        final var width = mtl.makeInstance(TestThesaurus.class).width();
        assertThat(width).isNotNull().isEqualTo(new MetadataThesaurusEntryImpl("classifier", "width", empty()));
    }

    @MetadataThesaurusClassifier(value = "classifier")
    public interface TestThesaurusWithDefault {

        default MetadataThesaurusEntry isDefault() {
            throw new UnsupportedOperationException();
        }

    }

    @Test
    void testMakeInstance_withDefault() {
        assertThrows(IllegalArgumentException.class, () -> mtl.makeInstance(TestThesaurusWithDefault.class));
    }

    @MetadataThesaurusClassifier(value = "classifier")
    public interface TestThesaurusWithStatic {

        static MetadataThesaurusEntry isStatic() {
            throw new UnsupportedOperationException();
        }

    }

    @Test
    void testMakeInstance_withStatic() {
        assertThrows(IllegalArgumentException.class, () -> mtl.makeInstance(TestThesaurusWithStatic.class));
    }

    @MetadataThesaurusClassifier("classifier")
    public class TestInvalidThesaurusNotInterface {
        public MetadataThesaurusEntry width() {
            return null;
        }
    }

    @Test
    void testMakeInstance_notInterface() {
        assertThrows(IllegalArgumentException.class, () -> mtl.makeInstance(TestInvalidThesaurusNotInterface.class));
    }

    @MetadataThesaurusClassifier(value = "classifier")
    public interface TestInvalidThesaurusWithParams {
        MetadataThesaurusEntry width(int value);
    }

    @Test
    void testMakeInstance_withParams() {
        assertThrows(IllegalArgumentException.class, () -> mtl.makeInstance(TestInvalidThesaurusWithParams.class));
    }

    @MetadataThesaurusClassifier(value = "classifier")
    public interface TestInvalidThesaurusBadReturnType {
        String width();
    }

    @Test
    void testMakeInstance_badReturnType() {
        assertThrows(IllegalArgumentException.class, () -> mtl.makeInstance(TestInvalidThesaurusBadReturnType.class));
    }

    public interface TestInvalidThesaurusNoAnnotation {
        String width();
    }

    @Test
    void testMakeInstance_noAnnotation() {
        assertThrows(IllegalArgumentException.class, () -> mtl.makeInstance(TestInvalidThesaurusNoAnnotation.class));
    }

    @MetadataThesaurusClassifier(value = "classifier-bis", parent = "parent")
    public interface TestThesaurusParent {
        MetadataThesaurusEntry width();

        MetadataThesaurusEntry anotherValue();
    }

    @Test
    void testMakeInstance_parent() {
        final var width = mtl.makeInstance(TestThesaurusParent.class).width();

        mtl.makeInstance(TestThesaurusParent.class);

        assertThat(width).isNotNull().isEqualTo(new MetadataThesaurusEntryImpl("classifier-bis", "parent.width",
                empty()));
    }

    @MetadataThesaurusClassifier(value = "classifier.ddd")
    public interface TestInvalidThesaurusClassifierDot {
        MetadataThesaurusEntry width();
    }

    @Test
    void testMakeInstance_classifierDot() {
        assertThrows(IllegalArgumentException.class, () -> mtl.makeInstance(TestInvalidThesaurusClassifierDot.class));
    }

    @Test
    void testToString() {
        mtl.makeInstance(TestThesaurus.class);
        mtl.makeInstance(TestThesaurusParent.class);
        assertThat(mtl.toString()).isNotBlank();
    }

    @Test
    void testGetImplements() {
        mtl.makeInstance(TestThesaurus.class);
        mtl.makeInstance(TestThesaurusParent.class);
        assertThat(mtl.getImplements()).isNotEmpty();
    }

}
