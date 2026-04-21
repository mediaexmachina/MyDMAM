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

import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.App;
import media.mexm.mydmam.component.AboutInstance;
import media.mexm.mydmam.entity.InstanceEntity;

@Repository
@Slf4j
public class InstanceDaoImpl implements InstanceDao {

    @Autowired
    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    AboutInstance aboutInstance;
    @Autowired
    InstanceRepository instanceRepository;

    @Override
    @Transactional(REQUIRES_NEW)
    public InstanceEntity getSelfInstance() {
        final var name = aboutInstance.getInstanceName();
        final var host = aboutInstance.getHostName();
        final var pid = aboutInstance.getPid();
        final var startDate = App.START_DATE;

        final var lastEntry = entityManager.createQuery("""
                SELECT e
                FROM InstanceEntity e
                WHERE e.name = :name
                """, InstanceEntity.class)
                .setParameter("name", name)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (lastEntry != null) {
            if (lastEntry.getHost().equals(host) == false) {
                lastEntry.setHost(host);
            }
            if (lastEntry.getLastPid().equals(pid) == false) {
                lastEntry.setLastPid(pid);
            }
            final var startDateTs = new Timestamp(startDate);
            if (equalsTimestamps(lastEntry.getLastStartDate(), startDateTs) == false) {
                lastEntry.setLastStartDate(startDateTs);
            }
            return lastEntry;
        }

        return instanceRepository.saveAndFlush(new InstanceEntity(name, host, pid, startDate));
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public void updatePresenceInstance(final int id) {
        entityManager.createQuery("""
                UPDATE InstanceEntity e
                SET e.lastPresenceDate = CURRENT_TIMESTAMP()
                WHERE e.id = :id
                """)
                .setParameter("id", id)
                .executeUpdate();
    }

}
