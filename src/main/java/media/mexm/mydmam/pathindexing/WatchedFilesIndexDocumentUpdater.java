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
 * Copyright (C) Media ex Machina 2026
 *
 */
package media.mexm.mydmam.pathindexing;

import java.util.stream.Stream;

import media.mexm.mydmam.component.FileAttributesReferenceIndexConverter;
import media.mexm.mydmam.indexer.IndexedDocumentConverter;
import media.mexm.mydmam.indexer.IndexedDocumentUpdater;
import media.mexm.mydmam.tools.StorageFileAttributesReference;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.FileAttributesReference;

public record WatchedFilesIndexDocumentUpdater(String realm,
											   String storage,
											   WatchedFiles scanResult,
											   FileAttributesReferenceIndexConverter converter) implements
											  IndexedDocumentUpdater<StorageFileAttributesReference> { // TODO test

	@Override
	public IndexedDocumentConverter<StorageFileAttributesReference> getConverter() {
		return converter;
	}

	StorageFileAttributesReference transform(final FileAttributesReference file) {
		return new StorageFileAttributesReference(file, storage, realm);
	}

	@Override
	public Stream<StorageFileAttributesReference> itemsToAdd() {
		return scanResult.founded().stream().map(this::transform);
	}

	@Override
	public Stream<StorageFileAttributesReference> itemsToDelete() {
		return scanResult.losted().stream().map(this::transform);
	}

	@Override
	public Stream<StorageFileAttributesReference> itemsToUpdate() {
		return scanResult.updated().stream().map(this::transform);
	}

}
