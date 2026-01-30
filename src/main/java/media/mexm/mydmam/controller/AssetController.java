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
package media.mexm.mydmam.controller;

import static media.mexm.mydmam.App.CONTROLLER_BASE_MAPPING_API_PATH;
import static media.mexm.mydmam.activity.ActivityEventType.MANUAL_RESET;
import static media.mexm.mydmam.entity.FileEntity.MAX_NAME_SIZE;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.dto.ResetActivitiesRequest;
import media.mexm.mydmam.service.PendingActivityService;
import tv.hd3g.jobkit.engine.JobKitEngine;

@RestController
@Validated
@Slf4j
@RequestMapping(value = CONTROLLER_BASE_MAPPING_API_PATH + "/asset")
public class AssetController {

	@Autowired
	MyDMAMConfigurationProperties conf;
	@Autowired
	PendingActivityService pendingActivityService;
	@Autowired
	JobKitEngine jobKitEngine;

	@PostMapping(value = "/reset-activities/{realm}", produces = APPLICATION_JSON_VALUE)
	@Transactional
	public ResponseEntity<Void> resetActivities(@PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String realm,
												@RequestBody @Validated final ResetActivitiesRequest request) {
		jobKitEngine.runOneShot(
				"Reset Asset Activities",
				conf.asyncAPISpoolName(),
				0,
				() -> pendingActivityService.startsActivities(
						realm, request.hashPaths(), request.recursive(), MANUAL_RESET),
				e -> {
					if (e != null) {
						log.error("Can't run resetActivities", e);
					}
				});

		return new ResponseEntity<>(ACCEPTED);
	}

}
