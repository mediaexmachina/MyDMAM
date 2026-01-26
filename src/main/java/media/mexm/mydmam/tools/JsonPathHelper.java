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
package media.mexm.mydmam.tools;

import java.util.Optional;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.TypeRef;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record JsonPathHelper(DocumentContext document) {// TODO test

	public <T> Optional<T> read(final String path, final Class<T> type) {
		try {
			return Optional.ofNullable(document.read(path, type));
		} catch (final PathNotFoundException e) {
			log.trace("Can't read path {}", path, e);
			return Optional.empty();
		}
	}

	public <T> Optional<T> read(final String path, final TypeRef<T> typeRef) {
		try {
			return Optional.ofNullable(document.read(path, typeRef));
		} catch (final PathNotFoundException e) {
			log.trace("Can't read path {}", path, e);
			return Optional.empty();
		}
	}

}
