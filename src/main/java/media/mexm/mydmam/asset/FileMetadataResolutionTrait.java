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

import media.mexm.mydmam.activity.ActivityHandler;

public interface FileMetadataResolutionTrait extends CreateFileMetadataEntryTrait, AccessFileMetadataEntryTrait {

	String MTD_TECHNICAL_CLASSIFIER = "technical";
	String MTD_WIDTH_KEY = "width";
	String MTD_HEIGHT_KEY = "height";

	/**
	 * @return -1 if not resolution
	 */
	default int getWidth() {
		return getMetadataValue(MTD_TECHNICAL_CLASSIFIER, MTD_WIDTH_KEY)
				.map(Integer::parseInt)
				.orElse(-1);
	}

	/**
	 * @return -1 if not resolution
	 */
	default int getHeight() {
		return getMetadataValue(MTD_TECHNICAL_CLASSIFIER, MTD_HEIGHT_KEY)
				.map(Integer::parseInt)
				.orElse(-1);
	}

	/**
	 * Don't set anything if width/height is less than 1
	 */
	default void setResolution(final ActivityHandler handler, final int width, final int height) {
		if (width < 1 || height < 1) {
			return;
		}
		createFileMetadataEntry(handler, MTD_TECHNICAL_CLASSIFIER, 0, MTD_WIDTH_KEY, String.valueOf(width));
		createFileMetadataEntry(handler, MTD_TECHNICAL_CLASSIFIER, 0, MTD_HEIGHT_KEY, String.valueOf(height));
	}

}
