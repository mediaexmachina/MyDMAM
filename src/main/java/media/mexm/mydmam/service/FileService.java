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
package media.mexm.mydmam.service;

import java.util.List;
import java.util.Set;

import media.mexm.mydmam.dto.StorageCategory;
import media.mexm.mydmam.dto.StorageStateClass;
import media.mexm.mydmam.entity.FileEntity;

public interface FileService {

	List<FileEntity> resolveHashPaths(Set<String> hashPaths,
									  Set<StorageCategory> limitCategory,
									  Set<StorageStateClass> limitStorageClasses,
									  String realmName,
									  boolean recursive);

}
