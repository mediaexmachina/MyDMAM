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
package media.mexm.mydmam.service;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mydmam.asset.DatabaseUpdateDirection.PUSH_TO_DB;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.apache.commons.io.FileUtils.moveFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.asset.DatabaseUpdateDirection;
import media.mexm.mydmam.asset.DeclaredRenderedFile;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.AssetRenderedFileRepository;
import media.mexm.mydmam.repository.AssetSummaryDao;
import media.mexm.mydmam.repository.AssetSummaryRepository;
import media.mexm.mydmam.repository.FileRepository;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
@Service
public class MediaAssetServiceImpl implements MediaAssetService {

	@Autowired
	MyDMAMConfigurationProperties configuration;
	@Autowired
	FileRepository fileRepository;
	@Autowired
	AssetSummaryRepository assetSummaryRepository;
	@Autowired
	AssetSummaryDao assetSummaryDao;
	@Autowired
	AssetRenderedFileRepository assetRenderedFileRepository;

	@Override
	public MediaAsset getFromWatchfolder(final String realmName,
										 final String storageName,
										 final FileAttributesReference file,
										 final MediaAssetService injectedService) {
		final var hashPath = hashPath(realmName, storageName, file.getPath());
		final var fileEntity = fileRepository.getByHashPath(hashPath, realmName);
		return getFromFileEntry(fileEntity, injectedService);
	}

	@Override
	public MediaAsset getFromFileEntry(final FileEntity file, final MediaAssetService injectedService) {
		return new MediaAsset(injectedService, file);
	}

	@Override
	public void purgeAssetArtefacts(final String realmName,
									final String storageName,
									final FileAttributesReference file) {
		/**
		 * TO BE IMPLEMENTED
		 */
	}

	@Override
	@Transactional
	@Deprecated
	public String updateMimeType(final MediaAsset asset, final DatabaseUpdateDirection direction) {
		final var file = asset.getFile();

		if (direction == PUSH_TO_DB) {
			final var mimeType = asset.getMimeType();
			assetSummaryDao.updateMimeType(file, mimeType);
			return mimeType;
		} else if (assetSummaryDao.getForFile(file)) {
			return file.getAssetSummary().getMimeType();
		} else {
			return null;
		}
	}

	@Override
	@Transactional(REQUIRES_NEW)
	public Map<AssetRenderedFileEntity, File> declareRenderedStaticFiles(final MediaAsset asset,
																		 final Collection<DeclaredRenderedFile> declaredRenderedFiles) throws IOException {
		if (declaredRenderedFiles.isEmpty()) {
			return Map.of();
		}

		final var fileEntity = asset.getFile();
		final var renderedMetadataDirectory = Objects.requireNonNull(configuration.getRealmByName(fileEntity.getRealm())
				.get()
				.renderedMetadataDirectory());

		final var distinctFileNamesCount = (int) declaredRenderedFiles.stream()
				.map(DeclaredRenderedFile::name)
				.distinct()
				.count();
		if (distinctFileNamesCount != declaredRenderedFiles.size()) {
			throw new IOException("Can't add files with same names: " + declaredRenderedFiles);
		}

		final var toCreate = declaredRenderedFiles.stream()
				.map(rendered -> new AssetRenderedFileEntity(fileEntity, rendered))
				.toList();

		assetRenderedFileRepository.saveAllAndFlush(toCreate);

		final var createdEtags = toCreate.stream()
				.map(AssetRenderedFileEntity::getEtag)
				.collect(toUnmodifiableSet());
		final var created = assetRenderedFileRepository.getRenderedForFileByEtags(fileEntity.getId(), createdEtags);

		final var result = new HashMap<AssetRenderedFileEntity, File>();
		for (final var renderedFileEntity : created) {
			final var name = renderedFileEntity.getName();

			final var declaredRenderedFile = declaredRenderedFiles.stream()
					.filter(f -> f.name().equals(name))
					.findFirst()
					.orElseThrow(() -> new IllegalStateException(
							"Can't found " + name + " from " + declaredRenderedFiles));

			final var renderedFile = new File(renderedMetadataDirectory + renderedFileEntity.getRelativePath())
					.getAbsoluteFile()
					.getCanonicalFile();

			if (renderedFile.exists()) {
				throw new IOException("Can't move move rendered file " + renderedFileEntity + " to " + renderedFile
									  + ", file exists");
			}

			log.debug("Start to move rendered file from \"{}\" to \"{}\" [via {}]",
					declaredRenderedFile.workingFile(), renderedFile, renderedFileEntity);

			moveFile(declaredRenderedFile.workingFile(), renderedFile);

			result.put(renderedFileEntity, renderedFile);
		}

		return unmodifiableMap(result);
	}

	@Override
	public Set<AssetRenderedFileEntity> getAllRenderedFiles(final String fileHashpath,
															final String realm) {// TODO test
		return assetRenderedFileRepository.getAllRenderedFiles(fileHashpath, realm);
	}

	@Override
	public File getPhysicalRenderedFile(final AssetRenderedFileEntity assetRenderedFileEntity,
										final String realm) { // TODO test
		final var renderedMetadataDirectory = Objects.requireNonNull(configuration.getRealmByName(realm)
				.orElseThrow()
				.renderedMetadataDirectory());

		try {
			final var renderedFile = new File(renderedMetadataDirectory + assetRenderedFileEntity.getRelativePath())
					.getAbsoluteFile()
					.getCanonicalFile();
			if (renderedFile.exists() == false) {
				throw new FileNotFoundException(renderedFile.getPath());
			} else if (renderedFile.length() != assetRenderedFileEntity.getLength()) {
				throw new IOException("Invalid real file size: "
									  + renderedFile.length() + " bytes on \"" + renderedFile + "\"");
			}
			return renderedFile;
		} catch (final IOException e) {
			throw new UncheckedIOException("Can't access to a valid rendered file: " + assetRenderedFileEntity, e);
		}
	}

}
