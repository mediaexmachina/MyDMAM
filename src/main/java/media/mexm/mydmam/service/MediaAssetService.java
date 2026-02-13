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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import media.mexm.mydmam.asset.DatabaseUpdateDirection;
import media.mexm.mydmam.asset.DeclaredRenderedFile;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
import tv.hd3g.transfertfiles.FileAttributesReference;

public interface MediaAssetService {

	MediaAsset getFromWatchfolder(String realmName,
								  String storageName,
								  FileAttributesReference file,
								  MediaAssetService injectedService);

	MediaAsset getFromFileEntry(FileEntity file, MediaAssetService injectedService);

	void purgeAssetArtefacts(String realmName, String storageName, FileAttributesReference file);

	String updateMimeType(MediaAsset asset, DatabaseUpdateDirection direction);

	Map<AssetRenderedFileEntity, File> declareRenderedStaticFiles(MediaAsset asset,
																  Collection<DeclaredRenderedFile> declaredRenderedFiles) throws IOException;

	Set<AssetRenderedFileEntity> getAllRenderedFiles(String fileHashpath, String realm);

	File getPhysicalRenderedFile(AssetRenderedFileEntity assetRenderedFileEntity, String realm);

	Collection<MediaAsset> resetDetectedMetadatas(Collection<MediaAsset> assetsToReset,
												  MediaAssetService injectedService);

}
