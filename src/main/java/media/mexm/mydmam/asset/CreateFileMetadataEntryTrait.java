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

import java.util.Map;

import media.mexm.mydmam.activity.ActivityHandler;

public interface CreateFileMetadataEntryTrait {

	void createFileMetadataEntry(String originHandler,
								 String classifier,
								 int layer,
								 String key,
								 String value);

	default void createFileMetadataEntry(final ActivityHandler hander,
										 final String classifier,
										 final int layer,
										 final String key,
										 final String value) {
		if (hander instanceof final MetadataExtractorHandler mtdHander) {
			createFileMetadataEntry(mtdHander.getMetadataOriginName(), classifier, layer, key, value);
		} else {
			createFileMetadataEntry(hander.getHandlerName(), classifier, layer, key, value);
		}
	}

	default void createFileMetadataEntry(final String originHandler,
										 final String classifier,
										 final int layer,
										 final Map<String, String> entries) {
		entries.entrySet()
				.forEach(entry -> {
					createFileMetadataEntry(originHandler,
							classifier,
							layer,
							entry.getKey(),
							entry.getValue());
				});
	}

	default void createFileMetadataEntry(final ActivityHandler hander,
										 final String classifier,
										 final int layer,
										 final Map<String, String> entries) {
		entries.entrySet()
				.forEach(entry -> {
					createFileMetadataEntry(hander,
							classifier,
							layer,
							entry.getKey(),
							entry.getValue());
				});
	}

}
