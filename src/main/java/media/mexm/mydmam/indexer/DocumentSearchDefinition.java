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

import java.util.Set;
import java.util.function.Consumer;

import org.apache.lucene.document.Document;

public interface DocumentSearchDefinition { // TODO test

	default Set<String> getStoredFieldsAddedToDocument() {
		return Set.of();
	}

	String getDocumentTypeName();

	void addStoredFieldsToSearchResult(Document foundedDoc,
									   float score,
									   String explain,
									   Consumer<FileSearchResult> foundedFilesToAdd);

}
