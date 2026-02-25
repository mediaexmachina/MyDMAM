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

import static media.mexm.mydmam.dto.AssetResponse.buildFromEntities;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class AssetResponseTest {

	@Mock
	FileMetadataEntity fileMetadataEntity;
	@Mock
	AssetRenderedFileEntity assetRenderedFileEntity;
	@Mock
	KeyValueMetadataResponse keyValueMetadataResponse;
	@Mock
	RenderedFileResponse renderedFileResponse;

	@Fake
	int indexLayer;

	AssetResponse ar;

	@Test
	void testBuildFromEntities_null() {
		ar = buildFromEntities(null, null);
		assertThat(ar.index()).isEqualTo(Map.of());
	}

	@Test
	void testBuildFromEntities() {
		when(fileMetadataEntity.toKeyValueMetadataResponse()).thenReturn(keyValueMetadataResponse);
		when(fileMetadataEntity.getLayer()).thenReturn(indexLayer);
		when(assetRenderedFileEntity.toRenderedReponse()).thenReturn(renderedFileResponse);
		when(assetRenderedFileEntity.getIndexref()).thenReturn(indexLayer);

		ar = buildFromEntities(Set.of(fileMetadataEntity), Set.of(assetRenderedFileEntity));

		assertThat(ar.index())
				.hasSize(1)
				.containsKey(indexLayer);

		final var response = ar.index().get(indexLayer);
		assertThat(response.index()).isEqualByComparingTo(indexLayer);
		assertThat(response.fileMetadatas()).containsOnly(keyValueMetadataResponse);
		assertThat(response.rendered()).containsOnly(renderedFileResponse);

		verify(fileMetadataEntity, times(1)).toKeyValueMetadataResponse();
		verify(fileMetadataEntity, times(1)).getLayer();
		verify(assetRenderedFileEntity, times(1)).toRenderedReponse();
		verify(assetRenderedFileEntity, times(1)).getIndexref();
	}

}
