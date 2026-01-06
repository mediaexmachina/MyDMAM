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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.tools.FileEntityConsumer;

@Repository
@Slf4j
public class FileDaoImpl implements FileDao {

	private static final String PARENT_HASH_PATH_PARAM = "parentHashPath";
	private static final String REALM_PARAM = "realm";
	@Autowired
	@PersistenceContext
	EntityManager entityManager;

	@Override
	@Transactional
	public List<FileEntity> getByParentHashPath(final String parentHashPath,
												final int from,
												final int size,
												final Optional<FileSort> oSort) {
		final var criteriaBuilder = entityManager.getCriteriaBuilder();
		final var criteriaQuery = criteriaBuilder.createQuery(FileEntity.class);
		final var root = criteriaQuery.from(FileEntity.class);
		criteriaQuery.select(root);
		criteriaQuery.where(criteriaBuilder.equal(root.get(PARENT_HASH_PATH_PARAM), parentHashPath));
		oSort.ifPresent(sort -> criteriaQuery.orderBy(sort.makeOrderBy(root, criteriaBuilder)));

		return entityManager.createQuery(criteriaQuery)
				.setFirstResult(from)
				.setMaxResults(size)
				.getResultList();
	}

	@Override
	@Transactional
	public void getByParentHashPath(final String realm,
									final Set<String> parentHashPaths,
									final FileEntityConsumer onFile,
									final boolean recursive) {
		final var currentDirContent = new ArrayList<FileEntity>();
		final var parentHashPathToSeek = new ArrayList<>(parentHashPaths);

		final var maxDeep = recursive ? 100 : 1;
		for (var deep = 0; deep < maxDeep; deep++) {
			/**
			 * Should be optimized with WITH RECURSIVE Hibernate
			 */
			currentDirContent.addAll(entityManager.createQuery("""
					SELECT f FROM FileEntity f
					WHERE f.parentHashPath IN :parentHashPath
					AND f.realm = :realm
					""", FileEntity.class)
					.setParameter(PARENT_HASH_PATH_PARAM, parentHashPathToSeek)
					.setParameter(REALM_PARAM, realm)
					.getResultList());

			parentHashPathToSeek.clear();
			parentHashPathToSeek.addAll(currentDirContent.stream()
					.filter(FileEntity::isDirectory)
					.map(FileEntity::getHashPath)
					.toList());

			currentDirContent.forEach(onFile::accept);
			currentDirContent.clear();
		}

	}

	@Override
	@Transactional
	public int countParentHashPathItems(final String realm, final String storage, final String parentHashPath) {
		return entityManager.createQuery("""
				SELECT COUNT(f) FROM FileEntity f
				WHERE f.parentHashPath = :parentHashPath
				AND f.realm = :realm
				AND f.storage = :storage
				""", Long.class)
				.setParameter(PARENT_HASH_PATH_PARAM, parentHashPath)
				.setParameter(REALM_PARAM, realm)
				.setParameter("storage", storage)
				.getSingleResult()
				.intValue();
	}

	@Override
	@Transactional
	public void getAllFromRealm(final String realm, final FileEntityConsumer onFile) {
		entityManager.createQuery("""
				SELECT f FROM FileEntity f
				WHERE f.realm = :realm
				""", FileEntity.class)
				.setParameter(REALM_PARAM, realm)
				.getResultStream()
				.forEach(onFile);
	}

}
