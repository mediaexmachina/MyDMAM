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

import static org.apache.commons.io.FileUtils.forceMkdir;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;

@Validated
@Slf4j
public record RealmConf(@Valid Map<TechnicalName, PathIndexingStorage> storages,
						Duration timeBetweenScans,
						@DefaultValue("processasset") @NotEmpty String spoolProcessAsset,
						File workingDirectory) {

	public RealmConf {
		storages = Optional.ofNullable(storages).orElse(Map.of());

		if (workingDirectory != null) {
			try {
				if (workingDirectory.exists() == false) {
					log.info("Create working directory \"{}\"", workingDirectory.getAbsolutePath());
					forceMkdir(workingDirectory);
				} else if (workingDirectory.isDirectory() == false
						   || workingDirectory.canRead() == false
						   || workingDirectory.canWrite() == false) {
					throw new IOException("Can't read/write or it's not a directory");
				}
			} catch (final IOException e) {
				throw new UncheckedIOException("Invalid workingDirectory: " + workingDirectory, e);
			}
		}

		if (timeBetweenScans != null && (timeBetweenScans == Duration.ZERO || timeBetweenScans.isNegative())) {
			throw new IllegalArgumentException("Invalid mockTimeBetweenScans=" + timeBetweenScans);
		}
	}

}
