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
import java.util.Map;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Validated
@Slf4j
public record InfraConf(@Valid Map<TechnicalName, PathIndexingRealm> realms,
						@DefaultValue("1h") @NotNull Duration timeBetweenScans,
						@DefaultValue("pathindexing") @NotEmpty String spoolEvents) {

	public InfraConf {
		if (timeBetweenScans == Duration.ZERO || timeBetweenScans.isNegative()) {
			throw new IllegalArgumentException("Invalid mockTimeBetweenScans=" + timeBetweenScans);
		}
	}

}
