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
import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mydmam.component.InternalObjectMapper.TYPE_LIST_STRING;
import static media.mexm.mydmam.dto.StorageCategory.DAS;
import static media.mexm.mydmam.dto.StorageStateClass.ONLINE;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.component.InternalObjectMapper;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.PendingActivityEntity;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.repository.FileDao;
import media.mexm.mydmam.repository.FileRepository;
import media.mexm.mydmam.repository.PendingActivityDao;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
@Service
public class PendingActivityServiceImpl implements PendingActivityService {

	private static final String LOG_QUEUE_RUN_FOR_ON = "Queue run for: \"{}\", on {}.";

	@Autowired
	PendingActivityDao pendingActivityDao;
	@Autowired
	FileService fileService;
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
	@Autowired
	Indexer indexer;

	private boolean checkSupportedStorageStateClasses(final ActivityHandler activityHandler,
													  final RealmStorageConfiguredEnv confEnv) {
		final var supported = Optional.ofNullable(activityHandler.getSupportedStorageStateClasses())
				.orElse(Set.of());
		if (supported.isEmpty()) {
			return true;
		}
		return supported.contains(confEnv.storage().getStorageStateClass());
	}

	private void runAssetsActivities(final ActivityEventType eventType,
									 final List<MediaAsset> assets) {

		assets.forEach(asset -> {
			final var file = asset.getFile();
			if (file.isDirectory()) {
				throw new IllegalArgumentException("Can't run activities on directory (for " + file + ")");
			}
		});

		final var allActivitiesJobs = assets.stream()
				.flatMap(asset -> {
					final var file = asset.getFile();
					final var confEnv = configuration.getRealmAndStorage(file.getRealm(), file.getStorage());

					final var selectedHandlers = activityHandlers.stream()
							.filter(activityHandler -> checkSupportedStorageStateClasses(activityHandler, confEnv))
							.filter(activityHandler -> activityHandler.canHandle(asset, eventType, confEnv))
							.toList();

					final var oIndexer = indexer.getIndexerByRealm(confEnv.realmName());
					final var previousHandlers = synchronizedSet(new HashSet<String>(getHandlersNames(
							selectedHandlers)));

					if (selectedHandlers.isEmpty()) {
						log.trace("Nothing to run for: \"{}\", previousHandlers={}, activityHandlers count={}",
								asset, previousHandlers, activityHandlers.size());
					}

					return selectedHandlers.stream()
							.map(activityHandler -> new PendingActivityJob(
									confEnv,
									asset,
									activityHandler,
									eventType,
									previousHandlers,
									internalObjectMapper.writeValueAsString(previousHandlers),
									pendingActivityDao,
									this,
									oIndexer));
				})
				.toList();

		log.info("Prepare {} activity(ies) to run as {}", allActivitiesJobs.size(), eventType);

		pendingActivityDao.declateActivities(
				allActivitiesJobs,
				aboutInstance.getInstanceName(),
				aboutInstance.getPid());

		allActivitiesJobs.forEach(a -> {
			log.trace(LOG_QUEUE_RUN_FOR_ON, a.asset(), a.activityHandler().getHandlerName());
			jobKitEngine.runOneShot(a);
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

		runAssetsActivities(eventType, assets);

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

		if (recursive) {
			log.info("Starts activities \"{}\" on realm \"{}\": {}", eventType, realmName, hashPaths);
		} else {
			log.info("Starts activities \"{}\" on realm \"{}\", recursive from {}", eventType, realmName, hashPaths);
		}

		final var assetsFiles = fileService.resolveHashPaths(
				hashPaths,
				Set.of(DAS),
				Set.of(ONLINE),
				realmName,
				recursive)
				.stream()
				.filter(not(FileEntity::isDirectory))
				.map(f -> mediaAssetService.getFromFileEntry(f, mediaAssetService))
				.toList();

		log.trace("assets count = {}", assetsFiles.size());
		if (assetsFiles.isEmpty() == false) {
			runAssetsActivities(eventType, assetsFiles);
		}
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
		final var confEnv = pendingActivityJob.configuredEnv();

		final var selectedHandlers = activityHandlers.stream()
				.filter(handler -> previousHandlers.contains(handler.getHandlerName()) == false)
				.filter(activityHandler -> checkSupportedStorageStateClasses(activityHandler, confEnv))
				.filter(handler -> handler.canHandle(asset, eventType, confEnv))
				.toList();

		previousHandlers.addAll(getHandlersNames(selectedHandlers));

		final var newJobs = selectedHandlers.stream()
				.map(activityHandler -> pendingActivityJob.evolve(
						activityHandler,
						previousHandlers,
						internalObjectMapper.writeValueAsString(previousHandlers)))
				.toList();

		pendingActivityDao.declateActivities(
				newJobs,
				aboutInstance.getInstanceName(),
				aboutInstance.getPid());

		newJobs.forEach(job -> {
			log.trace(LOG_QUEUE_RUN_FOR_ON, asset, job.activityHandler().getHandlerName());
			jobKitEngine.runOneShot(job);
		});

	}

	@Override
	@Transactional
	public void restartPendingActivities() {
		log.info("Restart pending activities...");

		final var activityFilesIds = pendingActivityDao.getFilesAndWithResetPendingActivities(
				configuration.getRealmNames(),
				aboutInstance.getInstanceName(),
				aboutInstance.getPid());

		if (activityFilesIds.isEmpty()) {
			log.info("No pending activities to restart");
			return;
		}

		log.info("Prepare to restart {} activity(ies)...", activityFilesIds.size());

		final var unknownActivityHanders = new HashSet<String>();
		final var pendingActivitiesByFile = pendingActivityDao.getFilesAndPendingActivityByFileId(activityFilesIds);

		final var allActivitiesJobs = pendingActivitiesByFile.entrySet()
				.stream()
				.flatMap(entry -> {
					final var file = entry.getKey();
					final var pendings = entry.getValue();

					final var previousHandlersFromDb = pendings.stream()
							.map(PendingActivityEntity::getPreviousHandlers)
							.map(ph -> internalObjectMapper.readValue(ph, TYPE_LIST_STRING))
							.flatMap(List::stream);
					final var actualHandlersFromDb = pendings.stream()
							.map(PendingActivityEntity::getHandlerName);

					final var previousHandlers = Collections.synchronizedSet(
							new HashSet<String>(
									Stream.concat(
											previousHandlersFromDb,
											actualHandlersFromDb)
											.distinct()
											.toList()));

					final var asset = mediaAssetService.getFromFileEntry(file, mediaAssetService);
					final var confEnv = configuration.getRealmAndStorage(file.getRealm(), file.getStorage());
					final var oIndexer = indexer.getIndexerByRealm(confEnv.realmName());

					return pendings.stream()
							.map(pending -> {
								final var oActivityHander = activityHandlers.stream()
										.filter(ah -> ah.getHandlerName().equalsIgnoreCase(pending
												.getHandlerName()))
										.findFirst();
								if (oActivityHander.isEmpty()) {
									unknownActivityHanders.add(pending.getHandlerName());
									return null;
								}

								return new PendingActivityJob(
										confEnv,
										asset,
										oActivityHander.get(),
										ActivityEventType.valueOf(pending.getEventType()),
										previousHandlers,
										internalObjectMapper.writeValueAsString(previousHandlers),
										pendingActivityDao,
										this,
										oIndexer);
							})
							.filter(Objects::nonNull);
				})
				.toList();

		if (unknownActivityHanders.isEmpty() == false) {
			log.warn("Can't found ActivityHander with this name(s): {}", unknownActivityHanders);
		}

		log.info("Restart {} activity(ies)", allActivitiesJobs.size());
		allActivitiesJobs.forEach(job -> {
			log.trace(LOG_QUEUE_RUN_FOR_ON, job.asset(), job.activityHandler().getHandlerName());
			jobKitEngine.runOneShot(job);
		});
	}

	@Override
	public void cleanupFiles(final String realmName,
							 final String storageName,
							 final RealmConf realm,
							 final Set<? extends FileAttributesReference> losted) {
		losted.forEach(file -> mediaAssetService.purgeAssetArtefacts(realmName, storageName, file));
	}

}
