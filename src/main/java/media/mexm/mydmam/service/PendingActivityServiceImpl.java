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

import static java.util.Collections.synchronizedSet;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHander;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import media.mexm.mydmam.repository.PendingActivityDao;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
@Service
public class PendingActivityServiceImpl implements PendingActivityService { // TODO test

	@Autowired
	PendingActivityDao pendingActivityDao;
	@Autowired
	JobKitEngine jobKitEngine;
	@Autowired
	List<ActivityHander> activityHanders;
	@Autowired
	MediaAssetService mediaAssetService;
	@Value("${mydmamConsts.spoolProcessAsset:processasset}")
	String spoolProcessAsset;

	@Override
	public void applyActivities(final String realmName,
								final String storageName,
								final PathIndexingRealm realm,
								final Set<? extends FileAttributesReference> files,
								final ActivityEventType eventType) {
		final var assets = files.stream()
				.filter(not(FileAttributesReference::isDirectory))
				.map(f -> mediaAssetService.getFromWatchfolder(realmName, storageName, f))
				.toList();
		if (assets.isEmpty()) {
			return;
		}
		final var hashPathItems = assets.stream().map(MediaAsset::getHashPath).collect(toUnmodifiableSet());
		pendingActivityDao.declateActivities(hashPathItems, eventType.toString(), "dispatch");
		final var spoolName = Optional.ofNullable(realm.spoolProcessAsset()).orElse(spoolProcessAsset);

		assets.forEach(asset -> {
			jobKitEngine.runOneShot("Dispatch founded file: " + asset.getName(), spoolName, 0, () -> {
				log.debug("Start to dispatch founded file: \"{}\" ({}:{})", asset.getName(), realmName, storageName);
				dispatchAssetActivities(
						realmName,
						storageName,
						asset, spoolName,
						eventType,
						synchronizedSet(new HashSet<>()));
			}, e -> {
				if (e != null) {
					log.error("Can't dispatch founded file: \"{}\" ({}:{})",
							asset.getName(), realmName, storageName, e);
				} else {
					log.trace("End dispatch founded file: \"{}\" ({}:{})",
							asset.getName(), realmName, storageName);
				}
				pendingActivityDao.endsActivity(asset.getHashPath(), eventType.toString());
			});
		});

	}

	void dispatchAssetActivities(final String realmName,
								 final String storageName,
								 final MediaAsset asset,
								 final String spoolName,
								 final ActivityEventType eventType,
								 final Set<Class<?>> previousHandlers) {
		final var handlers = activityHanders.stream()
				.filter(ah -> ah.canHandle(asset, eventType))
				.toList();

		if (handlers.isEmpty()) {
			return;
		}

		final var actualHandlerClassList = handlers.stream()
				.map(ActivityHander::getClass)
				.toList();
		final var doubleRunHandlerClassList = previousHandlers.stream()
				.filter(actualHandlerClassList::contains)
				.toList();

		if (doubleRunHandlerClassList.isEmpty() == false) {
			/**
			 * Loop detected!
			 */
			log.warn("ActivityHander loop detected for {} [context: \"{}\" ({}:{})]",
					doubleRunHandlerClassList, asset.getName(), realmName, storageName);
			return;
		}
		previousHandlers.addAll(actualHandlerClassList);

		final var taskContextNameList = handlers.stream()
				.map(ActivityHander::getTaskContextName)
				.collect(joining(", "));

		log.trace("Queue run for: \"{}\" ({}:{}), on {}.",
				asset.getName(), realmName, storageName, taskContextNameList);

		handlers.forEach(ah -> {
			final var taskContextName = ah.getTaskContextName();
			final var jobName = "Run media asset activity " + taskContextName + " on file: " + asset.getName();

			pendingActivityDao.declateActivities(Set.of(asset.getHashPath()), taskContextName, "queue");

			jobKitEngine.runOneShot(jobName, spoolName, 0, () -> {
				log.debug("Start media asset activity on file: \"{}\" ({}:{})",
						asset.getName(), realmName, storageName);
				pendingActivityDao.declateActivities(Set.of(asset.getHashPath()), taskContextName, "starts");
				ah.handle(asset, eventType);
			}, e -> {
				pendingActivityDao.endsActivity(asset.getHashPath(), taskContextName);
				if (e != null) {
					log.error("Can't run media asset activity on file: \"{}\" ({}:{})",
							asset.getName(), realmName, storageName, e);
				} else {
					log.trace("Ends media asset activity on file: \"{}\" ({}:{})",
							asset.getName(), realmName, storageName);
					dispatchAssetActivities(realmName, storageName, asset, spoolName, eventType, previousHandlers);
				}
			});

		});

	}

	/*
	TODO manage DAO + on start behavior
	void updateActivity(String hashPath, String taskContext, String pendingTask);
	List<PendingActivityEntity> getPendingActivities(Duration maxAge, Set<String> realms);
	 * */

}
