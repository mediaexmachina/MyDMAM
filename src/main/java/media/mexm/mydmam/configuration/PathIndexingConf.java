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
package media.mexm.mydmam.configuration;

import static java.text.Normalizer.normalize;
import static java.text.Normalizer.Form.NFKD;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static media.mexm.mydmam.entity.FileEntity.MAX_NAME_SIZE;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.component.PathIndexer;
import media.mexm.mydmam.pathindexing.RealmStorageFolderActivity;
import media.mexm.mydmam.pathindexing.RealmStorageWatchedFilesDb;
import tv.hd3g.jobkit.watchfolder.Watchfolders;

@Validated
@Slf4j
public record PathIndexingConf(@Valid Map<String, PathIndexingRealm> realms,
							   Duration timeBetweenScans,
							   String spoolScans,
							   String spoolEvents) {

	private static final Pattern REPLACE_NORMALIZED = Pattern.compile("\\p{M}");
	private static final Pattern REMOVE_NON_VALID_CHARS = Pattern.compile("[^a-z^A-Z^0-9^\\-^\\_]+"); // NOSONAR S5869

	static String correctName(final String rawname, final String fieldName) {
		Objects.requireNonNull(rawname, "You must setup " + fieldName);

		final var normalized = REPLACE_NORMALIZED.matcher(normalize(rawname.trim(), NFKD)).replaceAll("");
		final var name = REMOVE_NON_VALID_CHARS.matcher(normalized).replaceAll("_");

		if (name.isEmpty()) {
			throw new IllegalArgumentException("You must setup " + fieldName);
		}

		return name.substring(0, Math.min(name.length(), MAX_NAME_SIZE));
	}

	public String getSpoolEvents() {
		return Optional.ofNullable(spoolEvents)
				.filter(t -> t.isEmpty() == false)
				.orElse("pathindexing");
	}

	public Map<RealmStorageFolderActivity, Watchfolders> makeWatchfolders(final PathIndexer indexer) {
		record KV(RealmStorageFolderActivity key, Watchfolders value) {
		}

		final var spoolEvents = getSpoolEvents();

		return Optional.ofNullable(realms)
				.orElse(Map.of())
				.entrySet()
				.stream()
				.flatMap(entry -> {
					final var realmName = correctName(entry.getKey(), "realm name");
					final var realmConf = entry.getValue();

					return realmConf.storagesStream()
							.map(storageConf -> {
								final var storageName = correctName(storageConf.getKey(), "storage name");
								final var storage = storageConf.getValue();

								final var spoolScans = Optional.ofNullable(storage.spoolScans())
										.or(() -> Optional.ofNullable(realmConf.spoolScans()))
										.or(() -> Optional.ofNullable(this.spoolScans))
										.filter(not(String::isEmpty))
										.orElse("pathindexing");

								final var timeBetweenScans = Optional.ofNullable(storage.timeBetweenScans())
										.filter(Duration::isPositive)
										.or(() -> Optional.ofNullable(realmConf.timeBetweenScans()))
										.filter(Duration::isPositive)
										.or(() -> Optional.ofNullable(this.timeBetweenScans))
										.filter(Duration::isPositive)
										.orElse(Duration.ofHours(1));

								log.debug(
										"Prepare Watchfolder for {}:{} with timeBetweenScans={}, spoolScans={} spoolEvents={}",
										realmName, storageName, timeBetweenScans, spoolScans, spoolEvents);

								final var folderActivity = new RealmStorageFolderActivity(
										indexer, realmName, realmConf, storageName, storage);

								return new KV(folderActivity, new Watchfolders(
										List.of(storage.scan()),
										folderActivity,
										timeBetweenScans,
										indexer.getJobKitEngine(),
										spoolScans,
										spoolEvents,
										() -> new RealmStorageWatchedFilesDb(
												indexer, realmName, storageName, storage)));
							});
				})
				.collect(toUnmodifiableMap(KV::key, KV::value));
	}

}
