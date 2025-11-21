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
import static media.mexm.mydmam.activity.ActivityEventType.COLD_RESTART;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHander;
import media.mexm.mydmam.activity.PendingActivityJob;
import media.mexm.mydmam.activity.PendingDispatchActivityJob;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import media.mexm.mydmam.repository.PendingActivityDao;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
@Service
public class PendingActivityServiceImpl implements PendingActivityService { // TODO test

	private final PendingActivityDao pendingActivityDao;
	private final JobKitEngine jobKitEngine;
	private final List<ActivityHander> activityHanders;
	private final MediaAssetService mediaAssetService;
	private final String spoolProcessAsset;
	private final MyDMAMConfigurationProperties configuration;

	public PendingActivityServiceImpl(@Autowired final PendingActivityDao pendingActivityDao,
									  @Autowired final JobKitEngine jobKitEngine,
									  @Autowired final List<ActivityHander> activityHanders,
									  @Autowired final MediaAssetService mediaAssetService,
									  @Autowired final MyDMAMConfigurationProperties configuration,
									  @Value("${mydmamConsts.spoolProcessAsset:processasset}") final String spoolProcessAsset) {
		this.pendingActivityDao = pendingActivityDao;
		this.jobKitEngine = jobKitEngine;
		this.activityHanders = activityHanders;
		this.mediaAssetService = mediaAssetService;
		this.configuration = configuration;
		this.spoolProcessAsset = spoolProcessAsset;
	}

	void coldStart() {
		final var pending = pendingActivityDao.getPendingActivities(configuration.getRealmNames());
		if (pending.isEmpty()) {
			return;
		}

		// TODO pending => files, pending by file
		// TODO pendingActivityDao.resetPendingActivities(pending by file)
		// TODO if ActivityEventType not cold -> PendingDispatchActivityJob (actual code)
		// TODO else if ActivityEventType cold + dao.resetPendingActivities cold + dispatchAssetActivities
		// TODO else dao.delete(pending by file) + dao.declateActivities cold + dispatchAssetActivities

		pending.forEach(entry -> {
			final var taskContext = entry.getTaskContext();
			final var pendingTask = entry.getPendingTask(); // TODO in log message
			final var file = entry.getFile();
			final var realmName = file.getRealm();
			final var storageName = file.getStorage();
			final var hashPath = file.getHashPath();
			final var realm = configuration.getRealmByName(realmName).orElseThrow();
			final var spoolName = Optional.ofNullable(realm.spoolProcessAsset()).orElse(spoolProcessAsset);
			final var asset = mediaAssetService.getFromFileEntry(realmName, storageName, file);

			final var oEventType = Stream.of(ActivityEventType.values())
					.filter(eventType -> eventType.toString().equalsIgnoreCase(taskContext))
					.findFirst();
			if (oEventType.isPresent()) {
				final var eventType = oEventType.get();
				final var eventTypeName = eventType.toString();
				pendingActivityDao.updateActivity(hashPath, eventTypeName, "dispatch");

				jobKitEngine.runOneShot(
						new PendingDispatchActivityJob(
								realmName,
								storageName,
								spoolName,
								asset,
								eventType,
								pendingActivityDao,
								this));
			} else {
				// TODO delete tasks
				// pendingActivityDao.delete(pendingActivities)
				dispatchAssetActivities(realmName,
						storageName,
						asset,
						spoolName,
						COLD_RESTART,
						synchronizedSet(new HashSet<>()));
			}
		});

	}

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

		assets.forEach(asset -> jobKitEngine.runOneShot(
				new PendingDispatchActivityJob(
						realmName,
						storageName,
						spoolName,
						asset,
						eventType,
						pendingActivityDao,
						this)));

	}

	@Override
	public void dispatchAssetActivities(final String realmName,
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
			pendingActivityDao.declateActivities(Set.of(asset.getHashPath()), ah.getTaskContextName(), "queue");

			jobKitEngine.runOneShot(new PendingActivityJob(
					realmName,
					storageName,
					spoolName,
					asset,
					ah,
					eventType,
					previousHandlers,
					pendingActivityDao,
					this));
		});

	}

	@Override
	public void cleanupFiles(final String realmName,
							 final String storageName,
							 final PathIndexingRealm realm,
							 final Set<? extends FileAttributesReference> losted) {
		losted.forEach(file -> mediaAssetService.purgeAssetArtefacts(realmName, storageName, file));
	}

}
