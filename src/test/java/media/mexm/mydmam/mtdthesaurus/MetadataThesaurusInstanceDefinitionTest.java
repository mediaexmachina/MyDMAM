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

import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusInstanceDefinition.checkInterfaceClass;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusInstanceDefinition.extractClassifier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetadataThesaurusInstanceDefinitionTest {

    interface NoClassifier {
    }

    @MetadataThesaurusClassifier("")
    interface EmptyClassifier {
    }

    @MetadataThesaurusClassifier("a.b")
    interface DotClassifier {
    }

    @Test
    void testExtractClassifier() {
        assertThat(extractClassifier(MtdThesaurusDefDublinCore.class))
                .extracting(MetadataThesaurusClassifier::value)
                .isEqualTo("dc");
        assertThrows(IllegalArgumentException.class,
                () -> extractClassifier(NoClassifier.class));
        assertThrows(IllegalArgumentException.class,
                () -> extractClassifier(EmptyClassifier.class));
        assertThrows(IllegalArgumentException.class,
                () -> extractClassifier(DotClassifier.class));
    }

    MetadataThesaurusInstanceDefinition mtid;

    @BeforeEach
    void init() {
        mtid = new MetadataThesaurusInstanceDefinition(MtdThesaurusDefDublinCore.class);
    }

    @Test
    void testGetClassifier() {
        assertEquals("dc", mtid.getClassifier());
    }

    @MetadataThesaurusClassifier("cls")
    interface NotAbstractClassifier {
    }

    @Test
    void testCheckInterfaceClass_ok() {
        final var instanceClass = NotAbstractClassifier.class;
        final var methodList = Stream.of(instanceClass.getMethods())
                .sorted((l, r) -> l.getName().compareTo(r.getName()))
                .toList();
        checkInterfaceClass(instanceClass, methodList);
    }

    @MetadataThesaurusClassifier("cls")
    interface WithDefaultClassifier {
        default void aa() {
        }
    }

    @Test
    void testCheckInterfaceClass_withAbstract() {
        final var instanceClass = WithDefaultClassifier.class;
        final var methodList = Stream.of(instanceClass.getMethods())
                .sorted((l, r) -> l.getName().compareTo(r.getName()))
                .toList();
        assertThrows(IllegalArgumentException.class, () -> checkInterfaceClass(instanceClass, methodList));
    }

    @MetadataThesaurusClassifier("cls")
    class NotAnInterface {
    }

    @Test
    void testCheckInterfaceClass_notAnInterface() {
        final var instanceClass = NotAnInterface.class;
        assertThrows(IllegalArgumentException.class, // NOSONAR S5778
                () -> checkInterfaceClass(instanceClass, List.of()));
    }

    @Test
    void testGetAllMethods() {
        assertThat(mtid.getAllMethods()).hasSize(15);
        assertTrue(mtid.getAllMethods().stream()
                .anyMatch(f -> f.getName().equals("format")));
    }

    @Test
    void testGetKeyNameByMethod() {
        final var methods = mtid.getAllMethods();
        final var aMethod = methods.stream().findFirst().orElseThrow();
        assertEquals(aMethod.getName(), mtid.getKeyNameByMethod(aMethod));

        final var anotherMethod = getClass().getMethods()[0];
        assertThrows(IllegalArgumentException.class,
                () -> mtid.getKeyNameByMethod(anotherMethod));
    }

}
