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
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static java.util.stream.Stream.concat;
import static media.mexm.mydmam.component.InternalObjectMapper.TYPE_LIST_STRING;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.PendingActivityJob;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.component.AboutInstance;
import media.mexm.mydmam.component.InternalObjectMapper;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.PendingActivityEntity;
import media.mexm.mydmam.repository.FileDao;
import media.mexm.mydmam.repository.FileRepository;
import media.mexm.mydmam.repository.PendingActivityDao;
import media.mexm.mydmam.tools.FileEntityConsumer;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
@Service
public class PendingActivityServiceImpl implements PendingActivityService {

	private static final String LOG_QUEUE_RUN_FOR_ON = "Queue run for: \"{}\", on {}.";

	@Autowired
	PendingActivityDao pendingActivityDao;
	@Autowired
	FileRepository fileRepository;
	@Autowired
	FileDao fileDao;
	@Autowired
	JobKitEngine jobKitEngine;
	@Autowired
	List<ActivityHandler> activityHandlers;
	@Autowired
	MediaAssetService mediaAssetService;
	@Autowired
	MyDMAMConfigurationProperties configuration;
	@Autowired
	InternalObjectMapper internalObjectMapper;
	@Autowired
	AboutInstance aboutInstance;

	private void runAssetsActivities(final RealmConf realm,
									 final ActivityEventType eventType,
									 final List<MediaAsset> assets) {
		assets.forEach(asset -> {
			final var selectedHandlers = activityHandlers.stream()
					.filter(activityHandler -> activityHandler.canHandle(asset, eventType))
					.toList();

			final var previousHandlers = synchronizedSet(new HashSet<String>(getHandlersNames(selectedHandlers)));

			selectedHandlers.forEach(activityHandler -> {
				log.trace(LOG_QUEUE_RUN_FOR_ON, asset, activityHandler.getHandlerName());

				pendingActivityDao.declateActivity(
						asset.getHashPath(),
						activityHandler,
						eventType,
						internalObjectMapper.writeValueAsString(previousHandlers),
						aboutInstance.getInstanceName(),
						aboutInstance.getPid());

				jobKitEngine.runOneShot(new PendingActivityJob(
						realm.spoolProcessAsset(),
						asset,
						activityHandler,
						eventType,
						previousHandlers,
						pendingActivityDao,
						this));
			});

			if (selectedHandlers.isEmpty()) {
				log.trace("Nothing to run for: \"{}\", previousHandlers={}, activityHandlers count={}",
						asset, previousHandlers, activityHandlers.size());
			}
		});
	}

	@Override
	public void startsActivities(final String realmName,
								 final String storageName,
								 final RealmConf realm,
								 final Set<? extends FileAttributesReference> files,
								 final ActivityEventType eventType) {
		final var assets = files.stream()
				.filter(not(FileAttributesReference::isDirectory))
				.map(f -> mediaAssetService.getFromWatchfolder(realmName, storageName, f, mediaAssetService))
				.toList();
		if (assets.isEmpty()) {
			return;
		}

		runAssetsActivities(realm, eventType, assets);

	}

	@Override
	@Transactional
	public void startsActivities(final String realmName,
								 final Set<String> hashPaths,
								 final boolean recursive,
								 final ActivityEventType eventType) {
		if (hashPaths.isEmpty()) {
			throw new IllegalArgumentException("No hashPaths to run activities");
		}

		final var realm = configuration.getRealmByName(realmName)
				.orElseThrow(() -> new IllegalArgumentException("Invalid/not configured realm " + realmName));

		if (recursive) {
			log.info("Starts activities \"{}\" on realm \"{}\": {}", eventType, realmName, hashPaths);
		} else {
			log.info("Starts activities \"{}\" on realm \"{}\", recursive from {}", eventType, realmName, hashPaths);
		}

		final var availableStorageNames = realm.getOnlineDASStorageNames();
		if (availableStorageNames.isEmpty()) {
			throw new IllegalStateException(
					"No available storages for this selected realm \"" + realmName + "\" to run activities");
		}
		log.debug("availableStorageNames={}", availableStorageNames);

		final var filteredFileList = fileRepository.getByHashPath(hashPaths)
				.stream()
				.filter(f -> {
					if (f.getRealm().equals(realmName) == false) {
						throw new IllegalStateException("Invalid realm (\"" + realmName + "\") for file " + f);
					}
					return true;
				})
				.filter(f -> availableStorageNames.contains(f.getStorage()))
				.toList();

		log.trace("filteredFileList={}", filteredFileList);

		final var assetsFiles = filteredFileList.stream()
				.filter(not(FileEntity::isDirectory))
				.map(f -> mediaAssetService.getFromFileEntry(f, mediaAssetService))
				.toList();

		log.trace("assets count = {}", assetsFiles.size());
		if (assetsFiles.isEmpty() == false) {
			runAssetsActivities(realm, eventType, assetsFiles);
		}

		final var parentHashPaths = filteredFileList.stream()
				.filter(FileEntity::isDirectory)
				.map(FileEntity::getHashPath)
				.collect(toUnmodifiableSet());

		log.trace("parentHashPaths={}", parentHashPaths);
		if (parentHashPaths.isEmpty()) {
			return;
		}

		final var subDirAssets = new ArrayList<MediaAsset>();

		final FileEntityConsumer onFile = fileEntry -> {
			if (fileEntry.isDirectory()) {
				return;
			}
			subDirAssets.add(mediaAssetService.getFromFileEntry(fileEntry, mediaAssetService));
		};

		fileDao.getByParentHashPath(realmName, parentHashPaths, onFile, recursive);

		log.trace("subDir assets count = {}", subDirAssets.size());
		if (subDirAssets.isEmpty()) {
			return;
		}

		runAssetsActivities(realm, eventType, subDirAssets);
	}

	static Set<String> getHandlersNames(final List<ActivityHandler> selectedHandlers) {
		return selectedHandlers.stream()
				.map(ActivityHandler::getHandlerName)
				.distinct()
				.collect(toUnmodifiableSet());
	}

	@Override
	public void continueAssetActivity(final PendingActivityJob pendingActivityJob) {
		final var previousHandlers = pendingActivityJob.previousHandlers();
		final var asset = pendingActivityJob.asset();
		final var eventType = pendingActivityJob.eventType();

		final var selectedHandlers = activityHandlers.stream()
				.filter(handler -> previousHandlers.contains(handler.getHandlerName()) == false)
				.filter(handler -> handler.canHandle(asset, eventType))
				.toList();

		previousHandlers.addAll(getHandlersNames(selectedHandlers));

		selectedHandlers.forEach(activityHandler -> {
			log.trace(LOG_QUEUE_RUN_FOR_ON, asset, activityHandler.getHandlerName());

			pendingActivityDao.declateActivity(
					asset.getHashPath(),
					activityHandler,
					eventType,
					internalObjectMapper.writeValueAsString(previousHandlers),
					aboutInstance.getInstanceName(),
					aboutInstance.getPid());

			jobKitEngine.runOneShot(pendingActivityJob.evolve(activityHandler));
		});

	}

	@Override
	@Transactional
	public void restartPendingActivities() {
		final var allPendingList = pendingActivityDao.getPendingActivities(
				configuration.getRealmNames(),
				aboutInstance.getInstanceName());
		if (allPendingList.isEmpty()) {
			return;
		}

		final var unknownActivityHanders = new HashSet<String>();

		Stream.of(ActivityEventType.values())
				.forEach(eventType -> allPendingList.stream()
						.filter(p -> p.getEventType().equalsIgnoreCase(eventType.name()))
						.collect(groupingBy(PendingActivityEntity::getFile,
								HashMap::new,
								toSet()))
						.forEach((file, pendings) -> {
							final var previousHandlersFromDb = pendings.stream()
									.map(PendingActivityEntity::getPreviousHandlers)
									.map(ph -> internalObjectMapper.readValue(ph, TYPE_LIST_STRING))
									.flatMap(List::stream);
							final var actualHandlersFromDb = pendings.stream()
									.map(PendingActivityEntity::getHandlerName);
							final var previousHandlers = synchronizedSet(
									new HashSet<String>(
											concat(
													previousHandlersFromDb,
													actualHandlersFromDb)
															.distinct()
															.toList()));

							final var asset = mediaAssetService.getFromFileEntry(file, mediaAssetService);
							final var spoolName = configuration.getRealmByName(file.getRealm())
									.map(RealmConf::spoolProcessAsset)
									.orElseThrow(() -> new IllegalStateException(
											"Can't found realm=" + file.getRealm()));

							pendings.forEach(pending -> {
								final var oActivityHander = activityHandlers.stream()
										.filter(ah -> ah.getHandlerName().equalsIgnoreCase(pending
												.getHandlerName()))
										.findFirst();
								if (oActivityHander.isEmpty()) {
									unknownActivityHanders.add(pending.getHandlerName());
									return;
								}

								pendingActivityDao.resetPendingActivity(
										pending,
										aboutInstance.getInstanceName(),
										aboutInstance.getPid());

								log.trace(LOG_QUEUE_RUN_FOR_ON, asset, pending.getHandlerName());

								jobKitEngine.runOneShot(new PendingActivityJob(
										spoolName,
										asset,
										oActivityHander.get(),
										eventType,
										previousHandlers,
										pendingActivityDao,
										this));
							});
						}));

		if (unknownActivityHanders.isEmpty() == false) {
			log.warn("Can't found ActivityHander with this name(s): {}", unknownActivityHanders);
		}
	}

	@Override
	public void cleanupFiles(final String realmName,
							 final String storageName,
							 final RealmConf realm,
							 final Set<? extends FileAttributesReference> losted) {
		losted.forEach(file -> mediaAssetService.purgeAssetArtefacts(realmName, storageName, file));
	}

}
