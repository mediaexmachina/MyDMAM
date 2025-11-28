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

import static media.mexm.mydmam.configuration.PathIndexingConf.correctName;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.indexer.RealmIndexer;

@Component
@Slf4j
public class Indexer implements DisposableBean {

	private final Map<String, RealmIndexer> indexerByRealmName = new HashMap<>();

	@Autowired
	MyDMAMConfigurationProperties conf;
	@Value("${mydmamConsts.explainSearchResults:false}")
	boolean explainSearchResults;

	public void init() throws IOException {
		final var pathIndexing = conf.pathindexing();
		if (pathIndexing == null) {
			return;
		}

		for (final var entry : Optional.ofNullable(pathIndexing.realms())
				.orElse(Map.of())
				.entrySet()) {
			final var realmName = correctName(entry.getKey(), "realm name");

			final var oWorkingDirectory = entry.getValue().getValidWorkingDirectory(realmName);
			if (oWorkingDirectory.isEmpty()) {
				continue;
			}

			log.info("Prepare indexer for realm={}, on {}",
					realmName, oWorkingDirectory.get().getAbsolutePath());
			final var realmIndexer = new RealmIndexer(realmName, oWorkingDirectory.get(), explainSearchResults);
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
	public void destroy() throws Exception {
		indexerByRealmName.values().forEach(RealmIndexer::close);
	}
}
