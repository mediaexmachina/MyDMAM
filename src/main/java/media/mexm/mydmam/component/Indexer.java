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
package media.mexm.mydmam.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.indexer.RealmIndexer;
import media.mexm.mydmam.repository.FileDao;
import media.mexm.mydmam.service.MediaAssetService;
import tv.hd3g.jobkit.engine.JobKitEngine;

@Component
@Slf4j
public class Indexer implements DisposableBean {

    private final Map<String, RealmIndexer> indexerByRealmName = new HashMap<>();

    @Autowired
    MyDMAMConfigurationProperties conf;
    @Autowired
    FileDao fileDao;
    @Autowired
    JobKitEngine jobKit;

    public void init() throws IOException {
        if (conf.realms() == null) {
            return;
        }

        for (final var entry : Optional.ofNullable(conf.realms())
                .orElse(Map.of())
                .entrySet()) {
            final var realmName = entry.getKey().name();

            final var workingDirectory = entry.getValue().workingDirectory();
            if (workingDirectory == null) {
                continue;
            }

            log.info("Prepare indexer for realm={}, on {}",
                    realmName, workingDirectory.getAbsolutePath());
            final var realmIndexer = new RealmIndexer(
                    realmName,
                    workingDirectory,
                    conf.env().explainSearchResults(),
                    entry.getValue().delayedSync());
            indexerByRealmName.put(realmName, realmIndexer);
        }
    }

    public Optional<RealmIndexer> getIndexerByRealm(final String realm) {
        if (indexerByRealmName.containsKey(realm) == false) {
            return Optional.empty();
        }
        return Optional.ofNullable(indexerByRealmName.get(realm));
    }

    @Override
    public void destroy() {
        indexerByRealmName.values().forEach(RealmIndexer::close);
        indexerByRealmName.clear();
    }

    public void reset(final String spoolName, final MediaAssetService mediaAssetService) {
        jobKit.runOneShot("Reset all indexes", spoolName, 0, () -> {
            indexerByRealmName.forEach((realm, indexer) -> {
                log.info("Start to reset indexes on realm {}", realm);
                indexer.reset();
                fileDao.getAllFromRealm(realm, mediaAssetService::updateIndexer);
            });
            log.info("All indexes are now reseted");
        }, e -> {
            if (e != null) {
                log.error("Can't reset indexes", e);
            }
        });

    }
}
