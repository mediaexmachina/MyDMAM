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
package media.mexm.mydmam.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import media.mexm.mydmam.entity.AssetRenderedFileEntity;

public interface AssetRenderedFileRepository extends JpaRepository<AssetRenderedFileEntity, Long> {

	@Query("SELECT arf FROM AssetRenderedFileEntity arf WHERE arf.file.id = :fileId AND arf.etag IN :etags")
	Set<AssetRenderedFileEntity> getRenderedForFileByEtags(int fileId, Set<Long> etags);

	@Query("""
			SELECT arf
			FROM AssetRenderedFileEntity arf
			WHERE arf.file.hashPath = :hashPath
			AND arf.file.realm = :realm
			AND arf.name = :name
			AND arf.indexref = :indexref
			""")
	AssetRenderedFileEntity getRenderedFile(String hashPath, String realm, String name, int indexref);

}
