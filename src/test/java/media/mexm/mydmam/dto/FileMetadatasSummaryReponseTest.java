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
package media.mexm.mydmam.dto;

import static media.mexm.mydmam.component.InternalObjectMapper.TYPE_MAP_STRING_STRING;
import static media.mexm.mydmam.dto.FileMetadatasSummaryResponse.createFromAssetSummaryEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.component.InternalObjectMapper;
import media.mexm.mydmam.entity.AssetSummaryEntity;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class FileMetadatasSummaryReponseTest {

	@Mock
	AssetSummaryEntity assetSummaryEntity;
	@Mock
	InternalObjectMapper objectMapper;

	@Fake
	String mimeType;
	@Fake
	String specificationKey;
	@Fake
	String specificationValue;
	@Fake
	String specificationJson;

	@Test
	void testCreateFromAssetSummaryEntity() {
		when(assetSummaryEntity.getSpecifications())
				.thenReturn(specificationJson);
		when(assetSummaryEntity.getMimeType())
				.thenReturn(mimeType);
		when(objectMapper.readValue(specificationJson, TYPE_MAP_STRING_STRING))
				.thenReturn(Map.of(specificationKey, specificationValue));

		final var fmr = createFromAssetSummaryEntity(assetSummaryEntity, objectMapper);
		assertEquals(mimeType, fmr.mimeType());
		assertEquals(Map.of(specificationKey, specificationValue), fmr.specifications());

		verify(assetSummaryEntity, times(1)).getSpecifications();
		verify(assetSummaryEntity, times(1)).getMimeType();
		verify(objectMapper, times(1)).readValue(specificationJson, TYPE_MAP_STRING_STRING);
	}

}
