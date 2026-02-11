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

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.component.MimeTypeDetector;

@Slf4j
public record DeclaredRenderedFile(File workingFile,
								   String name,
								   boolean toGzip,
								   String mimeType,
								   int index,
								   String previewType) {

	public DeclaredRenderedFile(final File workingFile,
								final String name,
								final boolean toGzip,
								final MimeTypeDetector mimeTypeDetector,
								final int index,
								final String previewType) throws IOException {
		final File validatedWorkingFile;
		final String mimeType;
		if (toGzip) {

			validatedWorkingFile = new File(workingFile.getParentFile(), workingFile.getName() + ".gz");

			try (final var fso = new GZIPOutputStream(new FileOutputStream(validatedWorkingFile))) {
				log.info("Gzip rendered file from \"{}\" to \"{}\"", workingFile, validatedWorkingFile);
				copyFile(workingFile, fso);
			}

			mimeType = mimeTypeDetector.getMimeType(workingFile);
			forceDelete(workingFile);
		} else {
			validatedWorkingFile = workingFile;
			mimeType = mimeTypeDetector.getMimeType(workingFile);
		}
		this(validatedWorkingFile, name, toGzip, mimeType, index, previewType);
	}

}
