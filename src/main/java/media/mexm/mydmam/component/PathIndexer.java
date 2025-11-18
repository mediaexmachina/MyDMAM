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

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.PathIndexingConf;
import media.mexm.mydmam.pathindexing.RealmStorageFolderActivity;
import media.mexm.mydmam.service.PathIndexerService;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.watchfolder.Watchfolders;

@Slf4j
@Component
public class PathIndexer {

	@Autowired
	JobKitEngine jobKitEngine;
	@Autowired
	PathIndexerService pathIndexerService;
	@Autowired
	MyDMAMConfigurationProperties configuration;

	@Nullable
	private PathIndexingConf pathIndexingConf;
	private Map<RealmStorageFolderActivity, Watchfolders> watchfolders = Map.of();

	@PostConstruct
	public void starts() {
		watchfolders = pathIndexerService.makeWatchfolders();
		pathIndexingConf = configuration.pathindexing();
		watchfolders.values().forEach(Watchfolders::startScans);
	}

	@PreDestroy
	public void ends() {
		watchfolders.values().forEach(Watchfolders::stopScans);
	}

	public void scanNow(final String realm, final String storage) {
		watchfolders.entrySet()
				.stream()
				.filter(w -> {
					final var folderActivity = w.getKey();
					return folderActivity.realmName().equals(realm)
						   && folderActivity.storageName().equals(storage);
				})
				.findFirst()
				.ifPresent(entry -> {
					final var spoolEvents = pathIndexingConf.getSpoolEvents();

					jobKitEngine.runOneShot(
							"Manual scan for " + realm + ":" + storage,
							spoolEvents,
							0,
							() -> {
								final var startTime = System.currentTimeMillis();
								final var result = entry.getValue().manualScan();
								final var duration = Duration.ofMillis(System.currentTimeMillis() - startTime);
								if (result.isEmpty()) {
									return;
								}
								final var resultEntry = result.entrySet().stream().findFirst().get();
								entry.getKey().onAfterScan(resultEntry.getKey(), duration, resultEntry.getValue());
							},
							e -> Optional.ofNullable(e)
									.ifPresent(ee -> log.error("Can't manually scan Watchfoler on {}:{}",
											realm, storage, ee)));
				});
	}

}
