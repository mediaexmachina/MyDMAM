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

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.repository.InstanceDao;
import tv.hd3g.jobkit.engine.BackgroundService;
import tv.hd3g.jobkit.engine.JobKitEngine;

@Slf4j
@Component
public class UpdateDbPresenceInstance implements InternalService {

    private final InstanceDao instanceDao;
    private final JobKitEngine jobKit;
    private final MyDMAMConfigurationProperties configuration;
    private BackgroundService service;

    public UpdateDbPresenceInstance(@Autowired final InstanceDao instanceDao,
                                    @Autowired final JobKitEngine jobKit,
                                    @Autowired final MyDMAMConfigurationProperties configuration) {
        this.instanceDao = instanceDao;
        this.jobKit = jobKit;
        this.configuration = configuration;
    }

    @Override
    public void internalServiceStart() throws Exception {
        final int instanceId = instanceDao.getSelfInstance().getId();
        final var spoolName = configuration.env().spoolEvents();

        service = jobKit.startService("Update instance ref in db", spoolName, ofHours(1),
                () -> {
                    log.debug("Update db presence for this instance");
                    instanceDao.updatePresenceInstance(instanceId);
                }, nothing);
    }

    @Override
    public String getInternalServiceName() {
        return getClass().getSimpleName();
    }

    @Override
    public void internalServiceStop() throws Exception {
        Optional.ofNullable(service).ifPresent(BackgroundService::disable);
    }
}
