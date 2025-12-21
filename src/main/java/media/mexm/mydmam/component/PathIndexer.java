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

import static java.util.stream.Collectors.toUnmodifiableMap;

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
	}

	public void startScans() {
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
					final var spoolEvents = configuration.infra().spoolEvents();

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

		final var infra = configuration.infra();
		if (infra == null) {
			return Map.of();
		}
		final var spoolEvents = infra.spoolEvents();

		return Optional.ofNullable(infra.realms())
				.orElse(Map.of())
				.entrySet()
				.stream()
				.flatMap(entry -> {
					final var realmName = entry.getKey().name();
					final var realmConf = entry.getValue();

					return realmConf.storages().entrySet().stream()
							.filter(storageConf -> {
								final var storage = storageConf.getValue();
								return storage.noScans() == false;
							})
							.map(storageConf -> {
								final var storageName = storageConf.getKey().name();
								final var storage = storageConf.getValue();

								final var spoolScans = storage.spoolScans();

								final var timeBetweenScans = Optional.ofNullable(storage.timeBetweenScans())
										.or(() -> Optional.ofNullable(realmConf.timeBetweenScans()))
										.orElse(infra.timeBetweenScans());

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
										List.of(storage.makeObservedFolder(realmName, storageName)),
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
