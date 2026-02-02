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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mydmam.dto.StorageCategory.DAS;
import static media.mexm.mydmam.dto.StorageStateClass.ONLINE;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.dto.StorageCategory;
import media.mexm.mydmam.dto.StorageStateClass;
import media.mexm.mydmam.tools.DelayedSyncConfiguration;

@Validated
@Slf4j
public record RealmConf(@Valid Map<TechnicalName, PathIndexingStorage> storages,
						Duration timeBetweenScans,
						@DefaultValue("processasset") @NotEmpty String spoolProcessAsset,
						File workingDirectory,
						File renderedMetadataDirectory,
						DelayedSyncConfiguration delayedSync) {

	public RealmConf {
		storages = Optional.ofNullable(storages).orElse(Map.of());

		checkDirectory(workingDirectory, "workingDirectory");
		checkDirectory(renderedMetadataDirectory, "renderedMetadataDirectory");

		if (timeBetweenScans != null && (timeBetweenScans == Duration.ZERO || timeBetweenScans.isNegative())) {
			throw new IllegalArgumentException("Invalid mockTimeBetweenScans=" + timeBetweenScans);
		}

		if (delayedSync == null) {
			delayedSync = new DelayedSyncConfiguration(1000, Duration.ofMinutes(1));
		}
	}

	static void checkDirectory(final File directory, final String name) {
		if (directory == null) {
			return;
		}
		try {
			if (directory.exists() == false) {
				log.info("Create {} \"{}\"", name, directory.getAbsolutePath());
				forceMkdir(directory);
			} else if (directory.isDirectory() == false
					   || directory.canRead() == false
					   || directory.canWrite() == false) {
				throw new IOException("Can't read/write " + directory + " or it's not a directory, for " + name);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Invalid directory: " + directory + ", for " + name, e);
		}
	}

	public Set<String> getOnlineDASStorageNames() {
		return getStorageNames(Set.of(DAS), Set.of(ONLINE));
	}

	/**
	 * @param limitCategory empty = all
	 * @param limitStorageClasses empty = all
	 */
	public Set<String> getStorageNames(final Set<StorageCategory> limitCategory,
									   final Set<StorageStateClass> limitStorageClasses) {
		return storages()
				.entrySet()
				.stream()
				.filter(entry -> {
					if (limitCategory.isEmpty()) {
						return true;
					}
					return limitCategory.contains(entry.getValue().getCategory());
				})
				.filter(entry -> {
					if (limitStorageClasses.isEmpty()) {
						return true;
					}
					return limitStorageClasses.contains(entry.getValue().getStorageStateClass());
				})
				.map(Entry::getKey)
				.map(TechnicalName::name)
				.collect(toUnmodifiableSet());
	}

	public Optional<PathIndexingStorage> getStorageByName(final String storage) {
		return storages()
				.entrySet()
				.stream()
				.filter(entry -> entry.getKey().toString().equals(storage))
				.map(Entry::getValue)
				.findFirst();
	}

	public File makeWorkingFile(final String name, final Class<?> referer) {
		try {
			if (workingDirectory == null) {
				throw new FileNotFoundException("No workingDirectory is set for realm");
			}
			final var result = File.createTempFile(referer.getSimpleName(), name, workingDirectory).getCanonicalFile();
			log.debug("Create/prepare temp file for realm: {}", result);
			forceDelete(result);
			return result;
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't prepare a temp file in " + workingDirectory, e);
		}
	}

}
