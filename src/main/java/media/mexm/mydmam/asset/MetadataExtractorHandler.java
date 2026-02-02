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

import java.io.File;
import java.util.Optional;
import java.util.Set;

import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;

public interface MetadataExtractorHandler extends ActivityHandler {

	/**
	 * @return if empty list, manage nothing.
	 */
	Set<String> getManagedMimeTypes();

	default boolean canHandleMimeType(final MediaAsset asset) {
		return Optional.ofNullable(asset.getMimeType())
				.map(mimeType -> getManagedMimeTypes().contains(mimeType))
				.orElse(false);
	}

	default File makeWorkingFile(final String fileName,
								 final MediaAsset asset,
								 final RealmStorageConfiguredEnv storedOn) {
		return storedOn.realm().makeWorkingFile(
				asset.getFile().getId() + "-" + fileName,
				getClass());
	}

	Set<String> getProducedPreviewTypes();

	String getMetadataOriginName();

}
