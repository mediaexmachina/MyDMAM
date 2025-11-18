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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Validated
@Slf4j
public record PathIndexingRealm(@Valid @NotNull Map<String, PathIndexingStorage> storages,
								Duration timeBetweenScans,
								String spoolScans,
								String spoolProcessAsset,
								File workingDirectory) {

	public Stream<Entry<String, PathIndexingStorage>> storagesStream() {
		return Optional.ofNullable(storages)
				.orElse(Map.of())
				.entrySet()
				.stream();
	}

	public Optional<File> getValidWorkingDirectory(final String realmName) {
		if (workingDirectory == null) {
			log.debug("No workingDirectory set for realm {}", realmName);
			return Optional.empty();
		}
		try {
			if (workingDirectory.exists() == false) {
				log.info("Create working directory \"{}\" for realm {}",
						workingDirectory.getAbsolutePath(), realmName);
				forceMkdir(workingDirectory);
			} else if (workingDirectory.isDirectory() == false
					   || workingDirectory.canRead() == false
					   || workingDirectory.canWrite() == false) {
				throw new IOException("Can't read/write or it's not a directory");
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(
					"Invalid workingDirectory: " + workingDirectory + " for realm " + realmName, e);
		}

		return Optional.ofNullable(workingDirectory);
	}

}
