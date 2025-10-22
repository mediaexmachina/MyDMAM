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

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping(value = "/api/v1/ping",
				produces = APPLICATION_JSON_VALUE)
public class PingController {

	public static record PingRequest(@NotBlank @Size(min = 5, max = 255) String payload) {
	}

	public static record PongResponse(String payload) {
	}

	@PostMapping("")
	public ResponseEntity<PongResponse> postPing(@RequestBody @Validated final PingRequest ping) {
		return new ResponseEntity<>(
				new PongResponse(
						"pong: " + ping.payload.trim().toUpperCase()),
				OK);
	}

	@GetMapping("/fail")
	public ResponseEntity<Map<String, String>> getFail() {
		return new ResponseEntity<>(Map.of("why", "Fail!"), HttpStatus.I_AM_A_TEAPOT);
	}

}
