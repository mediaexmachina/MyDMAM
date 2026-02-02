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
package media.mexm.mydmam.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class InternalObjectMapperTest {

	@MockitoBean
	ObjectMapper objectMapper;
	@Mock
	ClassToType value;
	@Fake
	String content;

	Class<ClassToType> valueType = ClassToType.class;
	TypeReference<ClassToType> valueTypeReference = new TypeReference<>() {};

	@Autowired
	InternalObjectMapper iom;

	static class ClassToType {
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(objectMapper);
	}

	@Test
	void testWriteValueAsString() throws JsonProcessingException {
		when(objectMapper.writeValueAsString(value)).thenReturn(content);

		assertThat(iom.writeValueAsString(value)).isEqualTo(content);

		verify(objectMapper, times(1)).writeValueAsString(value);
	}

	@Test
	void testWriteValueAsString_error() throws JsonProcessingException {
		when(objectMapper.writeValueAsString(value)).thenThrow(JsonProcessingException.class);

		assertThrows(IllegalArgumentException.class, () -> iom.writeValueAsString(value));

		verify(objectMapper, times(1)).writeValueAsString(value);
	}

	@Test
	void testReadValueStringClassOfT() throws JsonProcessingException {
		when(objectMapper.readValue(content, valueType)).thenReturn(value);

		assertThat(iom.readValue(content, valueType)).isEqualTo(value);

		verify(objectMapper, times(1)).readValue(content, valueType);
	}

	@Test
	void testReadValueStringClassOfT_error() throws JsonProcessingException {
		when(objectMapper.readValue(content, valueType)).thenThrow(JsonProcessingException.class);

		assertThrows(IllegalArgumentException.class, () -> iom.readValue(content, valueType));

		verify(objectMapper, times(1)).readValue(content, valueType);
	}

	@Test
	void testReadValueStringTypeReferenceOfT() throws JsonProcessingException {
		when(objectMapper.readValue(content, valueTypeReference)).thenReturn(value);

		assertThat(iom.readValue(content, valueTypeReference)).isEqualTo(value);

		verify(objectMapper, times(1)).readValue(content, valueTypeReference);
	}

	@Test
	void testReadValueStringTypeReferenceOfT_error() throws JsonProcessingException {
		when(objectMapper.readValue(content, valueTypeReference)).thenThrow(JsonProcessingException.class);

		assertThrows(IllegalArgumentException.class, () -> iom.readValue(content, valueTypeReference));

		verify(objectMapper, times(1)).readValue(content, valueTypeReference);
	}

}
