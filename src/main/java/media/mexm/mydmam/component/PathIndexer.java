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

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static media.mexm.mydmam.configuration.PathIndexingConf.correctName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.pathindexing.RealmStorageFolderActivity;
import media.mexm.mydmam.pathindexing.RealmStorageWatchedFilesDb;
import media.mexm.mydmam.service.PathIndexerService;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.watchfolder.Watchfolders;

@Slf4j
@Component
public class PathIndexer {

	private final JobKitEngine jobKitEngine;
	private final PathIndexerService pathIndexerService;
	private final MyDMAMConfigurationProperties configuration;
	private final Map<RealmStorageFolderActivity, Watchfolders> watchfolders;

	public PathIndexer(@Autowired final JobKitEngine jobKitEngine,
					   @Autowired final PathIndexerService pathIndexerService,
					   @Autowired final MyDMAMConfigurationProperties configuration) {
		this.jobKitEngine = jobKitEngine;
		this.configuration = configuration;
		this.pathIndexerService = pathIndexerService;
		watchfolders = makeWatchfolders();
		watchfolders.values().forEach(Watchfolders::startScans);
	}

	Map<RealmStorageFolderActivity, Watchfolders> getWatchfolders() {
		return watchfolders;
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
					final var spoolEvents = configuration.pathindexing().getSpoolEvents();

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

	private Map<RealmStorageFolderActivity, Watchfolders> makeWatchfolders() {
		record KV(RealmStorageFolderActivity key, Watchfolders value) {
		}

		final var pathIndexingConf = configuration.pathindexing();
		if (pathIndexingConf == null) {
			return Map.of();
		}
		final var spoolEvents = pathIndexingConf.getSpoolEvents();

		return Optional.ofNullable(pathIndexingConf.realms())
				.orElse(Map.of())
				.entrySet()
				.stream()
				.flatMap(entry -> {
					final var realmName = correctName(entry.getKey(), "realm name");
					final var realmConf = entry.getValue();

					return realmConf.storagesStream()
							.filter(storageConf -> {
								final var storage = storageConf.getValue();
								final var scan = storage.scan();
								return scan.isDisabled() == false;
							})
							.map(storageConf -> {
								final var storageName = correctName(storageConf.getKey(), "storage name");
								final var storage = storageConf.getValue();

								final var spoolScans = Optional.ofNullable(storage.spoolScans())
										.or(() -> Optional.ofNullable(realmConf.spoolScans()))
										.or(() -> Optional.ofNullable(pathIndexingConf.spoolScans()))
										.filter(not(String::isEmpty))
										.orElse("pathindexing");

								final var timeBetweenScans = Optional.ofNullable(storage.timeBetweenScans())
										.filter(Duration::isPositive)
										.or(() -> Optional.ofNullable(realmConf.timeBetweenScans()))
										.filter(Duration::isPositive)
										.or(() -> Optional.ofNullable(pathIndexingConf.timeBetweenScans()))
										.filter(Duration::isPositive)
										.orElse(Duration.ofHours(1));

								log.debug(
										"Prepare Watchfolder for {}:{} with timeBetweenScans={}, spoolScans={} spoolEvents={}",
										realmName, storageName, timeBetweenScans, spoolScans, spoolEvents);

								final var folderActivity = new RealmStorageFolderActivity(
										pathIndexerService,
										realmName,
										realmConf,
										storageName,
										storage);

								return new KV(folderActivity, new Watchfolders(
										List.of(storage.scan()),
										folderActivity,
										timeBetweenScans,
										jobKitEngine,
										spoolScans,
										spoolEvents,
										() -> new RealmStorageWatchedFilesDb(
												pathIndexerService,
												realmName,
												storageName,
												storage)));
							});
				})
				.collect(toUnmodifiableMap(KV::key, KV::value));
	}

}
