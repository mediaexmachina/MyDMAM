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
import java.util.function.BiConsumer;

import media.mexm.mydmam.asset.DeclaredRenderedFile;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.AssetTextExtractedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.RelativePathProvider;
import tv.hd3g.transfertfiles.FileAttributesReference;

public interface MediaAssetService {

    String MEDIA_ASSET_AUDIT_ISSUER = "media-asset";
    String FULL_TEXT_PDF = "full-text-pdf.txt";

    FileEntity getFromWatchfolder(String realmName,
                                  String storageName,
                                  FileAttributesReference file);

    void purgeAssetArtefacts(String realmName, String storageName, FileAttributesReference file);

    String getRelativePath(FileEntity fileEntity, RelativePathProvider relativePathProvider);

    File getAbsolutePath(FileEntity fileEntity, RelativePathProvider relativePathProvider);

    void declareRenderedStaticFile(FileEntity fileEntity,
                                   DeclaredRenderedFile renderedFile) throws IOException;

    void declareRenderedStaticFile(FileEntity fileEntity,
                                   File workingFile,
                                   String name,
                                   boolean toGzip,
                                   int index,
                                   String previewType) throws IOException;

    void updateIndexer(FileEntity fileEntity);

    void declareTextExtractedFile(FileEntity fileEntity,
                                  File workingTextFile,
                                  String name) throws IOException;

    void forEachTextExtractedFile(FileEntity fileEntity,
                                  BiConsumer<AssetTextExtractedFileEntity, String> onTextExtracted);

    File getPhysicalRenderedFile(FileEntity fileEntity, AssetRenderedFileEntity assetRenderedFileEntity, String realm);

    Collection<FileEntity> resetDetectedMetadatas(Collection<FileEntity> assetsToReset);

}
