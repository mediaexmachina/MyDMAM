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
package media.mexm.mydmam.repository;

import java.util.List;
import java.util.Optional;

import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.tools.FileEntityConsumer;

public interface FileDao {

	List<FileEntity> getByParentHashPath(String parentHashPath, int from, int size, Optional<FileSort> sort);

	int countParentHashPathItems(String realm, String storage, String parentHashPath);

	void getAllFromRealm(String realm, FileEntityConsumer onFile);
}
