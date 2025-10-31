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

import java.io.IOException;
import java.time.Duration;

import media.mexm.mydmam.component.PathIndexer;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import tv.hd3g.jobkit.watchfolder.FolderActivity;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;

public record RealmStorageFolderActivity(PathIndexer indexer,
										 String realmName,
										 PathIndexingRealm realm,
										 String storageName,
										 PathIndexingStorage storage) implements FolderActivity {

	@Override
	public void onAfterScan(final ObservedFolder observedFolder,
							final Duration scanTime,
							final WatchedFiles scanResult) throws IOException {
		// TODO (2) Add to index base
		// TODO (2) Add to analysis
	}

}
