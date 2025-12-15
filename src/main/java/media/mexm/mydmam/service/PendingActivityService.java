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
import media.mexm.mydmam.activity.PendingActivityJob;
import media.mexm.mydmam.configuration.RealmConf;
import tv.hd3g.transfertfiles.FileAttributesReference;

public interface PendingActivityService {

	void continueAssetActivity(PendingActivityJob pendingActivityJob);

	void startsActivities(String realmName,
						  String storageName,
						  RealmConf realm,
						  Set<? extends FileAttributesReference> files,
						  ActivityEventType eventType);

	void cleanupFiles(String realmName,
					  String storageName,
					  RealmConf realm,
					  Set<? extends FileAttributesReference> losted);

	void restartPendingActivities();
}
