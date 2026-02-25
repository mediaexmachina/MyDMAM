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

import media.mexm.mydmam.activity.ActivityHandler;

public interface MimeTypeTrait extends CreateFileMetadataEntryTrait, AccessFileMetadataEntryTrait {

	String MTD_FILE_FORMAT_CLASSIFIER = "file-format";
	String MTD_MIME_TYPE_KEY = "mime-type";

	default Optional<String> getMimeType() {
		return getMetadataValue(MTD_FILE_FORMAT_CLASSIFIER, MTD_MIME_TYPE_KEY);
	}

	default void setMimeType(final ActivityHandler hander, final String mimeType) {
		createFileMetadataEntry(hander, MTD_FILE_FORMAT_CLASSIFIER, 0, MTD_MIME_TYPE_KEY, mimeType);
	}

}
