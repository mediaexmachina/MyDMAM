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

import static java.util.function.Predicate.not;
import static media.mexm.mydmam.component.InternalObjectMapper.TYPE_MAP_STRING_STRING;

import java.util.Map;
import java.util.Optional;

import media.mexm.mydmam.component.InternalObjectMapper;
import media.mexm.mydmam.entity.AssetSummaryEntity;

public record FileMetadatasReponse(String mimeType, Map<String, String> specifications) {

	public static FileMetadatasReponse createFromAssetSummaryEntity(final AssetSummaryEntity assetSummaryEntity,
																	final InternalObjectMapper objectMapper) {
		final var specifications = Optional.ofNullable(assetSummaryEntity.getSpecifications())
				.filter(not(String::isEmpty))
				.map(s -> objectMapper.readValue(s, TYPE_MAP_STRING_STRING))
				.orElse(Map.of());

		return new FileMetadatasReponse(
				assetSummaryEntity.getMimeType(),
				specifications);
	}

}
