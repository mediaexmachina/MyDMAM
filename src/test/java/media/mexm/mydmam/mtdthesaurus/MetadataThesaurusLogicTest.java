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

import static java.time.Instant.now;
import static java.util.Optional.empty;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.EQUALS;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.HASH_CODE;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.TO_STRING;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.checkMethodNotEquals;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.checkMethodNotHashCode;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.checkMethodNotHaveArgs;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.checkMethodNotToString;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.nameFormatter;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.set;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.setDateISO8601;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic.MtdRegisterDefinition;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class MetadataThesaurusLogicTest {

    @Mock
    MetadataThesaurusEntryIOProvider provider;
    @Mock
    Method method;

    String classifier = "dc";
    String key = "format";
    String now;
    long nowUnixtime;

    @Fake
    String methodName;
    @Fake
    String value;
    @Fake(min = 1, max = 1000)
    int layer;

    @Fake
    int defaultInt;
    @Fake
    int regularInt;

    MetadataThesaurusLogic mtl;

    @BeforeEach
    void init() {
        when(method.getName()).thenReturn(methodName);

        mtl = new MetadataThesaurusLogic();

        final var nowDate = now();
        now = nowDate.toString();
        nowUnixtime = nowDate.toEpochMilli();
    }

    @Test
    void testGetRegisterDefinitions() {
        final var result = mtl.getRegisterDefinitions();
        assertThat(result).isNotEmpty();

        assertThat(result.entrySet().stream()
                .filter(f -> f.getKey().getName().equals("dublinCore"))
                .findFirst()
                .map(Entry::getValue)
                .map(Class::getName))
                        .contains(MtdThesaurusDefDublinCore.class.getName());
    }

    @Test
    void testGetImplementsFromRegister() {
        final var result = mtl.getImplementsFromRegister();
        assertThat(result).isNotEmpty();
        final var allMethods = result.stream().map(MtdRegisterDefinition::methods).flatMap(List::stream).toList();
        assertThat(allMethods).isNotEmpty();
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

    @Test
    void testMakeRegister() {
        final var register = mtl.makeRegister(provider);
        assertNotNull(register);

        assertThrows(UnsupportedOperationException.class, register::hashCode);
        assertThrows(UnsupportedOperationException.class, () -> register.equals(register));
        assertThrows(UnsupportedOperationException.class, register::toString);

        assertNotNull(register.dublinCore());
    }

    @Test
    void testInjectInstanceWithIO() {
        final var def = mtl.injectInstanceWithIO(provider, MtdThesaurusDefDublinCore.class);
        assertNotNull(def);

        assertThrows(UnsupportedOperationException.class, def::hashCode);
        assertThrows(UnsupportedOperationException.class, () -> def.equals(def));
        assertThrows(UnsupportedOperationException.class, def::toString);

        assertNotNull(def.format());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testOnEntryCall() {
        assertThrows(UnsupportedOperationException.class,
                () -> mtl.onEntryCall(provider, classifier, key, EQUALS, null));
        assertThrows(UnsupportedOperationException.class,
                () -> mtl.onEntryCall(provider, classifier, key, HASH_CODE, null));
        assertThat(mtl.onEntryCall(provider, classifier, key, TO_STRING, null))
                .isEqualTo(classifier + "." + key);
        assertThat(mtl.onEntryCall(provider, classifier, key, "classifier", null))
                .isEqualTo(classifier);
        assertThat(mtl.onEntryCall(provider, classifier, key, "key", null))
                .isEqualTo(key);

        mtl.onEntryCall(provider, classifier, key, "set", args(value));
        verify(provider, times(1)).setValueToDatabase(classifier, key, 0, value);

        mtl.onEntryCall(provider, classifier, key, "set", args(layer, value));
        verify(provider, times(1)).setValueToDatabase(classifier, key, layer, value);

        mtl.onEntryCall(provider, classifier, key, "setDateISO8601", args(Optional.ofNullable(now)));
        verify(provider, times(1)).setValueToDatabase(classifier, key, 0, String.valueOf(nowUnixtime));

        mtl.onEntryCall(provider, classifier, key, "setDateISO8601", args(layer, Optional.ofNullable(now)));
        verify(provider, times(1)).setValueToDatabase(classifier, key, layer, String.valueOf(nowUnixtime));

        when(provider.getValueFromDatabase(classifier, key, 0)).thenReturn(Optional.ofNullable(value));
        var result = (Optional<String>) mtl.onEntryCall(provider, classifier, key, "get", args());
        assertThat(result).contains(value);
        verify(provider, times(1)).getValueFromDatabase(classifier, key, 0);

        when(provider.getValueFromDatabase(classifier, key, layer)).thenReturn(Optional.ofNullable(value));
        result = (Optional<String>) mtl.onEntryCall(provider, classifier, key, "get", args(layer));
        assertThat(result).contains(value);
        verify(provider, times(1)).getValueFromDatabase(classifier, key, layer);

        key = key + "-int";
        when(provider.getValueFromDatabase(classifier, key, 0))
                .thenReturn(Optional.ofNullable(String.valueOf(regularInt)));
        var resultInt = (int) mtl.onEntryCall(provider, classifier, key, "getAsInt", args(defaultInt));
        assertThat(resultInt).isEqualTo(regularInt);
        verify(provider, times(1)).getValueFromDatabase(classifier, key, 0);

        when(provider.getValueFromDatabase(classifier, key, layer))
                .thenReturn(Optional.ofNullable(String.valueOf(regularInt)));
        resultInt = (int) mtl.onEntryCall(provider, classifier, key, "getAsInt", args(layer, defaultInt));
        assertThat(resultInt).isEqualTo(regularInt);
        verify(provider, times(1)).getValueFromDatabase(classifier, key, layer);

        key = key + "NOPE";
        resultInt = (int) mtl.onEntryCall(provider, classifier, key, "getAsInt", args(defaultInt));
        assertThat(resultInt).isEqualTo(defaultInt);
        verify(provider, times(1)).getValueFromDatabase(classifier, key, 0);

        resultInt = (int) mtl.onEntryCall(provider, classifier, key, "getAsInt", args(layer, defaultInt));
        assertThat(resultInt).isEqualTo(defaultInt);
        verify(provider, times(1)).getValueFromDatabase(classifier, key, layer);

        when(provider.getValueLayerFromDatabase(classifier, key)).thenReturn(Map.of(layer, value));
        final var resultMapString = (Map<Integer, String>) mtl.onEntryCall(provider, classifier, key, "getAll", args());
        assertThat(resultMapString).isEqualTo(Map.of(layer, value));
        verify(provider, times(1)).getValueLayerFromDatabase(classifier, key);

        when(provider.getValueLayerFromDatabase(classifier, key)).thenReturn(Map.of(layer, String.valueOf(regularInt)));
        final var resultMapInt = (Map<Integer, Integer>) mtl
                .onEntryCall(provider, classifier, key, "getAllInt", args());
        assertThat(resultMapInt).isEqualTo(Map.of(layer, regularInt));
        verify(provider, times(2)).getValueLayerFromDatabase(classifier, key);

    }

    static Object[] args(final Object... args) {
        if (args == null || args.length == 0) {
            return null;
        }
        return args;
    }

    @Test
    void testSetDateISO8601() {
        assertThat(setDateISO8601(Optional.ofNullable(now))).contains(String.valueOf(nowUnixtime));
        assertThat(setDateISO8601(Optional.ofNullable(value))).isEmpty();
        assertThat(setDateISO8601(empty())).isEmpty();
    }

    @Test
    void testSet() {
        assertThat(set(value)).contains(value);
        assertThat(set(null)).isEmpty();
        assertThat(set(" ")).isEmpty();
        assertThat(set(Duration.ofMillis(nowUnixtime))).contains(String.valueOf(nowUnixtime));
        assertThat(set(regularInt)).contains(String.valueOf(regularInt));

        assertThat(set(Optional.ofNullable(value))).contains(value);
        assertThat(set(empty())).isEmpty();
        assertThat(set(Optional.ofNullable(" "))).isEmpty();
        assertThat(set(Optional.ofNullable(Duration.ofMillis(nowUnixtime)))).contains(String.valueOf(nowUnixtime));
        assertThat(set(Optional.ofNullable(regularInt))).contains(String.valueOf(regularInt));
    }

    @Test
    void testCheckMethodNotHaveArgs() {
        checkMethodNotHaveArgs(method, null);
        checkMethodNotHaveArgs(method, new Object[] {});

        final var arg = new Object[] { value };
        assertThrows(UnsupportedOperationException.class, () -> checkMethodNotHaveArgs(method, arg));
        verify(method, atLeastOnce()).getName();
    }

    @Test
    void testCheckMethodNotHashCode() {
        checkMethodNotHashCode(method);
        when(method.getName()).thenReturn(HASH_CODE);
        assertThrows(UnsupportedOperationException.class, () -> checkMethodNotHashCode(method));
        verify(method, atLeastOnce()).getName();
    }

    @Test
    void testCheckMethodNotToString() {
        checkMethodNotToString(method);
        when(method.getName()).thenReturn(TO_STRING);
        assertThrows(UnsupportedOperationException.class, () -> checkMethodNotToString(method));
        verify(method, atLeastOnce()).getName();
    }

    @Test
    void testCheckMethodNotEquals() {
        checkMethodNotEquals(method);
        when(method.getName()).thenReturn(EQUALS);
        assertThrows(UnsupportedOperationException.class, () -> checkMethodNotEquals(method));
        verify(method, atLeastOnce()).getName();
    }

}
