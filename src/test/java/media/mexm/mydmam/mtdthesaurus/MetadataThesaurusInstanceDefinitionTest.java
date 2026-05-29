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

import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusClassifierKind.BIBLIOGRAPHIC_RECORD;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryNumericalUnit.BITS_PER_SECONDS;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryNumericalUnit.NO_UNIT;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryType.DISPLAYED_AS_IT;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryType.IDENTIFIER_OR_SERIAL_ID;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusInstanceDefinition.checkInterfaceClass;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusInstanceDefinition.extractClassifier;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusInstanceDefinition.prettifyMethodKeyName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import media.mexm.mydmam.dto.MtdThesaurusDefEntrySignature;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.MtdRegisterMethodDefinition;

class MetadataThesaurusInstanceDefinitionTest {

    MetadataThesaurusInstanceDefinition mtid;

    interface NoClassifier {
    }

    @MetadataThesaurusClassifier(value = "", longname = "ln", kind = BIBLIOGRAPHIC_RECORD)
    interface EmptyClassifier {
    }

    @MetadataThesaurusClassifier(value = "a.b", longname = "ln", kind = BIBLIOGRAPHIC_RECORD)
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

    @Test
    void testGetClassifier() {
        mtid = new MetadataThesaurusInstanceDefinition(MtdThesaurusDefDublinCore.class);
        assertEquals("dc", mtid.getClassifier());
    }

    @MetadataThesaurusClassifier(value = "cls", longname = "ln", kind = BIBLIOGRAPHIC_RECORD)
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

    @MetadataThesaurusClassifier(value = "cls", longname = "ln", kind = BIBLIOGRAPHIC_RECORD)
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

    @MetadataThesaurusClassifier(value = "cls", longname = "ln", kind = BIBLIOGRAPHIC_RECORD)
    class NotAnInterface {
    }

    @Test
    void testCheckInterfaceClass_notAnInterface() {
        final var instanceClass = NotAnInterface.class;
        assertThrows(IllegalArgumentException.class, // NOSONAR S5778
                () -> checkInterfaceClass(instanceClass, List.of()));
    }

    @Test
    void testPrettifyMethodKeyName() {
        assertThat(prettifyMethodKeyName("")).isEmpty();
        assertThat(prettifyMethodKeyName("a")).isEqualTo("a");
        assertThat(prettifyMethodKeyName("A")).isEqualTo("A");
        assertThat(prettifyMethodKeyName("keyName")).isEqualTo("KeyName");
        assertThat(prettifyMethodKeyName("key-Name")).isEqualTo("Key Name");
    }

    @MetadataThesaurusClassifier(value = "cls", longname = "Test classifier", kind = BIBLIOGRAPHIC_RECORD)
    public interface FullClassifier {
        /**
         * 1
         */
        MetadataThesaurusEntry a1();

        /**
         * 4
         */
        @MetadataThesaurusEntryAttribute(unit = BITS_PER_SECONDS)
        MetadataThesaurusEntry b2();

        /**
         * 2
         */
        MetadataThesaurusEntry aaaBbb2();

        /**
         * 0
         */
        @MetadataThesaurusSortIndexOrder(1)
        MetadataThesaurusEntry b0();

        /**
         * 3
         */
        @MetadataThesaurusEntryAttribute(type = IDENTIFIER_OR_SERIAL_ID, longname = "LongName !")
        MetadataThesaurusEntry b1();

    }

    @Test
    void testExtractAllMethodDefinitions() {
        mtid = new MetadataThesaurusInstanceDefinition(FullClassifier.class);
        final var def = mtid.extractAllMethodDefinitions(10);

        assertThat(def).hasSize(5);

        assertThat(def.get(0))
                .isEqualTo(new MtdRegisterMethodDefinition(
                        "b0",
                        "b0",
                        new MtdThesaurusDefEntrySignature("B0", 10000, DISPLAYED_AS_IT, NO_UNIT)));
        assertThat(def.get(1))
                .isEqualTo(new MtdRegisterMethodDefinition(
                        "a1",
                        "a1",
                        new MtdThesaurusDefEntrySignature("A1", 10001, DISPLAYED_AS_IT, NO_UNIT)));
        assertThat(def.get(2))
                .isEqualTo(new MtdRegisterMethodDefinition(
                        "aaaBbb2",
                        "aaa-bbb2",
                        new MtdThesaurusDefEntrySignature("Aaa bbb2", 10002, DISPLAYED_AS_IT, NO_UNIT)));
        assertThat(def.get(3))
                .isEqualTo(new MtdRegisterMethodDefinition(
                        "b1",
                        "b1",
                        new MtdThesaurusDefEntrySignature("LongName !", 10003, IDENTIFIER_OR_SERIAL_ID, NO_UNIT)));
        assertThat(def.get(4))
                .isEqualTo(new MtdRegisterMethodDefinition(
                        "b2",
                        "b2",
                        new MtdThesaurusDefEntrySignature("B2", 10004, DISPLAYED_AS_IT, BITS_PER_SECONDS)));
    }

}
