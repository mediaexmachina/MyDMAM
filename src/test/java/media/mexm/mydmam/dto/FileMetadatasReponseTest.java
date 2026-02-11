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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.AssetSummaryEntity;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class FileMetadatasReponseTest {

	@Mock
	AssetSummaryEntity assetSummaryEntity;
	@Mock
	AssetRenderedFileEntity renderedFiles;
	@Mock
	FileMetadatasRenderedReponse fileMetadatasRenderedReponse;

	@Fake
	String mimeType;

	FileMetadatasReponse fmr;

	@Test
	void test() {
		when(assetSummaryEntity.getMimeType())
				.thenReturn(mimeType);
		when(renderedFiles.toRenderedReponse())
				.thenReturn(fileMetadatasRenderedReponse);

		fmr = new FileMetadatasReponse(
				assetSummaryEntity,
				Set.of(renderedFiles));

		final var summary = fmr.summary();
		assertNotNull(summary);
		assertEquals(mimeType, summary.mimeType());

		final var rendered = fmr.rendered();
		assertNotNull(rendered);
		assertThat(rendered)
				.hasSize(1)
				.contains(fileMetadatasRenderedReponse);

		verify(assetSummaryEntity, times(1)).getMimeType();
		verify(renderedFiles, times(1)).toRenderedReponse();
	}

	@Test
	void testNotNull() {
		fmr = new FileMetadatasReponse((AssetSummaryEntity) null, (Set<AssetRenderedFileEntity>) null);

		final var summary = fmr.summary();
		assertNull(summary);

		final var rendered = fmr.rendered();
		assertThat(rendered).isEmpty();
	}

}
