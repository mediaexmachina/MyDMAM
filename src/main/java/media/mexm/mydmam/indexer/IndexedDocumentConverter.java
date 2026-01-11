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

import static media.mexm.mydmam.indexer.NamedIndexField.DOCUMENT_TYPE;
import static org.apache.lucene.document.Field.Store.YES;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;

/**
 * Must be stateless and reusable
 */
public interface IndexedDocumentConverter<T> {

	default Document makeDocument() {
		final var document = new Document();
		document.add(new StringField(DOCUMENT_TYPE, getDocumentTypeName(), YES));
		return document;
	}

	String getDocumentTypeName();

	void toDocument(T item, Document document);

	Term makeDocumentReferenceTerm(T item);

}
