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
package media.mexm.mydmam.component;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.medsea.mimeutil.detector.MagicMimeMimeDetector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MagicMimeDetector {// TODO test

	@Autowired
	MagicMimeMimeDetector detector;

	public List<String> getMimeTypes(final InputStream source) {
		return wrapTypes(detector.getMimeTypes(source));
	}

	public List<String> getMimeTypes(final File source) {
		final var typeExtraction = wrapTypes(detector.getMimeTypes(source));
		log.trace("File \"{}\" detected as mime type {}", source.getPath(), typeExtraction);
		return typeExtraction;
	}

	private List<String> wrapTypes(final Collection<?> types) {
		if (types.isEmpty()) {
			return List.of("application/octet-stream");
		}
		return types.stream().map(Object::toString).toList();
	}

	// TODO implements EncodingGuesser

}
