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
package media.mexm.mydmam.controller;

import static java.lang.Math.min;
import static java.util.Collections.unmodifiableMap;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static java.util.stream.Stream.concat;
import static media.mexm.mydmam.App.CONTROLLER_BASE_MAPPING_API_PATH;
import static media.mexm.mydmam.dto.FileItemResponse.createFromEntity;
import static media.mexm.mydmam.dto.FileMetadatasReponse.createFromEntities;
import static media.mexm.mydmam.dto.StorageCategory.EXTERNAL;
import static media.mexm.mydmam.dto.StorageStateClass.OFFLINE;
import static media.mexm.mydmam.entity.FileEntity.HASH_STRING_LEN;
import static media.mexm.mydmam.entity.FileEntity.MAX_NAME_SIZE;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.component.InternalObjectMapper;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.dto.FileMetadatasReponse;
import media.mexm.mydmam.dto.FileResponse;
import media.mexm.mydmam.dto.RealmListResponse;
import media.mexm.mydmam.dto.StorageListResponse;
import media.mexm.mydmam.dto.StorageState;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.AssetSummaryEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.AssetRenderedFileDao;
import media.mexm.mydmam.repository.AssetSummaryDao;
import media.mexm.mydmam.repository.FileDao;
import media.mexm.mydmam.repository.FileRepository;
import media.mexm.mydmam.repository.FileSort;
import media.mexm.mydmam.tools.SortOrder;

@RestController
@Validated
@RequestMapping(value = CONTROLLER_BASE_MAPPING_API_PATH + "/filesystem",
				produces = APPLICATION_JSON_VALUE)
@Slf4j
public class FileSystemController {

	@Autowired
	MyDMAMConfigurationProperties conf;
	@Autowired
	FileRepository fileRepository;
	@Autowired
	FileDao fileDao;
	@Autowired
	AssetSummaryDao assetSummaryDao;
	@Autowired
	AssetRenderedFileDao assetRenderedFileDao;
	@Autowired
	InternalObjectMapper internalObjectMapper;

	@GetMapping("/list")
	@Transactional
	public ResponseEntity<RealmListResponse> getRealms() {
		final var list = fileRepository.getAllRealms().stream().sorted().toList();
		return new ResponseEntity<>(new RealmListResponse(list), OK);
	}

	@GetMapping("/list/{realm}")
	@Transactional
	public ResponseEntity<StorageListResponse> getStorages(@PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String realm) {
		final var storageDatabaseList = fileRepository.getAllStoragesByRealm(realm).stream().sorted().toList();

		final var configuredStorageStates = conf.getRealmByName(realm)
				.stream()
				.map(RealmConf::storages)
				.map(Map::entrySet)
				.flatMap(Set::stream)
				.collect(toUnmodifiableMap(
						entry -> entry.getKey().name(),
						entry -> {
							final var pathIndexingStorage = entry.getValue();
							final var description = pathIndexingStorage.description();
							final var location = pathIndexingStorage.location();
							final var category = pathIndexingStorage.getCategory();
							final var stateClass = pathIndexingStorage.getStorageStateClass();
							return new StorageState(description, location, category, stateClass);
						}));

		final var notConfiguredStoragesNames = storageDatabaseList.stream()
				.filter(name -> configuredStorageStates.containsKey(name) == false)
				.collect(toUnmodifiableMap(
						name -> name,
						_ -> new StorageState("", "", EXTERNAL, OFFLINE)));

		final var allStorageStates = new HashMap<>(configuredStorageStates);
		allStorageStates.putAll(notConfiguredStoragesNames);

		return new ResponseEntity<>(
				new StorageListResponse(
						realm,
						storageDatabaseList,
						unmodifiableMap(allStorageStates)),
				OK);
	}

	@GetMapping("/list/{realm}/{storage}")
	@Transactional
	public ResponseEntity<FileResponse> listRoot(@PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String realm,
												 @PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String storage,
												 @RequestParam(required = false,
															   defaultValue = "0") @Min(0) final Integer skip,
												 @RequestParam(required = false,
															   defaultValue = "0") @Min(0) final Integer limit,
												 @RequestParam(required = false,
															   defaultValue = "0") @Min(0) @Max(1) final Integer summaries,
												 @RequestParam(required = false,
															   defaultValue = "0") @Min(0) @Max(1) final Integer rendered,
												 @RequestParam(required = false,
															   defaultValue = "none") final SortOrder sortByName,
												 @RequestParam(required = false,
															   defaultValue = "none") final SortOrder sortByType,
												 @RequestParam(required = false,
															   defaultValue = "none") final SortOrder sortByDate,
												 @RequestParam(required = false,
															   defaultValue = "none") final SortOrder sortBySize) {
		return list(realm, storage, hashPath(realm, storage, "/"),
				skip, limit, summaries, rendered, sortByName, sortByType, sortByDate, sortBySize);
	}

	@GetMapping("/list/{realm}/{storage}/{hashPath}")
	@Transactional
	public ResponseEntity<FileResponse> list(@PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String realm,
											 @PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String storage,
											 @PathVariable @NotBlank @Size(max = HASH_STRING_LEN) final String hashPath,
											 @RequestParam(required = false,
														   defaultValue = "0") @Min(0) final Integer skip,
											 @RequestParam(required = false,
														   defaultValue = "0") @Min(0) final Integer limit,
											 @RequestParam(required = false,
														   defaultValue = "0") @Min(0) @Max(1) final Integer summaries,
											 @RequestParam(required = false,
														   defaultValue = "0") @Min(0) @Max(1) final Integer rendered,
											 @RequestParam(required = false,
														   defaultValue = "none") final SortOrder sortByName,
											 @RequestParam(required = false,
														   defaultValue = "none") final SortOrder sortByType,
											 @RequestParam(required = false,
														   defaultValue = "none") final SortOrder sortByDate,
											 @RequestParam(required = false,
														   defaultValue = "none") final SortOrder sortBySize) {
		final var maxAllowedEntries = min(conf.dirListMaxSize(), limit == 0 ? conf.dirListMaxSize() : limit);
		final var sort = new FileSort(sortByName, sortByType, sortByDate, sortBySize);
		final var resultItemList = fileDao.getByParentHashPath(
				hashPath.toLowerCase(),
				skip,
				maxAllowedEntries,
				Optional.ofNullable(sort));
		final var listSize = resultItemList.size();
		final var totalSize = fileDao.countParentHashPathItems(realm, storage, hashPath.toLowerCase());
		final var oFileParent = Optional.ofNullable(fileRepository.getByHashPath(hashPath.toLowerCase(), realm));

		final var listParentPath = resultItemList.stream()
				.map(FileEntity::getPath)
				.map(FilenameUtils::getFullPathNoEndSeparator)
				.findFirst()
				.or(() -> oFileParent.map(FileEntity::getPath))
				.orElse(null);

		final var parentPath = Optional.ofNullable(listParentPath)
				.map(FilenameUtils::getFullPathNoEndSeparator)
				.orElse("/");
		final var parentHashPath = hashPath(realm, storage, parentPath);

		Set<Integer> allEntitiesIds = Set.of();
		if (summaries == 1 || rendered == 1) {
			allEntitiesIds = concat(resultItemList.stream(), oFileParent.stream())
					.filter(not(FileEntity::isDirectory))
					.map(FileEntity::getId)
					.distinct()
					.collect(toUnmodifiableSet());
		}

		final Map<String, AssetSummaryEntity> allSummariesByHashpath;
		if (summaries == 1) {
			allSummariesByHashpath = assetSummaryDao.getAssetSummariesByFileId(allEntitiesIds, realm);
		} else {
			allSummariesByHashpath = Map.of();
		}

		final Map<String, Set<AssetRenderedFileEntity>> allRenderedByHashpath;
		if (rendered == 1) {
			allRenderedByHashpath = assetRenderedFileDao.getRenderedFilesByFileId(allEntitiesIds, realm);
		} else {
			allRenderedByHashpath = Map.of();
		}

		final Map<String, FileMetadatasReponse> metadatas = concat(
				allSummariesByHashpath.keySet().stream(),
				allRenderedByHashpath.keySet().stream())
						.distinct()
						.collect(toUnmodifiableMap(
								identity(),
								f -> createFromEntities(
										allSummariesByHashpath.get(f),
										allRenderedByHashpath.get(f),
										internalObjectMapper)));

		try {
			final var currentItem = oFileParent
					.map(entity -> createFromEntity(entity, realm, storage))
					.orElse(null);

			return new ResponseEntity<>(
					new FileResponse(
							realm,
							storage,
							currentItem,
							listParentPath,
							parentHashPath,
							listSize,
							skip,
							totalSize,
							sort,
							resultItemList.stream()
									.map(fileEntity -> createFromEntity(fileEntity, realm, storage))
									.toList(),
							metadatas),
					OK);
		} catch (final IllegalArgumentException e) {
			log.debug("Invalid query for {}: {}", hashPath, e.getMessage());
			return new ResponseEntity<>(BAD_REQUEST);
		}
	}

}
