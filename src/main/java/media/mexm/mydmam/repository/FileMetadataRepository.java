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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;

public interface FileMetadataRepository extends JpaRepository<FileMetadataEntity, Long> {

    @Query("""
            DELETE FROM FileMetadataEntity fm
            WHERE fm.file.id IN :fileIds
            """)
    @Modifying
    void deleteByFileId(Set<Integer> fileIds);

    @Query("""
            SELECT fm
            FROM FileMetadataEntity fm
            WHERE fm.file = :file
            """)
    Set<FileMetadataEntity> getByFile(FileEntity file);

}
