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
package media.mexm.mydmam.activity.component;

import static media.mexm.mydmam.asset.FileMetadataResolutionTrait.MTD_TECHNICAL_CLASSIFIER;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.HandlingResult;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.asset.MetadataExtractorHandler;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.tools.ImageMagick;

@Component
@Slf4j
public class ImageInfoExtractionActivity implements MetadataExtractorHandler {

	private static final String PREVIEW_TYPE = "image-format";

	@Autowired
	ImageMagick imageMagick;
	@Autowired
	MimeTypeDetector mimeTypeDetector;

	@Override
	public Set<String> getManagedMimeTypes() {
		return Set.of(
				"image/jpeg",
				"image/png",
				"image/bmp",
				"image/gif",
				"image/vnd.adobe.photoshop",
				"image/tiff",
				"application/postscript",
				"image/jp2",
				"application/dicom",
				"image/x-icon",
				"image/pict",
				"image/vndwapwbmp",
				"image/x-pcx",
				"image/x-portable-bitmap",
				"image/x-xbm",
				"image/xpm",
				"image/cineon",
				"image/dpx",
				"image/tga",
				"image/exr",
				"image/vnd.radiance",
				"image/webp",
				"image/sgi",
				"image/x-palm-pixmap",
				"image/x-g3-fax",
				"image/jpcd",
				"image/x-sct",
				"image/jbig",
				"image/x-miff",
				"image/x-sun");
	}

	@Override
	public Set<String> getProducedPreviewTypes() {
		return Set.of(PREVIEW_TYPE);
	}

	@Override
	public String getMetadataOriginName() {
		return "imagemagick";
	}

	@Override
	public boolean isEnabled() {
		return imageMagick.isEnabled();
	}

	@Override
	public boolean canHandle(final MediaAsset asset,
							 final ActivityEventType eventType,
							 final RealmStorageConfiguredEnv storedOn) {
		return storedOn.isDAS()
			   && storedOn.haveWorkingDir()
			   && storedOn.haveRenderedDir()
			   && canHandleMimeType(asset);
	}

	@Override
	public HandlingResult handle(final MediaAsset asset,
								 final ActivityEventType eventType,
								 final RealmStorageConfiguredEnv storedOn) throws Exception {
		final var assetFile = asset.getLocalInternalFile(storedOn.storage());
		final var workingFile = makeWorkingFile("identify.json", asset, storedOn);

		final var jsonNode = imageMagick.extractIdentifyJsonFile(
				assetFile,
				workingFile);

		final var version = jsonNode.read("$.version", String.class).orElse("<unset>");
		if (version.equals("1.0") == false) {
			throw new IllegalArgumentException("Can't support JSON version " + version);
		}

		asset.declareRenderedStaticFile(workingFile, "identify.json", true, mimeTypeDetector, 0, PREVIEW_TYPE);

		jsonNode.read("$.image.mimeType", String.class)
				.ifPresent(mimeType -> asset.setMimeType(this, mimeType));

		final var width = jsonNode.read("$.image.geometry.width", Integer.class).orElse(0);
		final var height = jsonNode.read("$.image.geometry.height", Integer.class).orElse(0);
		final var colorspace = jsonNode.read("$.image.colorspace", String.class).orElse(null);
		final var orientation = jsonNode.read("$.image.orientation", String.class).orElse(null);
		final var type = jsonNode.read("$.image.type", String.class).map(String::toLowerCase).orElse(null);

		final var entries = Map.of(
				"colorspace", colorspace,
				"orientation", orientation,
				"type", type);

		asset.setResolution(this, width, height);
		asset.createFileMetadataEntry(this, MTD_TECHNICAL_CLASSIFIER, 0, entries);
		log.debug("Found properties for {}: {}×{}; {}", asset, width, height, entries);

		return new HandlingResult();
	}

}
