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
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_DATE;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_DIRECTORY;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_EXISTS;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_HASH_PATH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_HIDDEN;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_LENGTH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_LINK;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_NAME;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_PARENT_HASH_PATH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_PARENT_PATH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_PATH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_SPECIAL;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_STORAGE;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
public class RealmIndexer { // TODO test

	private final String realmName;
	private final File indexDir;
	private final Directory fsDirectoryIndex;
	private final DirectoryReader reader;
	private final StandardAnalyzer analyzer;
	private final IndexWriterConfig indexWriterConfig;

	public RealmIndexer(final String realmName,
						final File workingDir) throws IOException {
		this.realmName = realmName;
		indexDir = new File(workingDir, "index");

		forceMkdir(indexDir);
		fsDirectoryIndex = FSDirectory.open(indexDir.toPath());
		analyzer = new StandardAnalyzer();
		indexWriterConfig = new IndexWriterConfig(analyzer);
		reader = DirectoryReader.open(fsDirectoryIndex);
	}

	public synchronized void close() {
		try {
			reader.close();
		} catch (final IOException e) {
			log.error("Can't close Lucene reader index on " + indexDir.getAbsolutePath(), e);
		}
		try {
			fsDirectoryIndex.close();
		} catch (final IOException e) {
			log.error("Can't close Lucene index on " + indexDir.getAbsolutePath(), e);
		}
	}

	private synchronized void write(final LuceneWriterConsumer cWriter) {
		try {
			log.debug("Open Lucene index on \"{}\" for writing", indexDir);
			final var writer = new IndexWriter(fsDirectoryIndex, indexWriterConfig);
			cWriter.accept(writer);
			writer.close();
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write to Lucene index on " + indexDir.getAbsolutePath(), e);
		}
	}

	private Document toDocument(final FileAttributesReference file, final String storageName, final String hashPath) {
		final var document = new Document();

		log.trace("Make Lucene document on {}:{}:{}", realmName, storageName, hashPath);

		document.add(new TextField(FILE_STORAGE, storageName, YES));
		document.add(new TextField(FILE_PATH, file.getPath(), YES));
		document.add(new IntField(FILE_DIRECTORY, file.isDirectory() ? 1 : 0, YES));
		document.add(new IntField(FILE_HIDDEN, file.isHidden() ? 1 : 0, YES));
		document.add(new IntField(FILE_LINK, file.isLink() ? 1 : 0, YES));
		document.add(new IntField(FILE_SPECIAL, file.isSpecial() ? 1 : 0, YES));
		document.add(new LongField(FILE_DATE, file.lastModified(), YES));
		document.add(new LongField(FILE_LENGTH, file.length(), YES));
		document.add(new IntField(FILE_EXISTS, file.exists() ? 1 : 0, YES));

		document.add(new TextField(FILE_PARENT_PATH, file.getParentPath(), NO));
		document.add(new TextField(FILE_NAME, file.getName(), NO));
		document.add(new StringField(FILE_HASH_PATH, hashPath, NO));
		document.add(new StringField(FILE_PARENT_HASH_PATH,
				hashPath(realmName, storageName, file.getParentPath()), NO));
		return document;
	}

	public void update(final WatchedFiles scanResult, final String storageName) {
		if (scanResult.founded().isEmpty()
			&& scanResult.updated().isEmpty()
			&& scanResult.losted().isEmpty()) {
			return;
		}

		log.info("Start to update Lucene index \"{}\" with {}", indexDir, scanResult);
		write(writer -> {
			writer.addDocuments(scanResult.founded().stream()
					.map(f -> toDocument(f, storageName, hashPath(realmName, storageName, f.getPath())))
					.toList());

			for (final var file : scanResult.updated()) {
				final var hashPath = hashPath(realmName, storageName, file.getPath());
				final Iterable<? extends IndexableField> doc = toDocument(file, storageName, hashPath);
				// TODO maybe a read update ?
				/*
				public void updateField(String docId, int newFieldvalue) {
					MyDataObject data = primaryDataStore.fetch(docId);
					data.setFieldValue(newFieldValue);
					primaryDataStore.save(data);
					updateIndex(data);
				}

				public void updateIndex(MyDataObject object) {
					// convertToLucene is more or less the code in the
					// first snippet of your question
					Document d = convertToLucene(object);
					// IndexWriter should be created once
					// IndexWriter.updateDocument will internally delete and index
					// the document
					this.writer.updateDocument(new Term("id", object.getId()), d);
					// potentially call writer.commit()
				}
				* */
				writer.updateDocument(new Term(FILE_HASH_PATH, hashPath), doc);
			}

			final var terms = scanResult.losted().stream()
					.map(f -> new Term(FILE_HASH_PATH, hashPath(realmName, storageName, f.getPath())))
					.toArray(Term[]::new);
			writer.deleteDocuments(terms);

			writer.commit();
		});

	}

	public List<FileSearchResult> openSearch(final String q, final Optional<String> limitToStorage, final int limit) {
		final var searcher = new IndexSearcher(reader);
		final var builder = new BooleanQuery.Builder();

		/*
		https://stackoverflow.com/questions/2005084/how-to-specify-two-fields-in-lucene-queryparser
				 * */
		final var exists = new TermQuery(new Term(FILE_EXISTS, "1"));
		builder.add(exists, MUST);

		limitToStorage.ifPresent(storage -> {
			final var onStorage = new TermQuery(new Term(FILE_STORAGE, storage));
			builder.add(onStorage, MUST);
		});

		try {
			// TODO manage "*" with WildcardQuery + add *q*
			/*
			 * https://stackoverflow.com/questions/5484965/howto-perform-a-contains-search-rather-than-starts-with-using-lucene-net
			 *
			 *
			 * parser.setFuzzyMinSim(0.6f);
			 * */

			@Deprecated
			final var multiField = new MultiFieldQueryParser(
					new String[] { FILE_NAME }, analyzer).parse(q);
			builder.add(multiField, MUST);
		} catch (final ParseException e) {
			throw new IllegalArgumentException("Can't parse query", e);
		}

		try {
			final var sortedTopDoc = searcher.search(builder.build(), limit, Sort.RELEVANCE);
			final var storedFields = searcher.storedFields();

			final var result = new ArrayList<FileSearchResult>(sortedTopDoc.scoreDocs.length);

			for (final var scoredDoc : sortedTopDoc.scoreDocs) {
				final var doc = storedFields.document(scoredDoc.doc, Set.of(FILE_HASH_PATH));

				Optional.ofNullable(doc.getField(FILE_HASH_PATH))
						.map(IndexableField::stringValue)
						.map(hashPath -> new FileSearchResult(hashPath, scoredDoc.score))
						.ifPresent(result::add);
			}

			return Collections.unmodifiableList(result);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't read from Lucene index on " + indexDir.getAbsolutePath(), e);
		}
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

	/*
	protected Query intRangeQuery(String field,
	Integer min, Integer max,
	boolean includeBoundaries){

	TermRangeQuery rangeQuery = new TermRangeQuery(field,
	NumericUtils.intToPrefixCoded(min.intValue()),
	NumericUtils.intToPrefixCoded(max.intValue()),
	includeBoundaries, includeBoundaries);

	return rangeQuery;
	}

	protected Query longRangeQuery(String field,
	Long min, Long max,
	boolean includeBoundaries){

	TermRangeQuery rangeQuery = new TermRangeQuery(field,
	NumericUtils.longToPrefixCoded(min.longValue()),
	NumericUtils.longToPrefixCoded(max.longValue()),
	includeBoundaries, includeBoundaries);

	return rangeQuery;
	}
	 * */

}
