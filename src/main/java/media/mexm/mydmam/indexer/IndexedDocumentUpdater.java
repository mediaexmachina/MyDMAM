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
package media.mexm.mydmam.indexer;

import static java.util.stream.Stream.empty;

import java.util.stream.Stream;

public interface IndexedDocumentUpdater<T> {// TODO test

	IndexedDocumentConverter<T> getConverter();

	default Stream<T> itemsToAdd() {
		return empty();
	}

	default Stream<T> itemsToUpdate() {
		return empty();
	}

	default Stream<T> itemsToDelete() {
		return empty();
	}

}
