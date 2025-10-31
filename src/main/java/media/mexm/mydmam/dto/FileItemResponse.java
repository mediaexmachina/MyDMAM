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

import media.mexm.mydmam.entity.FileEntity;

public record FileItemResponse(boolean directory, String path, String hashPath, long modified, long length) {

	public static FileItemResponse createFromEntity(final FileEntity entity) {// TODO test
		return new FileItemResponse(
				entity.isDirectory(),
				entity.getPath(),
				entity.getHashPath(),
				entity.getModified().getTime(),
				entity.getLength());
	}

}
