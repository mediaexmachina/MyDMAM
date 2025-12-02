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
import static media.mexm.mydmam.entity.FileEntity.MAX_NAME_SIZE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Optional;

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
import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.dto.RealmListResponse;
import media.mexm.mydmam.repository.FileDao;
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
	FileRepository fileRepository;// TODO needed ?
	@Autowired
	FileDao fileDao;// TODO needed ?

	@Value("${mydmamConsts.searchResultMaxSize:100}")
	int searchResultMaxSize;

	@GetMapping("/{realm}")
	@Transactional
	public ResponseEntity<RealmListResponse> openSearch(@PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String realm,
														@RequestParam(required = true) @NotBlank @Size(max = 256) final String q,
														@RequestParam(required = false,
																	  defaultValue = "0") @Min(0) final Integer limit) {
		final var oRealmIndexer = indexer.getIndexerByRealm(realm);
		if (oRealmIndexer.isEmpty()) {
			return new ResponseEntity<>(UNPROCESSABLE_ENTITY);
		}

		final var maxAllowedEntries = min(searchResultMaxSize, limit == 0 ? searchResultMaxSize : limit);
		final var searchResult = oRealmIndexer.get().openSearch(q.trim(), Optional.empty(), maxAllowedEntries);
		// TODO return with dedictated DTO

		final var list = fileRepository.getAllRealms().stream().sorted().toList(); // TODO nope
		return new ResponseEntity<>(new RealmListResponse(list), OK);
	}

	// TODO (2) same with limit to storage
	// TODO (2) optional with files resolution
}
