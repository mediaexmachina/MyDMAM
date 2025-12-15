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
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mydmam.App.CONTROLLER_BASE_MAPPING_API_PATH;
import static media.mexm.mydmam.dto.FileItemResponse.createFromEntity;
import static media.mexm.mydmam.entity.FileEntity.MAX_NAME_SIZE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.dto.FileItemResponse;
import media.mexm.mydmam.dto.OpenSearchResponse;
import media.mexm.mydmam.dto.SearchConstraintsRequest;
import media.mexm.mydmam.indexer.FileSearchResult;
import media.mexm.mydmam.repository.FileRepository;

@RestController
@Validated
@RequestMapping(value = CONTROLLER_BASE_MAPPING_API_PATH + "/search",
				produces = APPLICATION_JSON_VALUE)
@Slf4j
public class SearchController {

	@Autowired
	Indexer indexer;
	@Autowired
	FileRepository fileRepository;
	@Autowired
	MyDMAMConfigurationProperties conf;

	@RequestMapping(value = "/{realm}", method = { GET, POST, PUT })
	@Transactional
	public ResponseEntity<OpenSearchResponse> openSearch(@PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String realm,
														 @RequestParam(required = true) @NotBlank @Size(max = 256) final String q,
														 @RequestParam(required = false,
																	   defaultValue = "0") @Min(0) final Integer limit,
														 @RequestParam(required = false,
																	   defaultValue = "0") @Min(0) @Max(1) final Integer resolveHashPaths,
														 @RequestBody(required = false) @Validated @Nullable final SearchConstraintsRequest constraints) {

		final var oRealmIndexer = indexer.getIndexerByRealm(realm);
		if (oRealmIndexer.isEmpty()) {
			return new ResponseEntity<>(UNPROCESSABLE_ENTITY);
		}

		final var maxAllowedEntries = min(conf.searchResultMaxSize(), limit == 0 ? conf.searchResultMaxSize() : limit);
		final var oFileConstraints = Optional.ofNullable(constraints).map(SearchConstraintsRequest::fileConstraints);

		final var searchResult = oRealmIndexer.get().openSearch(q.trim(), oFileConstraints, maxAllowedEntries);

		Map<String, FileItemResponse> relatedFiles = Map.of();
		if (resolveHashPaths == 1 && searchResult.foundedFiles().isEmpty() == false) {
			final var hashPathsToResolve = searchResult.foundedFiles().stream()
					.map(FileSearchResult::hashPath)
					.distinct()
					.collect(toUnmodifiableSet());
			relatedFiles = fileRepository.getByHashPath(hashPathsToResolve).stream()
					.map(f -> createFromEntity(f, realm, f.getStorage()))
					.collect(toUnmodifiableMap(FileItemResponse::hashPath, identity()));
		}

		return new ResponseEntity<>(
				new OpenSearchResponse(
						searchResult,
						q.trim(),
						maxAllowedEntries,
						relatedFiles,
						constraints),
				OK);
	}

	@PostMapping("/reset-all-indexes")
	public ResponseEntity<Void> reset() {
		indexer.reset("admin-ops");
		return new ResponseEntity<>(OK);
	}

}
