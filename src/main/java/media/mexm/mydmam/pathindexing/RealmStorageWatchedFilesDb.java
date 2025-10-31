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
package media.mexm.mydmam.pathindexing;

import java.util.Set;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.component.PathIndexer;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.service.PathIndexerService;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchFolderPickupType;
import tv.hd3g.jobkit.watchfolder.WatchedFileScanner;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.jobkit.watchfolder.WatchedFilesDb;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;

@Slf4j
public class RealmStorageWatchedFilesDb implements WatchedFilesDb { // TODO test

	private final WatchedFileScanner scanner;
	private final String realmName;
	private final String storageName;
	private final PathIndexerService pathIndexerService;

	public RealmStorageWatchedFilesDb(final PathIndexer indexer,
									  final String realmName,
									  final String storageName,
									  final PathIndexingStorage storage) {
		scanner = new WatchedFileScanner(storage.scan(), storage.getDefaultMaxDeep());
		this.realmName = realmName;
		this.storageName = storageName;
		pathIndexerService = indexer.getPathIndexerService();
	}

	@Override
	public void setup(final ObservedFolder observedFolder, final WatchFolderPickupType pickUp) {
		/**
		 * No need to setup, beacause we have one db by ObservedFolder
		 */
	}

	@Override
	public WatchedFiles update(final ObservedFolder observedFolder, final AbstractFileSystemURL fileSystem) {
		return pathIndexerService.updateFoundedFiles(
				scanner, realmName, storageName, observedFolder, fileSystem);
	}

	@Override
	@Transactional
	public void reset(final ObservedFolder observedFolder, final Set<CachedFileAttributes> foundedFiles) {
		pathIndexerService.resetFoundedFiles(realmName, storageName, observedFolder, foundedFiles);
	}

}
