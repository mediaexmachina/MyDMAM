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
package media.mexm.mydmam.asset;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.forceDelete;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileEntity;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode
public class DeclaredRenderedFile { // TODO test

	private final File workingFile;
	private final String name;
	private final boolean toGzip;
	private final String mimeType;

	public DeclaredRenderedFile(final File workingFile,
								final String name,
								final boolean toGzip,
								final MimeTypeDetector mimeTypeDetector) throws IOException {
		this.name = name;
		this.toGzip = toGzip;
		if (toGzip) {
			this.workingFile = new File(workingFile.getParentFile(), workingFile.getName() + ".gz");

			try (final var fso = new GZIPOutputStream(new FileOutputStream(this.workingFile))) {
				log.info("Gzip rendered file from \"{}\" to \"{}\"", workingFile, this.workingFile);
				copyFile(workingFile, fso);
			}

			mimeType = mimeTypeDetector.getMimeType(workingFile);
			forceDelete(workingFile);
		} else {
			this.workingFile = workingFile;
			mimeType = mimeTypeDetector.getMimeType(workingFile);
		}

	}

	public AssetRenderedFileEntity makeAssetRenderedFileEntity(final FileEntity fileEntity,
															   final int index,
															   final String previewType) {
		return new AssetRenderedFileEntity(
				fileEntity,
				mimeType,
				previewType,
				toGzip ? AssetRenderedFileEntity.GZIP_ENCODED : AssetRenderedFileEntity.NOT_ENCODED,
				index,
				name,
				workingFile.length());
	}

}
