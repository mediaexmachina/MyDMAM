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
package media.mexm.mydmam.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class JsonPathHelperTest {

	@Mock
	DocumentContext document;
	@Mock
	TypeRef<ClassToType> typeRef;
	@Mock
	ClassToType object;

	@Fake
	String path;

	Class<ClassToType> type = ClassToType.class;
	JsonPathHelper jph;

	static class ClassToType {
	}

	@BeforeEach
	void init() {
		jph = new JsonPathHelper(document);
	}

	@Test
	void testReadStringClassOfT() {
		when(document.read(path, type)).thenReturn(object);

		assertThat(jph.read(path, type)).hasValue(object);

		verify(document, times(1)).read(path, type);
	}

	@Test
	void testReadStringClassOfT_error() {
		when(document.read(path, type)).thenThrow(PathNotFoundException.class);

		assertThat(jph.read(path, type)).isEmpty();

		verify(document, times(1)).read(path, type);
	}

	@Test
	void testReadStringTypeRefOfT() {
		when(document.read(path, typeRef)).thenReturn(object);

		assertThat(jph.read(path, typeRef)).hasValue(object);

		verify(document, times(1)).read(path, typeRef);
	}

	@Test
	void testReadStringTypeRefOfT_error() {
		when(document.read(path, typeRef)).thenThrow(PathNotFoundException.class);

		assertThat(jph.read(path, typeRef)).isEmpty();

		verify(document, times(1)).read(path, typeRef);
	}

}
