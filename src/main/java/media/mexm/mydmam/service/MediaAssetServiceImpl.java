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

import static media.mexm.mydmam.asset.DatabaseUpdateDirection.PUSH_TO_DB;
import static media.mexm.mydmam.entity.FileEntity.hashPath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.asset.DatabaseUpdateDirection;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.AssetSummaryDao;
import media.mexm.mydmam.repository.AssetSummaryRepository;
import media.mexm.mydmam.repository.FileRepository;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
@Service
public class MediaAssetServiceImpl implements MediaAssetService {

	@Autowired
	FileRepository fileRepository;
	@Autowired
	AssetSummaryRepository assetSummaryRepository;
	@Autowired
	AssetSummaryDao assetSummaryDao;

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

}
