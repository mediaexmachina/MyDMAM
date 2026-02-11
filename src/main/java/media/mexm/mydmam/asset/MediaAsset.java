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
package media.mexm.mydmam.asset;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static media.mexm.mydmam.asset.DatabaseUpdateDirection.GET_FROM_DB;
import static media.mexm.mydmam.asset.DatabaseUpdateDirection.PUSH_TO_DB;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.service.MediaAssetService;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.local.LocalFile;

@Slf4j
public class MediaAsset {

	@Getter
	private final MediaAssetService service;
	@Getter
	private final FileEntity file;
	private String mimeType;
	private Map<AssetRenderedFileEntity, File> renderedFiles;

	// TODO pending commit

	public MediaAsset(final MediaAssetService service,
					  final FileEntity file) {
		this.service = requireNonNull(service, "\"service\" can't to be null");
		this.file = requireNonNull(file, "\"file\" can't to be null");
	}

	public String getHashPath() {
		return file.getHashPath();
	}

	public String getName() {
		return FilenameUtils.getName(file.getPath());
	}

	public synchronized String getMimeType() {
		if (mimeType == null) {
			mimeType = service.updateMimeType(this, GET_FROM_DB);
		}
		return mimeType;
	}

	public synchronized void setMimeType(final String mimeType) {
		requireNonNull(mimeType, "\"mimeType\" can't to be null");
		if (mimeType.equals(this.mimeType)) {
			return;
		}
		this.mimeType = mimeType;
		service.updateMimeType(this, PUSH_TO_DB);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("realmName=");
		builder.append(file.getRealm());
		builder.append(", storageName=");
		builder.append(file.getStorage());
		builder.append(", path=\"");
		builder.append(file.getPath());
		builder.append("\"");
		return builder.toString();
	}

	/**
	 * Please do checks before call: StorageCategory must be DAS.
	 * @return only local file, else throw UnsupportedOperationException
	 */
	public File getLocalInternalFile(final PathIndexingStorage storage) {
		try (final var fileSystem = new AbstractFileSystemURL(storage.path())) {
			final var aFile = fileSystem.getFromPath(file.getPath());

			if (aFile instanceof final LocalFile localFile) {
				return localFile.getInternalFile();
			} else {
				throw new UnsupportedOperationException("Can't manage non-local files from " + fileSystem.getClass());
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't access to file system", e);
		}
	}

	public synchronized void declareRenderedStaticFile(final DeclaredRenderedFile declaredRenderedFile,
													   final int index,
													   final String previewType) throws IOException {
		declareRenderedStaticFiles(Set.of(declaredRenderedFile), index, previewType);
	}

	public synchronized void declareRenderedStaticFiles(final Collection<DeclaredRenderedFile> declaredRenderedFiles,
														final int index,
														final String previewType) throws IOException {
		final var declaredFiles = service.declareRenderedStaticFiles(this, declaredRenderedFiles, index, previewType);

		if (renderedFiles == null) {
			renderedFiles = new HashMap<>(declaredFiles);
		} else {
			renderedFiles.putAll(declaredFiles);
		}
	}

	public Map<AssetRenderedFileEntity, File> getRenderedFiles() {// TODO2 implements
		if (renderedFiles == null) {
			return Map.of();
		}
		return unmodifiableMap(renderedFiles);
	}

	public synchronized void createFileMetadataEntries(final MetadataExtractorHandler originHandler,
													   final String classifier,
													   final int layer,
													   final Map<String, String> entries) {
		originHandler.getMetadataOriginName();
		// TODO2 implement
	}

}
