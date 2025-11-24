/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2025
 *
 */
package media.mexm.mydmam.service;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mydmam.activity.ActivityEventType.NEW_FOUNDED_FILE;
import static media.mexm.mydmam.activity.ActivityEventType.UPDATED_FILE;
import static media.mexm.mydmam.audittrail.AuditTrailObjectType.FILE;
import static media.mexm.mydmam.entity.FileEntity.hashPath;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.audittrail.AuditTrailBatchInsertObject;
import media.mexm.mydmam.audittrail.RealmAuditTrail;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.FileRepository;
import tv.hd3g.jobkit.engine.JobKitEngine;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFileScanner;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
@Service
public class PathIndexerServiceImpl implements PathIndexerService {

	@Autowired
	FileRepository fileRepository;
	@Autowired
	AuditTrail auditTrail;
	@Autowired
	JobKitEngine jobKitEngine;
	@Autowired
	MyDMAMConfigurationProperties configuration;
	@Autowired
	PendingActivityService pendingActivityService;

	@Override
	@Transactional
	public WatchedFiles updateFoundedFiles(final WatchedFileScanner scanner,
										   final String realmName,
										   final String storageName,
										   final ObservedFolder observedFolder,
										   final AbstractFileSystemURL fileSystem) {
		final var minFixedStateTime = observedFolder.getMinFixedStateTime();
		final var allWatchedFilesHashPath = fileRepository
				.getAllHashPathByStorage(realmName, storageName);
		final var detected = scanner.scan(fileSystem);

		/**
		 * Update all founded
		 */
		final var detectedByhashKey = detected.stream()
				.collect(Collectors.toUnmodifiableMap(
						d -> hashPath(realmName, storageName, d.getPath()), d -> d));
		log.trace("detectedByhashKey={}", detectedByhashKey.size());

		Map<String, FileEntity> entitiesByHashKey;
		if (detectedByhashKey.isEmpty() == false) {
			entitiesByHashKey = fileRepository.getByHashPath(detectedByhashKey.keySet()).stream()
					.collect(toUnmodifiableMap(FileEntity::getHashPath, wf -> wf));
		} else {
			entitiesByHashKey = new HashMap<>();
		}

		final var updateFounded = entitiesByHashKey.entrySet().stream()
				.map(entry -> entry.getValue().update(detectedByhashKey.get(entry.getKey())))
				.toList();

		/**
		 * Get updated
		 */
		final var updatedChangedFounded = updateFounded.stream()
				.filter(FileEntity::isWatchMarkedAsDone)
				.filter(u -> u.isTimeQualified(minFixedStateTime))
				.filter(FileEntity::isWatchDoneButChanged)
				.map(FileEntity::resetDoneButChanged)
				.map(FileEntity::getHashPath)
				.map(detectedByhashKey::get)
				.collect(toUnmodifiableSet());

		/**
		 * Get qualified, set them marked
		 */
		final var qualifyFounded = updateFounded.stream()
				.filter(not(FileEntity::isWatchMarkedAsDone))
				.filter(u -> u.isTimeQualified(minFixedStateTime))
				.map(FileEntity::setMarkedAsDone)
				.toList();

		/**
		 * Get only them can be callbacked
		 */
		final var qualifiedAndCallbacked = qualifyFounded.stream()
				.filter(u -> u.isTimeQualified(minFixedStateTime))
				.map(FileEntity::getHashPath)
				.map(detectedByhashKey::get)
				.collect(toUnmodifiableSet());

		Set<FileAttributesReference> lostedAndCallbacked;
		if (detectedByhashKey.isEmpty() == false) {
			lostedAndCallbacked = fileRepository
					.getDoneAndLostedByHashPath(
							detectedByhashKey.keySet(),
							realmName, storageName)
					.stream()
					.map(f -> f.toFileAttributesReference(false))
					.collect(toUnmodifiableSet());
		} else {
			lostedAndCallbacked = fileRepository
					.getDoneLostedForEmptyDir(realmName, storageName).stream()
					.map(f -> f.toFileAttributesReference(false))
					.collect(toUnmodifiableSet());
		}

		/**
		 * Add new files
		 */
		final var addNewEntites = detected.stream()
				.filter(f -> (allWatchedFilesHashPath.contains(hashPath(realmName, storageName, f.getPath())) == false))
				.map(f -> new FileEntity(realmName, storageName, f))
				.toList();
		if (addNewEntites.isEmpty() == false) {
			fileRepository.saveAll(addNewEntites);
		}

		/**
		 * Clean deleted files
		 */
		final var toClean = allWatchedFilesHashPath.stream()
				.filter(not(detectedByhashKey::containsKey))
				.collect(toUnmodifiableSet());
		if (toClean.isEmpty() == false) {
			fileRepository.deleteByHashPath(toClean);
		}

		log.trace(
				"Lists detected={}, addNewEntites={}, updateFounded={}, updatedChangedFounded={}, qualifyFounded={}, qualifiedAndCallbacked={}, lostedAndCallbacked={}, toClean={}",
				detected.size(),
				addNewEntites.size(),
				updateFounded.size(),
				updatedChangedFounded.size(),
				qualifyFounded.size(),
				qualifiedAndCallbacked.size(),
				lostedAndCallbacked.size(),
				toClean.size());

		final var size = fileRepository.countByStorage(realmName, storageName);

		log.debug("Scan watchedFilesResult for {}: {} founded, {} lost, {} total",
				observedFolder.getLabel(),
				qualifiedAndCallbacked.size(),
				lostedAndCallbacked.size(),
				size);

		return new WatchedFiles(
				qualifiedAndCallbacked,
				lostedAndCallbacked,
				updatedChangedFounded,
				size);
	}

	@Override
	@Transactional
	public void resetFoundedFiles(final String realmName,
								  final String storageName,
								  final ObservedFolder observedFolder,
								  final Set<CachedFileAttributes> foundedFiles) {
		if (foundedFiles.isEmpty()) {
			throw new IllegalArgumentException("foundedFiles can't to be empty");
		}
		final var hashPathListToPurge = foundedFiles.stream()
				.map(ff -> hashPath(realmName, storageName, ff.getPath()))
				.collect(toUnmodifiableSet());
		fileRepository.deleteByHashPath(hashPathListToPurge);
	}

	@Override
	@Transactional
	public void onAfterScan(final String realmName,
							final String storageName,
							final PathIndexingRealm realm,
							final PathIndexingStorage storage,
							final ObservedFolder observedFolder,
							final Duration scanTime,
							final WatchedFiles scanResult) {
		auditTrail.getAuditTrailByRealm(realmName)
				.ifPresent(rat -> {
					fileActivitytoAuditTrail(realmName, storageName, rat, "founded", scanResult.founded());
					fileActivitytoAuditTrail(realmName, storageName, rat, "losted", scanResult.losted());
					fileActivitytoAuditTrail(realmName, storageName, rat, "updated", scanResult.updated());
				});

		pendingActivityService.startsActivities(realmName, storageName, realm, scanResult.founded(), NEW_FOUNDED_FILE);
		pendingActivityService.startsActivities(realmName, storageName, realm, scanResult.updated(), UPDATED_FILE);
		pendingActivityService.cleanupFiles(realmName, storageName, realm, scanResult.losted());
	}

	@Override
	public void fileActivitytoAuditTrail(final String realmName,
										 final String storageName,
										 final RealmAuditTrail auditTrail,
										 final String event,
										 final Set<? extends FileAttributesReference> items) {
		if (items.isEmpty()) {
			return;
		}
		log.debug("Save to audit trail after scan result on {}:{}, event={}, {} item(s)",
				realmName, storageName, event, items.size());

		final var inserts = items.stream()
				.map(i -> new AuditTrailBatchInsertObject(
						FILE,
						hashPath(realmName, storageName, i.getPath()),
						i))
				.toList();
		auditTrail.asyncPersist("pathindex", event, inserts);
	}

}
