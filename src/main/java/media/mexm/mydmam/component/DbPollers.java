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
package media.mexm.mydmam.component;

import static java.time.Duration.ofHours;
import static tv.hd3g.jobkit.engine.RunnableWithException.nothing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.repository.InstanceDao;
import media.mexm.mydmam.service.PendingActivityService;
import tv.hd3g.jobkit.engine.BackgroundService;
import tv.hd3g.jobkit.engine.JobKitEngine;

@Slf4j
@Component
public class DbPollers implements InternalService {

    private final InstanceDao instanceDao;
    private final PendingActivityService pendingActivityService;
    private final JobKitEngine jobKit;
    private final MyDMAMConfigurationProperties configuration;
    private final AtomicBoolean firstRestartPendingActivities;
    private final List<BackgroundService> services;

    public DbPollers(@Autowired final InstanceDao instanceDao,
                     @Autowired final PendingActivityService pendingActivityService,
                     @Autowired final JobKitEngine jobKit,
                     @Autowired final MyDMAMConfigurationProperties configuration) {
        this.instanceDao = instanceDao;
        this.pendingActivityService = pendingActivityService;
        this.jobKit = jobKit;
        this.configuration = configuration;
        firstRestartPendingActivities = new AtomicBoolean(true);
        services = new ArrayList<>();
    }

    @Override
    public void internalServiceStart() throws Exception {
        final int instanceId = instanceDao.getSelfInstance().getId();
        final var spoolName = configuration.env().spoolEvents();

        services.add(jobKit.startService("Update instance ref in db", spoolName, ofHours(1),
                () -> {
                    log.debug("Update db presence for this instance");
                    instanceDao.updatePresenceInstance(instanceId);
                }, nothing));

        final var pendingActivityMaxAgeGraceRestart = configuration.env().pendingActivityMaxAgeGraceRestart();
        services.add(jobKit.startService("Check and restart losted pending activities", spoolName,
                pendingActivityMaxAgeGraceRestart,
                () -> pendingActivityService.restartPendingActivities(firstRestartPendingActivities.getAndSet(false)),
                nothing));
    }

    @Override
    public String getInternalServiceName() {
        return getClass().getSimpleName();
    }

    @Override
    public void internalServiceStop() throws Exception {
        services.forEach(BackgroundService::disable);
    }
}
