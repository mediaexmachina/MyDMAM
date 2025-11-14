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

import static java.util.stream.Collectors.toUnmodifiableMap;
import static media.mexm.mydmam.entity.FileEntity.hashPath;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

import media.mexm.mydmam.audittrail.RealmAuditTrail;
import media.mexm.mydmam.component.PathIndexer;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import tv.hd3g.jobkit.watchfolder.FolderActivity;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.FileAttributesReference;

public record RealmStorageFolderActivity(PathIndexer indexer,
										 String realmName,
										 PathIndexingRealm realm,
										 String storageName,
										 PathIndexingStorage storage) implements FolderActivity {// TODO test

	@Override
	public void onAfterScan(final ObservedFolder observedFolder,
							final Duration scanTime,
							final WatchedFiles scanResult) throws IOException {
		indexer.getAuditTrail()
				.getAuditTrailByRealm(realmName)
				.ifPresent(auditTrail -> {
					toAuditTrail(auditTrail, "founded", scanResult.founded());
					toAuditTrail(auditTrail, "losted", scanResult.losted());
					toAuditTrail(auditTrail, "updated", scanResult.updated());
				});
	}

	void toAuditTrail(final RealmAuditTrail auditTrail,
					  final String event,
					  final Set<? extends FileAttributesReference> items) {
		if (items.isEmpty()) {
			return;
		}

		final var dataByObject = items.stream()
				.collect(toUnmodifiableMap(
						file -> hashPath(realmName, storageName, file.getPath()),
						Object.class::cast)); // (Object) file
		auditTrail.asyncPersist("pathindex", event, dataByObject);
	}

}
