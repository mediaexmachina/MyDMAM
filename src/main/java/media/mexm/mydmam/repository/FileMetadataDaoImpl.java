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

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;

@Repository
@Slf4j
public class FileMetadataDaoImpl implements FileMetadataDao {

    @Autowired
    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    FileMetadataRepository fileMetadataRepository;

    @Override
    @Transactional
    public Map<String, Set<FileMetadataEntity>> getFileMetadatasByFileIds(final Collection<Integer> fileIds,
                                                                          final String realm) {
        return entityManager.createQuery("""
                SELECT new map(DISTINCT(f.hashPath) AS hashPath, fm AS fileMetadata)
                FROM FileEntity f
                LEFT JOIN FileMetadataEntity fm ON fm.file = f
                WHERE
                    f.id IN :ids
                    AND f.realm = :realm
                    AND fm IS NOT NULL
                """, Map.class)
                .setParameter("ids", fileIds)
                .setParameter("realm", realm)
                .getResultStream()
                .collect(groupingBy(
                        f -> (String) f.get("hashPath"),
                        HashMap::new,
                        mapping(f -> (FileMetadataEntity) f.get("fileMetadata"), toSet())));
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public void addUpdateEntry(final FileEntity file, final FileMetadataEntity item) {
        entityManager.createQuery("""
                DELETE FROM FileMetadataEntity fm
                WHERE fm.file = :file
                AND fm.entryCrc = :entryCrc
                """)
                .setParameter("file", file)
                .setParameter("entryCrc", item.getEntryCrc())
                .executeUpdate();

        fileMetadataRepository.saveAndFlush(item);
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public void addUpdateEntries(final FileEntity file, final Collection<FileMetadataEntity> items) {
        final var allEntryCrcs = items.stream()
                .map(FileMetadataEntity::getEntryCrc)
                .distinct()
                .toList();

        entityManager.createQuery("""
                DELETE FROM FileMetadataEntity fm
                WHERE fm.file = :file
                AND fm.entryCrc IN :allEntryCrcs
                """)
                .setParameter("file", file)
                .setParameter("allEntryCrcs", allEntryCrcs)
                .executeUpdate();

        fileMetadataRepository.saveAllAndFlush(items);
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public Optional<String> getMetadataValue(final FileEntity fileEntity,
                                             final int layer,
                                             final String classifier,
                                             final String key) {
        return entityManager.createQuery("""
                SELECT fm.value
                FROM FileMetadataEntity fm
                WHERE
                    fm.file = :fileEntity
                    AND fm.classifier = :classifier
                    AND fm.layer = :layer
                    AND fm.key = :key
                """, String.class)
                .setParameter("fileEntity", fileEntity)
                .setParameter("classifier", classifier)
                .setParameter("layer", layer)
                .setParameter("key", key)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

}
