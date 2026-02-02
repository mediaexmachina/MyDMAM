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

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Optional;
import java.util.Set;

import media.mexm.mydmam.component.InternalObjectMapper;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.AssetSummaryEntity;

public record FileMetadatasReponse(FileMetadatasSummaryResponse summary,
								   Set<FileMetadatasRenderedReponse> rendered) {

	public FileMetadatasReponse(final AssetSummaryEntity assetSummaryEntity,
								final Set<AssetRenderedFileEntity> renderedFiles,
								final InternalObjectMapper objectMapper) {
		final var summaryResponse = Optional.ofNullable(assetSummaryEntity)
				.map(s -> new FileMetadatasSummaryResponse(s, objectMapper))
				.orElse(null);

		final var renderedReponse = Optional.ofNullable(renderedFiles)
				.stream()
				.flatMap(Set::stream)
				.map(AssetRenderedFileEntity::toRenderedReponse)
				.collect(toUnmodifiableSet());

		this(summaryResponse, renderedReponse);
	}

}
