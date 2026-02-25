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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;

public record AssetResponse(Map<Integer, AssetResponseIndex> index) {

	public static AssetResponse buildFromEntities(final Set<FileMetadataEntity> fileMetadatas,
												  final Set<AssetRenderedFileEntity> rendered) {

		final var metadatas = Optional.ofNullable(fileMetadatas)
				.stream()
				.flatMap(Set::stream)
				.collect(groupingBy(
						FileMetadataEntity::getLayer,
						HashMap::new,
						mapping(FileMetadataEntity::toKeyValueMetadataResponse, toUnmodifiableList())));

		final var renderedReponse = Optional.ofNullable(rendered)
				.stream()
				.flatMap(Set::stream)
				.collect(groupingBy(
						AssetRenderedFileEntity::getIndexref,
						HashMap::new,
						mapping(AssetRenderedFileEntity::toRenderedReponse, toUnmodifiableList())));

		final var payload = Stream.concat(
				metadatas.keySet().stream(),
				renderedReponse.keySet().stream())
				.distinct()
				.sorted()
				.map(index -> new AssetResponseIndex(
						index,
						metadatas.getOrDefault(index, List.of()),
						renderedReponse.getOrDefault(index, List.of())))
				.collect(toUnmodifiableMap(AssetResponseIndex::index, identity()));

		return new AssetResponse(payload);
	}

	public static record AssetResponseIndex(Integer index,
											List<KeyValueMetadataResponse> fileMetadatas,
											List<RenderedFileResponse> rendered) {
	}

}
