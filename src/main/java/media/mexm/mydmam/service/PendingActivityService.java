/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2025
 *
 */
package media.mexm.mydmam.service;

import java.util.Set;

import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import tv.hd3g.transfertfiles.FileAttributesReference;

public interface PendingActivityService {

	void applyActivities(String realmName,
						 String storageName,
						 PathIndexingRealm realm,
						 Set<? extends FileAttributesReference> files,
						 ActivityEventType eventType);

	void dispatchAssetActivities(String realmName,
								 String storageName,
								 MediaAsset asset,
								 String spoolName,
								 ActivityEventType eventType,
								 Set<Class<?>> previousHandlers);

	void cleanupFiles(String realmName,
					  String storageName,
					  PathIndexingRealm realm,
					  Set<? extends FileAttributesReference> losted);

}
