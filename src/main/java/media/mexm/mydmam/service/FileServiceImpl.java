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
 * Copyright (C) Media ex Machina 2026
 *
 */
package media.mexm.mydmam.service;

import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mydmam.entity.FileEntity.FILE_FULL_PATH_COMPARATOR;
import static media.mexm.mydmam.indexer.SearchConstraintCondition.MUST_NOT;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.dto.StorageCategory;
import media.mexm.mydmam.dto.StorageStateClass;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.FileRepository;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

	@Autowired
	MyDMAMConfigurationProperties configuration;
	@Autowired
	Indexer indexer;
	@Autowired
	FileRepository fileRepository;

	@Override
	public List<FileEntity> resolveHashPaths(final Set<String> hashPaths,
											 final Set<StorageCategory> limitCategory,
											 final Set<StorageStateClass> limitStorageClasses,
											 final String realmName,
											 final boolean recursive) {
		if (hashPaths.isEmpty()) {
			return List.of();
		}

		final var realm = configuration.getRealmByName(realmName)
				.orElseThrow(() -> new IllegalArgumentException("Invalid/not configured realm " + realmName));

		final var availableStorageNames = realm.getStorageNames(limitCategory, limitStorageClasses);
		if (availableStorageNames.isEmpty()) {
			throw new IllegalStateException(
					"No available storages for this selected realm \"" + realmName + "\", with "
											+ limitCategory + "/" + limitStorageClasses + "");
		}
		log.trace("realmName={} limitCategory={} availableStorageNames={} limitStorageClasses={}",
				realmName, limitCategory, limitStorageClasses, availableStorageNames);

		final var selectedFilesDirs = fileRepository.getByHashPath(hashPaths, realmName, availableStorageNames);
		final var selectedDirContent = fileRepository.getByParentHashPath(hashPaths, realmName, availableStorageNames);

		final var allSelected = Stream.of(
				selectedFilesDirs.stream(),
				selectedDirContent.stream())
				.flatMap(identity())
				.distinct()
				.collect(toUnmodifiableSet());

		if (recursive == false) {
			return allSelected.stream()
					.sorted(FILE_FULL_PATH_COMPARATOR)
					.toList();
		}

		final var realmIndexer = indexer.getIndexerByRealm(realmName)
				.orElseThrow(() -> new IllegalStateException("No indexer for realm " + realmName));

		final var actualSelectedHashPaths = allSelected.stream()
				.map(FileEntity::getHashPath)
				.collect(toUnmodifiableSet());

		final var subHashPaths = selectedDirContent.stream()
				.filter(FileEntity::isDirectory)
				.map(dirEntity -> realmIndexer.getHashPathsByRecursiveSearch(
						dirEntity.getStorage(),
						dirEntity.getPath(),
						MUST_NOT))
				.flatMap(Set::stream)
				.distinct()
				.filter(not(actualSelectedHashPaths::contains))
				.collect(toUnmodifiableSet());

		final var subFiles = fileRepository.getByHashPath(subHashPaths, realmName);

		return Stream.of(
				allSelected.stream(),
				subFiles.stream())
				.flatMap(identity())
				.distinct()
				.sorted(FILE_FULL_PATH_COMPARATOR)
				.toList();
	}

}
