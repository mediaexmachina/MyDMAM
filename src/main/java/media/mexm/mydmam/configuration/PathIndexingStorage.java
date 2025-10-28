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
package media.mexm.mydmam.configuration;

import java.time.Duration;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;

@Validated
public record PathIndexingStorage(@Valid @NotNull ObservedFolder scan,
								  @Valid @PositiveOrZero @Max(100) int maxDeep,
								  Duration timeBetweenScans,
								  String spoolScans) {

	public static final int DEFAULT_MAX_DEEP = 10;

	public int getDefaultMaxDeep() {
		if (maxDeep == 0) {
			return DEFAULT_MAX_DEEP;
		} else {
			return maxDeep;
		}

	}
}
