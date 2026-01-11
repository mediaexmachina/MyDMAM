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

import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static media.mexm.mydmam.indexer.NamedIndexField.DOCUMENT_TYPE_FILE;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_BASE_NAME;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_DATE;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_DIRECTORY;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_HASH_PATH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_HIDDEN;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_LENGTH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_LINK;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_NAME;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_PARENT_HASH_PATH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_PARENT_PATH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_SPECIAL;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_STORAGE;
import static media.mexm.mydmam.indexer.RealmIndexer.normalizeSearchString;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;

import java.util.Set;
import java.util.function.Consumer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.indexer.DocumentSearchDefinition;
import media.mexm.mydmam.indexer.FileSearchResult;
import media.mexm.mydmam.indexer.IndexedDocumentConverter;
import media.mexm.mydmam.tools.StorageFileAttributesReference;

@Slf4j
@Component
public class FileAttributesReferenceIndexConverter implements
												   IndexedDocumentConverter<StorageFileAttributesReference>,
												   DocumentSearchDefinition {// TODO test

	@Override
	public String getDocumentTypeName() {
		return DOCUMENT_TYPE_FILE;
	}

	@Override
	public Set<String> getStoredFieldsAddedToDocument() {
		return Set.of(
				FILE_HASH_PATH,
				FILE_STORAGE,
				FILE_NAME,
				FILE_PARENT_PATH);
	}

	@Override
	public void addStoredFieldsToSearchResult(final Document foundedDoc,
											  final float score,
											  final String explain,
											  final Consumer<FileSearchResult> foundedFilesToAdd) {
		final var hashPath = foundedDoc.getField(FILE_HASH_PATH);
		final var storage = foundedDoc.getField(FILE_STORAGE);
		final var name = foundedDoc.getField(FILE_NAME);
		final var parentPath = foundedDoc.getField(FILE_PARENT_PATH);

		foundedFilesToAdd.accept(new FileSearchResult(
				hashPath.stringValue(),
				storage.stringValue(),
				name.stringValue(),
				parentPath.stringValue(),
				score,
				explain));
	}

	@Override
	public void toDocument(final StorageFileAttributesReference item, final Document document) {
		final var hashPath = makeHashPath(item);
		log.trace("Make Lucene document on {}:{}:{}", item.realm(), item.storage(), hashPath);

		final var file = item.file();
		document.add(new StringField(FILE_HASH_PATH, hashPath, YES));
		document.add(new StringField(FILE_STORAGE, item.storage(), YES));
		document.add(new StringField(FILE_NAME, file.getName(), YES));
		document.add(new StringField(FILE_PARENT_PATH, file.getParentPath(), YES));

		normalizeSearchString(getBaseName(file.getName()))
				.forEach(baseName -> document.add(new StringField(FILE_BASE_NAME, baseName, NO)));

		document.add(new IntField(FILE_DIRECTORY, file.isDirectory() ? 1 : 0, NO));
		document.add(new IntField(FILE_HIDDEN, file.isHidden() ? 1 : 0, NO));
		document.add(new IntField(FILE_LINK, file.isLink() ? 1 : 0, NO));
		document.add(new IntField(FILE_SPECIAL, file.isSpecial() ? 1 : 0, NO));

		document.add(new LongField(FILE_DATE, file.lastModified(), NO));
		document.add(new LongField(FILE_LENGTH, file.length(), NO));
		document.add(new StringField(FILE_PARENT_HASH_PATH,
				hashPath(item.realm(), item.storage(), file.getParentPath()), NO));

	}

	String makeHashPath(final StorageFileAttributesReference item) {
		return hashPath(item.realm(), item.storage(), item.file().getPath());
	}

	@Override
	public Term makeDocumentReferenceTerm(final StorageFileAttributesReference item) {
		return new Term(FILE_HASH_PATH, makeHashPath(item));
	}

}
