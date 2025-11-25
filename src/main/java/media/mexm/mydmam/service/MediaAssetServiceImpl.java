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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.entity.FileEntity;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
@Service
public class MediaAssetServiceImpl implements MediaAssetService {

	@Autowired
	Indexer indexer;

	/*
	 * NOPE *this*
	 */

	@Override
	public MediaAsset getFromWatchfolder(final String realmName,
										 final String storageName,
										 final FileAttributesReference file) {
		return new MediaAsset(this, realmName, storageName, file.getPath());
	}

	@Override
	public MediaAsset getFromFileEntry(final FileEntity file) {
		return new MediaAsset(this, file.getRealm(), file.getStorage(), file.getPath());
	}

	@Override
	public void purgeAssetArtefacts(final String realmName,
									final String storageName,
									final FileAttributesReference file) {
		/**
		 * TO BE IMPLEMENTED
		 */
	}

}
