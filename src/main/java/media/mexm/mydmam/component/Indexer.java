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
		final var pathIndexing = conf.pathindexing();
		if (pathIndexing == null) {
			return;
		}

		for (final var entry : Optional.ofNullable(pathIndexing.realms())
				.orElse(Map.of())
				.entrySet()) {
			final var realmName = entry.getKey().name();

			final var workingDirectory = entry.getValue().workingDirectory();
			if (workingDirectory == null) {
				continue;
			}

			log.info("Prepare indexer for realm={}, on {}",
					realmName, workingDirectory.getAbsolutePath());
			final var realmIndexer = new RealmIndexer(realmName, workingDirectory, conf.explainSearchResults());
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

	public void reset(final String spoolName) {
		jobKit.runOneShot("Reset all indexes", spoolName, 0, () -> {
			indexerByRealmName.forEach((realm, indexer) -> {
				log.info("Start to reset indexes on realm {}", realm);

				try (var session = indexer.reset(conf.resetBatchSizeIndexer())) {
					fileDao.getAllFromRealm(realm, session);
				} catch (final Exception e) {
					log.error("Can't run reset", e);
				}
			});
			log.info("All indexes are now reseted");
		}, e -> {
			if (e != null) {
				log.error("Can't reset indexes", e);
			}
		});

	}
}
