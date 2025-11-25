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

	@Autowired
	@PersistenceContext
	EntityManager entityManager;

	@Override
	@Transactional
	public List<FileEntity> getByParentHashPath(final String parentHashPath, final int from, final int size) {
		return entityManager.createQuery("""
				SELECT f FROM FileEntity f
				WHERE f.parentHashPath = :parentHashPath
				""", FileEntity.class)
				.setParameter("parentHashPath", parentHashPath)
				.setFirstResult(from)
				.setMaxResults(size)
				.getResultList();
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
				.setParameter("parentHashPath", parentHashPath)
				.setParameter("realm", realm)
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
				.setParameter("realm", realm)
				.getResultStream()
				.forEach(onFile);
	}

}
