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
package media.mexm.mydmam.indexer;

import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
public class RealmIndexer { // TODO test

	private static final String FILE_HASH_PATH = "file.hashPath";
	private final String realmName;
	private final File indexDir;
	private final Directory fsDirectoryIndex;
	private final StandardAnalyzer analyzer;
	private final IndexWriterConfig indexWriterConfig;
	private final IndexWriter writer;

	public RealmIndexer(final String realmName,
						final File workingDir) throws IOException {
		this.realmName = realmName;
		indexDir = new File(workingDir, "index");

		forceMkdir(indexDir);
		fsDirectoryIndex = FSDirectory.open(indexDir.toPath());
		analyzer = new StandardAnalyzer();
		indexWriterConfig = new IndexWriterConfig(analyzer);
		indexWriterConfig.setCommitOnClose(true);

		writer = new IndexWriter(fsDirectoryIndex, indexWriterConfig);
	}

	public void close() throws IOException {
		writer.close();
	}

	private Document toDocument(final FileAttributesReference file, final String storageName, final String hashPath) {
		final var document = new Document();

		document.add(new TextField("file.path", file.getPath(), YES));
		document.add(new IntField("file.directory", file.isDirectory() ? 1 : 0, YES));
		document.add(new IntField("file.hidden", file.isHidden() ? 1 : 0, YES));
		document.add(new IntField("file.link", file.isLink() ? 1 : 0, YES));
		document.add(new IntField("file.special", file.isSpecial() ? 1 : 0, YES));
		document.add(new LongField("file.date", file.lastModified(), YES));
		document.add(new LongField("file.length", file.length(), YES));
		document.add(new IntField("file.exists", file.exists() ? 1 : 0, YES));

		document.add(new TextField("file.parentPath", file.getParentPath(), NO));
		document.add(new TextField("file.name", file.getName(), NO));
		document.add(new StringField(FILE_HASH_PATH, hashPath, NO));
		document.add(new StringField("file.parentHashPath",
				hashPath(realmName, storageName, file.getParentPath()), NO));
		return document;
	}

	public void put(final Set<? extends FileAttributesReference> files, final String storageName) throws IOException {
		if (files.isEmpty()) {
			return;
		}

		writer.addDocuments(files.stream()
				.map(f -> toDocument(f, storageName, hashPath(realmName, storageName, f.getPath())))
				.toList());
		writer.commit();
	}

	public void update(final Set<? extends FileAttributesReference> files,
					   final String storageName) throws IOException {
		if (files.isEmpty()) {
			return;
		}

		for (final var file : files) {
			final var hashPath = hashPath(realmName, storageName, file.getPath());
			final Iterable<? extends IndexableField> doc = toDocument(file, storageName, hashPath);
			writer.updateDocument(new Term(FILE_HASH_PATH, hashPath), doc);
		}
		writer.commit();
	}

	public void remove(final Set<? extends FileAttributesReference> files,
					   final String storageName) throws IOException {
		if (files.isEmpty()) {
			return;
		}

		final var terms = files.stream()
				.map(f -> new Term(FILE_HASH_PATH, hashPath(realmName, storageName, f.getPath())))
				.toArray(Term[]::new);

		writer.deleteDocuments(terms);
		writer.commit();
	}

	/*
	 * <ul>
	 * <li>{@link SortedDocValuesField}: {@code byte[]} indexed column-wise for sorting/faceting
	 * <li>{@link SortedSetDocValuesField}: {@code SortedSet<byte[]>} indexed column-wise for
	 * sorting/faceting
	 * <li>{@link NumericDocValuesField}: {@code long} indexed column-wise for sorting/faceting
	 * <li>{@link SortedNumericDocValuesField}: {@code SortedSet<long>} indexed column-wise for
	 * sorting/faceting
	 * <li>{@link StoredField}: Stored-only value for retrieving in summary results
	 * </ul>
	 * *
	 */
}
