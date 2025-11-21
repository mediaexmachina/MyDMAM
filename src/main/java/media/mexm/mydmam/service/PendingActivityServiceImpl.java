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
package media.mexm.mydmam.service;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.FileIOActivityHander;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.repository.PendingActivityDao;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
@Service
public class PendingActivityServiceImpl { // TODO interface

	@Autowired
	PendingActivityDao pendingActivityDao;
	@Autowired
	JobKitEngine jobKitEngine;
	@Autowired
	List<FileIOActivityHander> fileIOActivityHanders;
	@Autowired
	MediaAssetService mediaAssetService;
	@Value("${mydmamConsts.spoolProcessAsset:processasset}")
	String spoolProcessAsset;

	public void onFoundFiles(final String realmName,
							 final String storageName,
							 final PathIndexingRealm realm,
							 final PathIndexingStorage storage,
							 final Set<CachedFileAttributes> files) {
		final var assets = files.stream()
				.filter(not(CachedFileAttributes::isDirectory))
				.map(f -> mediaAssetService.getFromWatchfolder(realmName, storageName, f))
				.toList();
		if (assets.isEmpty()) {
			return;
		}

		final var hashPathItems = assets.stream().map(MediaAsset::getHashPath).collect(toUnmodifiableSet());
		pendingActivityDao.declateActivities(hashPathItems, "on-found-file", "dispatch");

		final var spool = Optional.ofNullable(realm.spoolProcessAsset()).orElse(spoolProcessAsset);

		assets.forEach(asset -> {
			jobKitEngine.runOneShot("Dispatch founded file: " + asset.getName(), spool, 0, () -> {
				log.debug("Start to dispatch founded file: \"{}\" ({}:{})", asset.getName(), realmName, storageName);

				// TODO ...
				final var handlerToRunList = fileIOActivityHanders.stream()
						.map(ah -> ah.onFoundNewAsset(asset))
						.flatMap(List::stream)
						.toList();

			}, e -> {
				if (e != null) {
					log.error("Can't dispatch founded file: \"{}\" ({}:{})",
							asset.getName(), realmName, storageName, e);
				} else {
					log.trace("End dispatch founded file: \"{}\" ({}:{})", asset.getName(), realmName, storageName);
				}
			});

		});

		// activityHanders
		//
		// TODO
	}

	public void onUpdateFiles(final String realmName,
							  final String storageName,
							  final PathIndexingRealm realm,
							  final PathIndexingStorage storage,
							  final Set<CachedFileAttributes> files) {
		// TODO
	}

	public void onLostedFiles(final String realmName,
							  final String storageName,
							  final PathIndexingRealm realm,
							  final PathIndexingStorage storage,
							  final Set<? extends FileAttributesReference> files) {
		// TODO
	}

	/*
	TODO manage DAO
	void declateActivities(Set<String> hashPathItems, String taskContext, String pendingTask);
	void updateActivity(String hashPath, String taskContext, String pendingTask);
	void endsActivity(String hashPath, String taskContext);
	List<PendingActivityEntity> getPendingActivities(Duration maxAge, Set<String> realms);
	 * */

}
