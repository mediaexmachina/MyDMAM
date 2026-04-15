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
import static media.mexm.mydmam.tools.CompareTimestamps.equalsTimestamps;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.entity.ExternalExecCapabilityEntity;
import media.mexm.mydmam.entity.ExternalExecEntity;

@Repository
@Slf4j
public class ExternalExecDaoImpl implements ExternalExecDao {

    private static final String PLAYBOOK = "playbook";
    private static final String EXTERNAL_EXEC = "externalExec";

    @Autowired
    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    InstanceDao instanceDao;
    @Autowired
    ExternalExecRepository externalExecRepository;
    @Autowired
    ExternalExecCapabilityRepository externalExecCapabilityRepository;

    private ExternalExecEntity getExternalExecEntity(final String execName,
                                                     final File exec,
                                                     final long crc) {
        final var instance = instanceDao.getSelfInstance();
        final var currentExternalExec = entityManager.createQuery("""
                SELECT ee
                FROM ExternalExecEntity ee
                WHERE ee.instance = :instance
                AND ee.execName = :execName
                """, ExternalExecEntity.class)
                .setParameter("instance", instance)
                .setParameter("execName", execName)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);

        final var execPath = exec.getAbsolutePath();
        final var execLength = exec.length();
        final var execModified = exec.lastModified();

        if (currentExternalExec != null) {
            if (currentExternalExec.getExecPath().equals(execPath)
                && currentExternalExec.getExecLength().equals(execLength)
                && equalsTimestamps(currentExternalExec.getExecModified(), execModified)
                && currentExternalExec.getExecCrc().equals(crc)) {
                return currentExternalExec;
            } else {
                externalExecRepository.delete(currentExternalExec);
            }
        }

        return externalExecRepository.saveAndFlush(
                new ExternalExecEntity(
                        instance,
                        execName,
                        execPath,
                        execModified,
                        execLength,
                        crc));
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public void removeExec(final String execName) {
        final var instance = instanceDao.getSelfInstance();
        entityManager.createQuery("""
                DELETE FROM ExternalExecEntity ee
                WHERE ee.instance = :instance
                AND ee.execName = :execName
                """)
                .setParameter("instance", instance)
                .setParameter("execName", execName)
                .executeUpdate();
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public void addPlaybookResult(final String execName,
                                  final File exec,
                                  final String playbook,
                                  final boolean pass,
                                  final long crc) {
        final var externalExec = getExternalExecEntity(execName, exec, crc);

        entityManager.createQuery("""
                DELETE FROM ExternalExecCapabilityEntity eec
                WHERE eec.externalExec = :externalExec
                AND eec.playbook = :playbook
                """)
                .setParameter(EXTERNAL_EXEC, externalExec)
                .setParameter(PLAYBOOK, playbook)
                .executeUpdate();

        externalExecCapabilityRepository.saveAndFlush(
                new ExternalExecCapabilityEntity(
                        externalExec,
                        playbook,
                        pass));
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public Optional<Boolean> getPlaybookResult(final String execName,
                                               final File exec,
                                               final String playbook,
                                               final long crc) {
        final var externalExec = getExternalExecEntity(execName, exec, crc);

        return entityManager.createQuery("""
                SELECT eec.pass
                FROM ExternalExecCapabilityEntity eec
                WHERE eec.externalExec = :externalExec
                AND eec.playbook = :playbook
                """, Boolean.class)
                .setParameter(EXTERNAL_EXEC, externalExec)
                .setParameter(PLAYBOOK, playbook)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public Set<String> getAllPlaybookPass(final String execName, final File exec, final long crc) {
        final var externalExec = getExternalExecEntity(execName, exec, crc);

        return entityManager.createQuery("""
                SELECT eec.playbook
                FROM ExternalExecCapabilityEntity eec
                WHERE eec.externalExec = :externalExec
                AND eec.pass = true
                """, String.class)
                .setParameter(EXTERNAL_EXEC, externalExec)
                .getResultStream()
                .distinct()
                .collect(Collectors.toUnmodifiableSet());
    }

}
