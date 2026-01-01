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

import java.util.List;

import media.mexm.mydmam.repository.FileSort;

public record FileResponse(String realm,
						   String storage,
						   FileItemResponse currentItem,
						   String path,
						   String parentHashPath,
						   int listSize,
						   int skipCount,
						   int total,
						   FileSort sort,
						   List<FileItemResponse> list) {
}
