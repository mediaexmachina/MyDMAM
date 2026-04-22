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

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.PendingActivityJob;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.InstanceEntity;
import media.mexm.mydmam.entity.PendingActivityEntity;

@Repository
@Slf4j
public class PendingActivityDaoImpl implements PendingActivityDao {

    private static final String FILE_ID = "file_id";

    @Autowired
    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    InstanceDao instanceDao;
    @Autowired
    FileRepository fileRepository;
    @Autowired
    PendingActivityRepository pendingActivityRepository;
    @Autowired
    MyDMAMConfigurationProperties conf;

    @Override
    @Transactional(REQUIRES_NEW)
    public void declateActivities(final List<PendingActivityJob> allActivitiesJobs,
                                  final Optional<InstanceEntity> oInstance) {
        final var toAdd = allActivitiesJobs.stream()
                .map(a -> new PendingActivityEntity(
                        a.activityHandler(),
                        a.eventType(),
                        a.previousHandlersJson(),
                        a.file(),
                        oInstance))
                .toList();

        pendingActivityRepository.saveAllAndFlush(toAdd);
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public void endsActivity(final FileEntity file, final ActivityHandler activityHandler) {
        final var doneActivities = entityManager.createQuery("""
                SELECT pa FROM PendingActivityEntity pa
                WHERE pa.file.id = :file_id
                AND pa.handlerName = :handlerName
                """, PendingActivityEntity.class)
                .setParameter(FILE_ID, file.getId())
                .setParameter("handlerName", activityHandler.getHandlerName())
                .getResultList();

        if (doneActivities.isEmpty()) {
            log.warn("Can't found activities \"{}\" for {}", activityHandler.getHandlerName(), file);
        } else {
            log.debug("Remove activities {}", doneActivities);
            pendingActivityRepository.deleteAll(doneActivities);
        }

    }

    @Override
    @Transactional(REQUIRES_NEW)
    public boolean haveDeclaredActivity(final FileEntity file, final ActivityHandler activityHandler) {
        final var haveActivities = entityManager.createQuery("""
                SELECT COUNT(pa) FROM PendingActivityEntity pa
                WHERE pa.file.id = :file_id
                AND pa.handlerName = :handlerName
                """, Long.class)
                .setParameter(FILE_ID, file.getId())
                .setParameter("handlerName", activityHandler.getHandlerName())
                .getSingleResult()
                .intValue();

        return haveActivities > 0;
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public boolean havePendingActivities(final FileEntity file) {
        final var haveActivities = entityManager.createQuery("""
                SELECT COUNT(pa) FROM PendingActivityEntity pa
                WHERE pa.file.id = :file_id
                """, Long.class)
                .setParameter(FILE_ID, file.getId())
                .getSingleResult()
                .intValue();
        return haveActivities > 0;
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public Map<FileEntity, Set<PendingActivityEntity>> restartCurrentInstancePendingActivities(final boolean firstBoot) {
        final var instance = instanceDao.getSelfInstance();
        final var olderThan = new Timestamp(System.currentTimeMillis()
                                            - conf.env().pendingActivityMaxAgeGraceRestart().toMillis());

        @SuppressWarnings("rawtypes")
        final List<Map> lostedPendingActivitiesRawList;
        if (firstBoot) {
            lostedPendingActivitiesRawList = entityManager.createQuery("""
                    SELECT new map(pa.file as kFileEntity, pa as kPendingActivityEntity)
                    FROM PendingActivityEntity pa
                    WHERE pa.instance = :instance
                    """, Map.class)
                    .setParameter("instance", instance)
                    .getResultList();
        } else {
            lostedPendingActivitiesRawList = entityManager.createQuery("""
                    SELECT new map(pa.f as kFileEntity, pa as kPendingActivityEntity)
                    FROM PendingActivityEntity pa
                    WHERE pa.instance = :instance
                          AND pa.updated < :olderThan
                    """, Map.class)
                    .setParameter("instance", instance)
                    .setParameter("olderThan", olderThan)
                    .getResultList();

        }

        if (lostedPendingActivitiesRawList.isEmpty()) {
            return Map.of();
        }

        final var lostedPendingActivities = lostedPendingActivitiesRawList.stream()
                .map(f -> (PendingActivityEntity) f.get("kPendingActivityEntity"))
                .toList();

        entityManager.createQuery("""
                UPDATE PendingActivityEntity pa
                SET pa.updated = CURRENT_TIMESTAMP()
                WHERE pa IN :lostedPendingActivities
                """)
                .setParameter("lostedPendingActivities", lostedPendingActivities)
                .executeUpdate();

        return lostedPendingActivitiesRawList.stream()
                .collect(groupingBy(f -> (FileEntity) f.get("kFileEntity"),
                        HashMap::new,
                        mapping(f -> (PendingActivityEntity) f.get("kPendingActivityEntity"),
                                toSet())));
    }

}
