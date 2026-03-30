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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.RelativePathProvider;

@Repository
@Slf4j
public class AssetRenderedFileDaoImpl implements AssetRenderedFileDao {

    @Autowired
    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional
    public Map<String, Set<AssetRenderedFileEntity>> getRenderedFilesByFileId(final Collection<Integer> ids,
                                                                              final String realm) {
        return entityManager.createQuery("""
                SELECT new map(f.hashPath AS hashPath, arf AS assetRenderedFile)
                FROM FileEntity f
                LEFT JOIN AssetRenderedFileEntity arf ON arf.file = f
                WHERE
                    f.id IN :ids
                    AND f.realm = :realm
                    AND arf IS NOT NULL
                """, Map.class)
                .setParameter("ids", ids)
                .setParameter("realm", realm)
                .getResultStream()
                .collect(groupingBy(
                        f -> (String) f.get("hashPath"),
                        HashMap::new,
                        mapping(f -> (AssetRenderedFileEntity) f.get("assetRenderedFile"), toSet())));
    }

    @Override
    @Transactional
    public Map<String, Set<RelativePathProvider>> deleteRenderedFilesByFileId(final Collection<Integer> ids) {
        final var relativePathsByRealmToDelete = entityManager.createQuery("""
                SELECT new map(f.realm AS realm, arf AS assetRenderedFile)
                FROM FileEntity f
                LEFT JOIN AssetRenderedFileEntity arf ON arf.file = f
                WHERE arf.file.id IN :fileIds
                """, Map.class)
                .setParameter("fileIds", ids)
                .getResultStream()
                .collect(groupingBy(
                        f -> (String) f.get("realm"),
                        HashMap::new,
                        mapping(f -> ((RelativePathProvider) f.get("assetRenderedFile")),
                                toSet())));

        entityManager.createQuery("""
                DELETE FROM AssetRenderedFileEntity arf
                WHERE arf.file.id IN :fileIds
                """)
                .setParameter("fileIds", ids)
                .executeUpdate();

        return relativePathsByRealmToDelete;
    }

}
