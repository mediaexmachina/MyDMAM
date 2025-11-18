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

import java.util.Objects;

import org.apache.commons.io.FilenameUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.service.MediaAssetService;

@Slf4j
public class MediaAsset {

	@Getter
	private final MediaAssetService service;
	@Getter
	private final String realmName;
	@Getter
	private final String storageName;
	@Getter
	private final String path;

	private String hashPath;
	private String name;

	public MediaAsset(final MediaAssetService service,
					  final String realmName,
					  final String storageName,
					  final String path) {
		this.service = Objects.requireNonNull(service, "\"service\" can't to be null");
		this.realmName = Objects.requireNonNull(realmName, "\"realmName\" can't to be null");
		this.storageName = Objects.requireNonNull(storageName, "\"storageName\" can't to be null");
		this.path = Objects.requireNonNull(path, "\"path\" can't to be null");
	}

	public synchronized String getHashPath() {
		if (hashPath == null) {
			hashPath = FileEntity.hashPath(realmName, storageName, path);
		}
		return hashPath;
	}

	public synchronized String getName() {
		if (name == null) {
			name = FilenameUtils.getName(path);
		}
		return name;
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("realmName=");
		builder.append(realmName);
		builder.append(", storageName=");
		builder.append(storageName);
		builder.append(", path=\"");
		builder.append(path);
		builder.append("\"");
		return builder.toString();
	}

}
