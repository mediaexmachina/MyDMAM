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

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.PendingActivityEntity;

@Repository
@Slf4j
public class PendingActivityDaoImpl implements PendingActivityDao {

	@Autowired
	@PersistenceContext
	EntityManager entityManager;

	@Autowired
	FileRepository fileRepository;
	@Autowired
	PendingActivityRepository pendingActivityRepository;

	@Value("${mydmamConsts.pendingActivityMaxAgeGraceRestart:24h}")
	Duration maxAgeGraceRestart;

	@Override
	@Transactional
	public void declateActivity(final String hashPathItem,
								final ActivityHandler activityHandler,
								final ActivityEventType eventType,
								final String previousHandlers,
								final String hostName,
								final long pid) {
		entityManager.createQuery("""
				SELECT f FROM FileEntity f
				WHERE f.hashPath = :hashPath
				""", FileEntity.class)
				.setParameter("hashPath", hashPathItem)
				.getResultStream()
				.map(f -> new PendingActivityEntity(
						activityHandler,
						eventType,
						previousHandlers,
						f,
						hostName,
						pid))
				.forEach(pa -> entityManager.persist(pa));
	}

	FileEntity getByHashPath(final String hashPath) {
		return Optional.ofNullable(fileRepository.getByHashPath(hashPath))
				.orElseThrow(() -> new IllegalStateException("Can't found file with hashPath " + hashPath));
	}

	@Override
	@Transactional
	public void endsActivity(final String hashPath, final ActivityHandler activityHandler) {
		Optional.ofNullable(fileRepository.getByHashPath(hashPath))
				.stream()
				.map(FileEntity::getPendingActivities)
				.flatMap(Set::stream)
				.filter(pa -> pa.getHandlerName().equalsIgnoreCase(activityHandler.getHandlerName()))
				.forEach(pendingActivityRepository::delete);
	}

	@Override
	@Transactional
	public List<PendingActivityEntity> getPendingActivities(final Set<String> realms, final String hostName) {
		final var olderThan = new Timestamp(System.currentTimeMillis() - maxAgeGraceRestart.toMillis());

		return entityManager.createQuery("""
				SELECT pa FROM PendingActivityEntity pa
				WHERE (pa.workerHost = :workerHost OR pa.updated < :olderThan)
				AND pa.file.realm IN :realms
				""", PendingActivityEntity.class)
				.setParameter("workerHost", hostName)
				.setParameter("olderThan", olderThan)
				.setParameter("realms", realms)
				.getResultList();
	}

	@Override
	@Transactional
	public void resetPendingActivity(final PendingActivityEntity pendingActivity,
									 final String hostName,
									 final long pid) {
		pendingActivity.reset(hostName, pid);
		pendingActivityRepository.saveAndFlush(pendingActivity);
	}

	@Override
	@Transactional
	public void deletePendingActivities(final Collection<PendingActivityEntity> pendingActivities) {
		pendingActivityRepository.deleteAll(pendingActivities);
	}

}
