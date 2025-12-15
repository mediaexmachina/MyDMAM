/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2025
 *
 */
package media.mexm.mydmam.service;

import java.time.Duration;
import java.util.Set;

import media.mexm.mydmam.audittrail.RealmAuditTrail;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFileScanner;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;
import tv.hd3g.transfertfiles.FileAttributesReference;

public interface PathIndexerService {

	WatchedFiles updateFoundedFiles(WatchedFileScanner scanner,
									String realmName,
									String storageName,
									ObservedFolder observedFolder,
									AbstractFileSystemURL fileSystem);

	void resetFoundedFiles(String realmName,
						   String storageName,
						   ObservedFolder observedFolder,
						   Set<CachedFileAttributes> foundedFiles);

	void onAfterScan(String realmName,
					 String storageName,
					 RealmConf realm,
					 PathIndexingStorage storage,
					 ObservedFolder observedFolder,
					 Duration scanTime,
					 WatchedFiles scanResult);

	void fileActivitytoAuditTrail(String realmName,
								  String storageName,
								  RealmAuditTrail auditTrail,
								  String event,
								  Set<? extends FileAttributesReference> items);
}
