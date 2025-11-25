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

import static java.text.Normalizer.normalize;
import static java.text.Normalizer.Form.NFKD;
import static media.mexm.mydmam.App.REPLACE_NORMALIZED;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static media.mexm.mydmam.indexer.NamedIndexField.DOCUMENT_TYPE;
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
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.entity.FileEntity;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
public class RealmIndexer {

	private static final Pattern REMOVE_NON_VALID_CHARS = Pattern.compile("[^a-z0-9]+"); // NOSONAR S5869
	private static final Pattern DETECT_NUMBERS = Pattern.compile(".*\\d.*");

	private final String realmName;
	private final File indexDir;
	private final Directory fsDirectoryIndex;
	private final StandardAnalyzer analyzer;
	private final boolean computeExplainOnResults;

	public RealmIndexer(final String realmName,
						final File workingDir,
						final boolean computeExplainOnResults) throws IOException {
		this.realmName = realmName;
		indexDir = new File(workingDir, "index");
		this.computeExplainOnResults = computeExplainOnResults;

		forceMkdir(indexDir);
		fsDirectoryIndex = FSDirectory.open(indexDir.toPath());
		analyzer = new StandardAnalyzer();
	}

	public synchronized void close() {
		try {
			fsDirectoryIndex.close();
		} catch (final IOException e) {
			log.error("Can't close Lucene index on " + indexDir.getAbsolutePath(), e);
		}
	}

	private synchronized void write(final LuceneWriterConsumer cWriter) {
		try {
			log.debug("Open Lucene index on \"{}\" for writing", indexDir);
			final var indexWriterConfig = new IndexWriterConfig(analyzer);
			final var writer = new IndexWriter(fsDirectoryIndex, indexWriterConfig);
			cWriter.accept(writer);
			writer.close();
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write to Lucene index on " + indexDir.getAbsolutePath(), e);
		}
	}

	/**
	 * Not compatible with other chars than western chars.
	 */
	public static List<String> normalizeSearchString(final String value) {
		if (value == null) {
			return List.of();
		}
		final var trimValue = value.toLowerCase().trim();
		if (trimValue.isEmpty()) {
			return List.of();
		}

		final var normalized = REPLACE_NORMALIZED.matcher(normalize(trimValue, NFKD)).replaceAll("");
		final var cleaned = REMOVE_NON_VALID_CHARS.matcher(normalized).replaceAll(" ").trim();

		if (cleaned.isEmpty()) {
			return List.of();
		}

		return Stream.of(cleaned.split(" "))
				.flatMap(word -> {
					if (DETECT_NUMBERS.matcher(word).matches()) {
						return Stream.of(word.split("(?<=\\d)(?=\\D)|(?=\\d)(?<=\\D)"));
					} else {
						return Stream.of(word);
					}
				})
				.toList();
	}

	private Document toDocument(final FileAttributesReference file, final String storageName, final String hashPath) {
		final var document = new Document();
		log.trace("Make Lucene document on {}:{}:{}", realmName, storageName, hashPath);

		document.add(new StringField(DOCUMENT_TYPE, DOCUMENT_TYPE_FILE, YES));
		document.add(new StringField(FILE_HASH_PATH, hashPath, YES));
		document.add(new StringField(FILE_STORAGE, storageName, YES));
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
				hashPath(realmName, storageName, file.getParentPath()), NO));
		return document;
	}

	private Document toDocument(final FileEntity file) {
		if (file.getRealm().equals(realmName) == false) {
			throw new IllegalArgumentException("Invalid realm (wants " + realmName + ") for " + file);
		}
		return toDocument(
				file.toFileAttributesReference(true),
				file.getStorage(),
				file.getHashPath());
	}

	/**
	 * Never forget to close() session a the end.
	 */
	public ResetSession reset(final int batchSize) {
		log.info("Wipe all file type documents on a reset session for realm {}", realmName);
		write(writer -> writer.deleteDocuments(new TermQuery(new Term(DOCUMENT_TYPE, DOCUMENT_TYPE_FILE))));

		return new ResetSession(
				this::toDocument,
				docList -> write(writer -> writer.addDocuments(docList)),
				batchSize);
	}

	public void update(final WatchedFiles scanResult, final String storageName) {
		if (scanResult.founded().isEmpty()
			&& scanResult.updated().isEmpty()
			&& scanResult.losted().isEmpty()) {
			return;
		}

		log.info("Start to update Lucene index \"{}\" with founded={}, updated={}, losted={}.",
				indexDir,
				scanResult.founded().size(),
				scanResult.updated().size(),
				scanResult.losted().size());

		write(writer -> {
			if (scanResult.founded().isEmpty() == false) {
				log.trace("Add to update Lucene index \"{}\": {}", indexDir, scanResult.founded());

				writer.addDocuments(scanResult.founded().stream()
						.map(f -> toDocument(f, storageName, hashPath(realmName, storageName, f.getPath())))
						.toList());
			}

			for (final var file : scanResult.updated()) {
				log.trace("Update Lucene index \"{}\": {}", indexDir, file);

				final var hashPath = hashPath(realmName, storageName, file.getPath());
				writer.updateDocument(new Term(FILE_HASH_PATH, hashPath), toDocument(file, storageName, hashPath));
			}

			if (scanResult.losted().isEmpty() == false) {
				log.trace("Remove from Lucene index \"{}\": {}", indexDir, scanResult.losted());

				final var terms = scanResult.losted().stream()
						.map(f -> new Term(FILE_HASH_PATH, hashPath(realmName, storageName, f.getPath())))
						.toArray(Term[]::new);
				writer.deleteDocuments(terms);
			}

			writer.commit();
		});

	}

	private SearchResult processSearch(final Optional<FileSearchConstraints> oFileSearchConstraints,
									   final Query query,
									   final int limit) {
		try (var reader = DirectoryReader.open(fsDirectoryIndex)) {
			final var searcher = new IndexSearcher(reader);

			final var builder = new BooleanQuery.Builder();
			oFileSearchConstraints.ifPresent(fileSearchConstraints -> fileSearchConstraints.apply(builder));
			builder.add(query, MUST);
			final var finalQuery = builder.build();

			final var totalFounded = searcher.count(finalQuery);
			final var sortedTopDoc = searcher.search(finalQuery, limit);
			final var storedFields = searcher.storedFields();
			final var foundedFiles = new ArrayList<FileSearchResult>();

			Explanation explain = null;
			for (final var scoredDoc : sortedTopDoc.scoreDocs) {
				final var doc = storedFields.document(scoredDoc.doc,
						Set.of(FILE_HASH_PATH,
								FILE_STORAGE,
								FILE_NAME,
								FILE_PARENT_PATH,
								DOCUMENT_TYPE));

				if (computeExplainOnResults) {
					explain = searcher.explain(finalQuery, scoredDoc.doc);
				}

				final var documentType = Optional.ofNullable(doc.getField(DOCUMENT_TYPE))
						.map(IndexableField::stringValue)
						.orElseThrow();

				if (documentType.equals(DOCUMENT_TYPE_FILE)) {
					final var hashPath = doc.getField(FILE_HASH_PATH);
					final var storage = doc.getField(FILE_STORAGE);
					final var name = doc.getField(FILE_NAME);
					final var parentPath = doc.getField(FILE_PARENT_PATH);

					foundedFiles.add(new FileSearchResult(
							hashPath.stringValue(),
							storage.stringValue(),
							name.stringValue(),
							parentPath.stringValue(),
							scoredDoc.score,
							Optional.ofNullable(explain).map(Explanation::toString).orElse(null)));

				} else {
					log.warn("Can't manage this document type: {}", documentType);
				}
			}

			return new SearchResult(foundedFiles.stream().sorted().toList(), totalFounded);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't read from Lucene index on " + indexDir.getAbsolutePath(), e);
		}
	}

	private static void addShouldBooleanBoostedQuery(final BooleanQuery.Builder builder,
													 final Query query,
													 final float boost) {
		builder.add(new BooleanClause(new BoostQuery(query, boost), SHOULD));
	}

	private static final Function<Query, BooleanClause> toBooleanMustClause = query -> new BooleanClause(query, MUST);

	public SearchResult openSearch(final String q,
								   final Optional<FileSearchConstraints> oConstraints,
								   final int limit) {
		final var mainQuery = new BooleanQuery.Builder();

		if (q.contains("*") || q.contains("?")) {
			/**
			 * Contains wildcards
			 */
			addShouldBooleanBoostedQuery(mainQuery, new WildcardQuery(new Term(FILE_NAME, q)), 10f);
			addShouldBooleanBoostedQuery(mainQuery, new WildcardQuery(new Term(FILE_BASE_NAME, q)), 8f);
		} else {
			/**
			 * Exact file name match
			 */
			addShouldBooleanBoostedQuery(mainQuery, new TermQuery(new Term(FILE_NAME, q)), 10f);
			addShouldBooleanBoostedQuery(mainQuery, new TermQuery(new Term(FILE_BASE_NAME, q)), 8f);
		}

		addShouldBooleanBoostedQuery(mainQuery, new FuzzyQuery(new Term(FILE_NAME, q)), 0.5f);

		final var normalizeQ = normalizeSearchString(q);

		addShouldBooleanBoostedQuery(mainQuery,
				new BooleanQuery.Builder()
						.add(normalizeQ.stream()
								.map(word -> new Term(FILE_NAME, word))
								.map(TermQuery::new)
								.map(toBooleanMustClause)
								.toList())
						.build(), 5f);

		addShouldBooleanBoostedQuery(mainQuery,
				new BooleanQuery.Builder()
						.add(normalizeQ.stream()
								.map(word -> new Term(FILE_BASE_NAME, word))
								.map(TermQuery::new)
								.map(toBooleanMustClause)
								.toList())
						.build(), 3f);

		addShouldBooleanBoostedQuery(mainQuery,
				new BooleanQuery.Builder()
						.add(normalizeQ.stream()
								.map(word -> new BooleanQuery.Builder()
										.add(new BooleanClause(
												new FuzzyQuery(
														new Term(FILE_BASE_NAME, word)),
												SHOULD))
										.add(new BooleanClause(
												new WildcardQuery(
														new Term(FILE_BASE_NAME, "*" + word + "*")),
												SHOULD))
										.build())
								.map(toBooleanMustClause)
								.toList())
						.build(), 0.1f);

		return processSearch(
				oConstraints,
				mainQuery.build(),
				limit);
	}

}
