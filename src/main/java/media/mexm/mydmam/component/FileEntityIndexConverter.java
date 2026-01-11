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

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.indexer.IndexedDocumentConverter;
import media.mexm.mydmam.tools.StorageFileAttributesReference;

@Component
public class FileEntityIndexConverter implements IndexedDocumentConverter<FileEntity> { // TODO test

	@Autowired
	FileAttributesReferenceIndexConverter fileAttributesConverter;

	StorageFileAttributesReference makeFromFileEntity(final FileEntity file) {
		return new StorageFileAttributesReference(
				file.toFileAttributesReference(true),
				file.getStorage(),
				file.getRealm());
	}

	@Override
	public String getDocumentTypeName() {
		return fileAttributesConverter.getDocumentTypeName();
	}

	@Override
	public void toDocument(final FileEntity item, final Document document) {
		fileAttributesConverter.toDocument(makeFromFileEntity(item), document);
	}

	@Override
	public Term makeDocumentReferenceTerm(final FileEntity item) {
		return fileAttributesConverter.makeDocumentReferenceTerm(makeFromFileEntity(item));
	}

}
