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
package media.mexm.mydmam.asset;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.IOUtils.readLines;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FilenameUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import media.mexm.mydmam.indexer.RealmIndexer;
import media.mexm.mydmam.service.MediaAssetService;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.local.LocalFile;

@Slf4j
public class MediaAsset implements
						MimeTypeTrait,
						CreateFileMetadataEntryTrait,
						AccessFileMetadataEntryTrait,
						FileMetadataResolutionTrait {

	static final String MTD_KEY_FULL_INDEXED_TEXT = "full-indexed-text";
	public static final String PREVIEW_TYPE_TEXT_CONTENT = "text-content";
	@Getter
	private final MediaAssetService service;
	@Getter
	private final FileEntity file;
	private Map<AssetRenderedFileEntity, File> renderedFiles;
	private Set<FileMetadataEntity> metadatas;

	private final LinkedList<DeclaredRenderedFile> pendingDeclaredRenderedFiles;
	private final LinkedList<FileMetadataEntity> pendingFileMetadatas;
	private final HashMap<String, String> textContentByRenderedNameCache;

	public MediaAsset(final MediaAssetService service,
					  final FileEntity file) {
		this.service = requireNonNull(service, "\"service\" can't to be null");
		this.file = requireNonNull(file, "\"file\" can't to be null");
		pendingDeclaredRenderedFiles = new LinkedList<>();
		pendingFileMetadatas = new LinkedList<>();
		textContentByRenderedNameCache = new HashMap<>();
	}

	public String getHashPath() {
		return file.getHashPath();
	}

	public String getName() {
		return FilenameUtils.getName(file.getPath());
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("realmName=");
		builder.append(file.getRealm());
		builder.append(", storageName=");
		builder.append(file.getStorage());
		builder.append(", path=\"");
		builder.append(file.getPath());
		builder.append("\"");
		return builder.toString();
	}

	/**
	 * Please do checks before call: StorageCategory must be DAS.
	 * @return only local file, else throw UnsupportedOperationException
	 */
	public File getLocalInternalFile(final PathIndexingStorage storage) {
		try (final var fileSystem = new AbstractFileSystemURL(storage.path())) {
			final var aFile = fileSystem.getFromPath(file.getPath());

			if (aFile instanceof final LocalFile localFile) {
				return localFile.getInternalFile();
			} else {
				throw new UnsupportedOperationException("Can't manage non-local files from " + fileSystem.getClass());
			}
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't access to file system", e);
		}
	}

	public synchronized void declareRenderedStaticFile(final File workingFile,
													   final String name,
													   final boolean toGzip,
													   final MimeTypeDetector mimeTypeDetector,
													   final int index,
													   final String previewType) throws IOException {
		pendingDeclaredRenderedFiles.add(new DeclaredRenderedFile(
				workingFile,
				name,
				toGzip,
				mimeTypeDetector,
				index,
				previewType));
	}

	public synchronized Map<AssetRenderedFileEntity, File> getRenderedFiles() {
		if (renderedFiles == null) {
			final var realm = file.getRealm();
			final var allRenderedFiles = service.getAllRenderedFiles(
					file.getHashPath(),
					realm);

			renderedFiles = new HashMap<>(
					allRenderedFiles.stream()
							.collect(toUnmodifiableMap(
									identity(),
									aRFEntity -> service.getPhysicalRenderedFile(aRFEntity, realm))));
		}
		return unmodifiableMap(renderedFiles);
	}

	@Override
	public synchronized Set<FileMetadataEntity> getMetadatas() {
		if (metadatas == null) {
			metadatas = new HashSet<>();
			metadatas.addAll(service.getAllMetadatas(this));
		}
		return unmodifiableSet(metadatas);
	}

	@Override
	public synchronized void createFileMetadataEntry(final String originHandler,
													 final String classifier,
													 final int layer,
													 final String key,
													 final String value) {
		pendingFileMetadatas.add(new FileMetadataEntity(file, originHandler, classifier, layer, key, value));
	}

	public synchronized void addFullTextToIndex(final MetadataExtractorHandler mtdHander,
												final String classifier,
												final int layer,
												final MimeTypeDetector mimeTypeDetector,
												final Charset charsetName,
												final File textToIndex) {
		log.debug("Add full text from {} to {}:{}:{}, via", textToIndex, file, classifier, layer, mtdHander);

		try {
			final var content = readFileToString(textToIndex, charsetName);
			final var staticName = mtdHander.getMetadataOriginName() + "-" + classifier + "-full-text";
			final var renderedFile = new DeclaredRenderedFile(
					textToIndex, staticName, true, mimeTypeDetector, layer, PREVIEW_TYPE_TEXT_CONTENT);

			if (renderedFile.mimeType().equals("text/plain")) {
				log.trace("Founded mimeType for {}: {}", textToIndex, renderedFile.mimeType());
			} else {
				log.warn("Founded mimeType for {}: {}, is not seen as text/plain. File will be moved on {}",
						textToIndex, renderedFile.mimeType(), renderedFile.workingFile());
			}

			pendingDeclaredRenderedFiles.add(renderedFile);
			createFileMetadataEntry(mtdHander, classifier, layer, MTD_KEY_FULL_INDEXED_TEXT, staticName);
			textContentByRenderedNameCache.put(staticName, content);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't access to " + textToIndex, e);
		}
	}

	public synchronized void commit(final Optional<RealmIndexer> oIndexer) throws IOException {
		if (pendingDeclaredRenderedFiles.isEmpty()
			&& pendingFileMetadatas.isEmpty()) {
			return;
		}

		log.debug("Start to commit asset {}, pendingDeclaredRenderedFiles={}, pendingFileMetadatas={}",
				file,
				pendingDeclaredRenderedFiles.size(),
				pendingFileMetadatas.size());

		service.declareRenderedStaticFiles(
				file,
				pendingDeclaredRenderedFiles.stream().toList());
		pendingDeclaredRenderedFiles.clear();

		service.declareFileMetadatas(
				file,
				pendingFileMetadatas.stream().toList());
		pendingFileMetadatas.clear();

		renderedFiles = null;
		metadatas = null;

		oIndexer.ifPresent(indexer -> indexer.updateAsset(this));
	}

	public synchronized Map<FileMetadataEntity, String> getTextContentByfileMetadata() {
		getRenderedFiles().keySet()
				.stream()
				.filter(f -> PREVIEW_TYPE_TEXT_CONTENT.equals(f.getPreviewType()))
				.filter(f -> textContentByRenderedNameCache.containsKey(f.getName()) == false)
				.toList()
				.forEach(f -> {
					final var physicalRenderedFile = service.getPhysicalRenderedFile(f, file.getRealm());
					final var content = readTextFile(physicalRenderedFile, f.isGzipEncoded());
					textContentByRenderedNameCache.put(f.getName(), content);
				});

		return getMetadatas().stream()
				.filter(m -> MTD_KEY_FULL_INDEXED_TEXT.equals(m.getKey()))
				.filter(m -> textContentByRenderedNameCache.containsKey(m.getValue()))
				.collect(toUnmodifiableMap(
						identity(),
						m -> textContentByRenderedNameCache.get(m.getValue())));
	}

	static String readTextFile(final File file, final boolean unGzip) {
		try {
			if (unGzip) {
				log.info("Start to read gzip text file \"{}\" to import text ({} bytes)", file, file.length());
				try (final var fsi = new GZIPInputStream(new FileInputStream(file))) {
					return readLines(fsi, UTF_8)
							.stream()
							.collect(joining(" "));
				}
			}

			log.info("Start to read plain text file \"{}\" to import text ({} bytes)", file, file.length());
			return readFileToString(file, UTF_8);
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't read file " + file, e);
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(file.getId());
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MediaAsset)) {
			return false;
		}
		final var other = (MediaAsset) obj;
		return Objects.equals(file.getId(), other.file.getId());
	}

}
