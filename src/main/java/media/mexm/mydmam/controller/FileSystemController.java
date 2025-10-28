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
import static media.mexm.mydmam.App.CONTROLLER_BASE_MAPPING_API_PATH;
import static media.mexm.mydmam.dto.FileItemResponse.createFromEntity;
import static media.mexm.mydmam.entity.FileEntity.HASH_STRING_LEN;
import static media.mexm.mydmam.entity.FileEntity.MAX_NAME_SIZE;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.dto.FileResponse;
import media.mexm.mydmam.dto.RealmListResponse;
import media.mexm.mydmam.dto.StorageListResponse;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.FileDao;
import media.mexm.mydmam.repository.FileRepository;

@RestController
@Validated
@RequestMapping(value = CONTROLLER_BASE_MAPPING_API_PATH + "/filesystem",
				produces = APPLICATION_JSON_VALUE)
@Slf4j
public class FileSystemController {

	@Autowired
	FileRepository fileRepository;
	@Autowired
	FileDao fileDao;

	@Value("${mydmamConsts.dirListMaxSize:100}")
	int dirListMaxSize;

	@GetMapping("/list")
	@Transactional
	public ResponseEntity<RealmListResponse> getRealms() {
		final var list = fileRepository.getAllRealms().stream().sorted().toList();
		return new ResponseEntity<>(new RealmListResponse(list), OK);
	}

	@GetMapping("/list/{realm}")
	@Transactional
	public ResponseEntity<StorageListResponse> getStorages(@PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String realm) {
		final var list = fileRepository.getAllStoragesByRealm(realm).stream().sorted().toList();
		return new ResponseEntity<>(new StorageListResponse(realm, list), OK);
	}

	@GetMapping("/list/{realm}/{storage}")
	@Transactional
	public ResponseEntity<FileResponse> listRoot(@PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String realm,
												 @PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String storage,
												 @RequestParam(required = false,
															   defaultValue = "0") @Min(0) final Integer skip,
												 @RequestParam(required = false,
															   defaultValue = "0") @Min(0) final Integer limit) {
		return list(realm, storage, hashPath(realm, storage, "/"), skip, limit);
	}

	@GetMapping("/list/{realm}/{storage}/{hashPath}")
	@Transactional
	public ResponseEntity<FileResponse> list(@PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String realm,
											 @PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String storage,
											 @PathVariable @NotBlank @Size(max = HASH_STRING_LEN) final String hashPath,
											 @RequestParam(required = false,
														   defaultValue = "0") @Min(0) final Integer skip,
											 @RequestParam(required = false,
														   defaultValue = "0") @Min(0) final Integer limit) {
		final var maxAllowedEntries = min(dirListMaxSize, limit == 0 ? dirListMaxSize : limit);
		final var resultItemList = fileDao.getByParentHashPath(hashPath.toLowerCase(), skip, maxAllowedEntries);
		final var listSize = resultItemList.size();
		final var totalSize = fileDao.countParentHashPathItems(realm, storage, hashPath.toLowerCase());
		final var oFileParent = Optional.ofNullable(fileRepository.getByHashPath(hashPath.toLowerCase()));

		final var listParentPath = resultItemList.stream()
				.map(FileEntity::getPath)
				.map(FilenameUtils::getFullPathNoEndSeparator)
				.findFirst()
				.or(() -> oFileParent.map(FileEntity::getPath))
				.orElse(null);

		final var parentPath = Optional.ofNullable(listParentPath)
				.map(p -> "" + FilenameUtils.getFullPathNoEndSeparator(p))
				.orElse("/");
		final var parentHashPath = hashPath(realm, storage, parentPath);

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
							resultItemList.stream().map(e -> createFromEntity(e, realm, storage)).toList()),
					OK);
		} catch (final IllegalArgumentException e) {
			log.debug("Invalid query for {}: {}", hashPath, e.getMessage());
			return new ResponseEntity<>(BAD_REQUEST);
		}
	}

}
