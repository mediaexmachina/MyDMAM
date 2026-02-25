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
package media.mexm.mydmam.asset;

import java.util.Optional;
import java.util.Set;

import media.mexm.mydmam.entity.FileMetadataEntity;

public interface AccessFileMetadataEntryTrait {

	Set<FileMetadataEntity> getMetadatas();

	default Optional<String> getMetadataValue(final String classifier, final String key, final int layer) {
		return getMetadatas().stream()
				.filter(m -> m.getClassifier().equals(classifier))
				.filter(m -> m.getLayer() == layer)
				.filter(m -> m.getKey().equals(key))
				.findFirst()
				.map(FileMetadataEntity::getValue);
	}

	/**
	 * With layer == 0
	 */
	default Optional<String> getMetadataValue(final String classifier, final String key) {
		return getMetadataValue(classifier, key, 0);
	}

}
