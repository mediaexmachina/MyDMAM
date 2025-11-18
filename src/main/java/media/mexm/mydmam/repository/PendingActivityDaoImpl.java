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

import static java.util.stream.Collectors.joining;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

	@Value("${mydmamConsts.instancename:''}")
	String instanceName;

	@Override
	@Transactional
	public void declateActivities(final Set<String> hashPathItems, final String taskContext, final String pendingTask) {
		final var host = getHostName();
		final var pid = getPid();

		entityManager.createQuery("""
				SELECT f FROM FileEntity f
				WHERE f.hashPath IN :hashPath
				""", FileEntity.class)
				.setParameter("hashPath", hashPathItems)
				.getResultStream()
				.map(f -> new PendingActivityEntity(
						f,
						taskContext,
						pendingTask,
						host,
						pid))
				.forEach(pa -> entityManager.persist(pa));
	}

	FileEntity getByHashPath(final String hashPath) {
		return Optional.ofNullable(fileRepository.getByHashPath(hashPath))
				.orElseThrow(() -> new IllegalStateException("Can't found file with hashPath " + hashPath));
	}

	@Override
	@Transactional
	public void updateActivity(final String hashPath, final String taskContext, final String pendingTask) {
		final var file = getByHashPath(hashPath);

		final var activeTasks = file.getPendingActivities().stream()
				.filter(pa -> pa.getTaskContext().equalsIgnoreCase(taskContext))
				.toList();

		if (activeTasks.isEmpty()) {
			log.warn("You should declate an activity before update it. hashPath={}, taskContext={}, pendingTask={}",
					hashPath, taskContext, pendingTask);
			declateActivities(Set.of(hashPath), taskContext, pendingTask);
			return;
		}

		if (activeTasks.size() > 1) {
			log.warn(
					"More than one pendingTask active for hashPath={}, taskContext={}, actives=\"{}\". Delete the extra ones.",
					hashPath,
					taskContext,
					activeTasks.stream().map(PendingActivityEntity::getPendingTask).collect(joining(", ")));
			activeTasks.stream()
					.skip(1)
					.forEach(pendingActivityRepository::delete);
		}
		final var task = activeTasks.get(0);

		final var host = getHostName();
		final var pid = getPid();

		if (task.getWorkerHost().equals(host) == false || task.getWorkerPid() != pid) {
			log.warn("Task stollen from another host {}", task);
		}

		task.setPendingTask(pendingTask, host, pid);
	}

	@Override
	@Transactional
	public void endsActivity(final String hashPath, final String taskContext) {
		Optional.ofNullable(fileRepository.getByHashPath(hashPath))
				.stream()
				.map(FileEntity::getPendingActivities)
				.flatMap(Set::stream)
				.filter(pa -> pa.getTaskContext().equalsIgnoreCase(taskContext))
				.forEach(pendingActivityRepository::delete);
	}

	@Override
	@Transactional
	public List<PendingActivityEntity> getPendingActivities(final Duration maxAge, final Set<String> realms) {
		final var host = getHostName();
		final var olderThan = new Timestamp(System.currentTimeMillis() - maxAge.toMillis());

		return entityManager.createQuery("""
				SELECT pa FROM PendingActivityEntity pa
				WHERE (f.workerHost = :workerHost OR f.updated < :olderThan)
				AND pa.file.realm IN :realms
				""", PendingActivityEntity.class)
				.setParameter("workerHost", host)
				.setParameter("olderThan", olderThan)
				.setParameter("realms", realms)
				.getResultList();
	}

	@Override
	@Transactional
	public void resetPendingActivities(final Collection<PendingActivityEntity> pendingActivities) {
		final var host = getHostName();
		final var pid = getPid();

		pendingActivities.forEach(pa -> pa.setPendingTask(pa.getPendingTask(), host, pid));
		pendingActivityRepository.saveAllAndFlush(pendingActivities);
	}

	String getHostName() {
		try {
			if (instanceName != null && instanceName.equals("") == false) {
				return InetAddress.getLocalHost().getHostName() + "#" + instanceName;
			}
			return InetAddress.getLocalHost().getHostName();
		} catch (final UnknownHostException e) {
			throw new IllegalStateException("Can't get hostname", e);
		}
	}

	static long getPid() {
		return ProcessHandle.current().pid();
	}

}
