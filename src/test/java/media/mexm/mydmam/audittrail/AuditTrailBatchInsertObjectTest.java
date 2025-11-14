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
 * Copyright (C) Media ex Machina 2025
 *
 */
package media.mexm.mydmam.audittrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class AuditTrailBatchInsertObjectTest {

	@Mock
	Object objectPayload;
	@Mock
	ObjectMapper objectMapper;

	@Fake
	AuditTrailObjectType objectType;
	@Fake
	String objectReference;
	@Fake
	String json;

	AuditTrailBatchInsertObject ati;

	@BeforeEach
	void init() {
		ati = new AuditTrailBatchInsertObject(objectType, objectReference, objectPayload);
	}

	@Test
	void testMakeAuditTrailItem() throws JsonProcessingException {
		when(objectMapper.writeValueAsString(objectPayload)).thenReturn(json);
		final var result = ati.makeAuditTrailItem(objectMapper);
		assertThat(result).isEqualTo(new AuditTrailItem(objectType, objectReference, json));
		verify(objectMapper, times(1)).writeValueAsString(objectPayload);
	}

	@Test
	void testMakeAuditTrailItem_fail() throws JsonProcessingException {
		when(objectMapper.writeValueAsString(objectPayload)).thenThrow(JsonProcessingException.class);
		assertThrows(IllegalArgumentException.class, () -> ati.makeAuditTrailItem(objectMapper));
		verify(objectMapper, times(1)).writeValueAsString(objectPayload);
	}

}
