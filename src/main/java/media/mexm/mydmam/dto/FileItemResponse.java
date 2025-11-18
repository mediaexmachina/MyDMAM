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
 * Copyright (C) Media ex Machina 2025
 *
 */
package media.mexm.mydmam.dto;

import static org.apache.commons.io.FilenameUtils.getName;

import media.mexm.mydmam.entity.FileEntity;

public record FileItemResponse(boolean directory,
							   String name,
							   String hashPath,
							   long modified,
							   long length,
							   boolean justDetected) {

	public static FileItemResponse createFromEntity(final FileEntity entity,
													final String checkRealm,
													final String checkStorage) {
		if (entity.getRealm().equals(checkRealm) == false) {
			throw new IllegalArgumentException(
					"Invalid realm check, expected=" + checkRealm + ", founded=" + entity.getRealm());
		} else if (entity.getStorage().equals(checkStorage) == false) {
			throw new IllegalArgumentException(
					"Invalid storage check (for realm " + checkRealm + "), expected="
											   + checkStorage + ", founded=" + entity.getStorage());
		}

		return new FileItemResponse(
				entity.isDirectory(),
				getName(entity.getPath()),
				entity.getHashPath(),
				entity.getModified().getTime(),
				entity.isDirectory() ? -1 : entity.getLength(),
				(entity.isDirectory() || entity.isWatchMarkedAsDone()) == false);
	}

}
