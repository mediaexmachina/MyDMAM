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

import static tv.hd3g.transfertfiles.AbstractFile.normalizePath;

import java.io.File;
import java.time.Duration;
import java.util.Set;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.URLAccess;

@Validated
public record PathIndexingStorage(@NotEmpty String path,
								  @Valid @PositiveOrZero @Max(100) @DefaultValue("10") int maxDeep,
								  @DefaultValue("1h") Duration timeBetweenScans,
								  @DefaultValue("10") int retryAfterTimeFactor,
								  Set<String> allowedExtentions,
								  Set<String> blockedExtentions,
								  Set<String> ignoreRelativePaths,
								  @DefaultValue("[\"desktop.ini\", \".DS_Store\", \"Thumbs.db\"]") Set<String> ignoreFiles,
								  Set<String> allowedFileNames,
								  Set<String> allowedDirNames,
								  Set<String> blockedFileNames,
								  Set<String> blockedDirNames,
								  @DefaultValue("true") boolean allowedHidden,
								  @DefaultValue("false") boolean allowedLinks,
								  @DefaultValue("20s") Duration minFixedStateTime,
								  @DefaultValue("false") boolean noScans,
								  @DefaultValue("pathindexing") @NotEmpty String spoolScans) {

	public PathIndexingStorage {
		if (timeBetweenScans != null && (timeBetweenScans == Duration.ZERO || timeBetweenScans.isNegative())) {
			throw new IllegalArgumentException("Invalid mockTimeBetweenScans=" + timeBetweenScans);
		}

		try {
			final var localPath = new File(path);
			if (localPath.exists()) {
				final var newTargetFolder = "file://localhost" + normalizePath(localPath.getCanonicalFile()
						.getAbsolutePath());
				path = newTargetFolder;
			} else {
				new URLAccess(path);
			}
			new AbstractFileSystemURL(path).close();
		} catch (final Exception e) {
			throw new IllegalArgumentException(
					"Can't found directory, or it doesn't seem to be an valid URL: \"" + path + "\"", e);
		}
	}

	/**
	 * @see ObservedFolder
	 */
	public ObservedFolder makeObservedFolder(final String realmName,
											 final String storageName) {
		final var observedFolder = new ObservedFolder();
		observedFolder.setLabel(realmName + ":" + storageName);
		observedFolder.setAllowedDirNames(allowedDirNames);
		observedFolder.setAllowedExtentions(allowedExtentions);
		observedFolder.setAllowedFileNames(allowedFileNames);
		observedFolder.setAllowedHidden(allowedHidden);
		observedFolder.setAllowedLinks(allowedLinks);
		observedFolder.setBlockedDirNames(blockedDirNames);
		observedFolder.setBlockedExtentions(blockedExtentions);
		observedFolder.setBlockedFileNames(blockedFileNames);
		observedFolder.setDisabled(noScans);
		observedFolder.setIgnoreFiles(ignoreFiles);
		observedFolder.setIgnoreRelativePaths(ignoreRelativePaths);
		observedFolder.setMinFixedStateTime(minFixedStateTime);
		observedFolder.setRetryAfterTimeFactor(retryAfterTimeFactor);
		observedFolder.setRecursive(true);
		observedFolder.setRetryAfterTimeFactor(retryAfterTimeFactor);
		observedFolder.setSpoolScans(spoolScans);
		observedFolder.setTargetFolder(path);
		observedFolder.setTimeBetweenScans(timeBetweenScans);
		return observedFolder;
	}

}
