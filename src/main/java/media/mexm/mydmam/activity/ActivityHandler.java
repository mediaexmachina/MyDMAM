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
package media.mexm.mydmam.activity;

import static media.mexm.mydmam.dto.StorageStateClass.ONLINE;

import java.util.Set;

import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.dto.StorageStateClass;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;

/**
 * An ActivityHandler must be stateless
 */
public interface ActivityHandler {

	default boolean isEnabled() {
		return true;
	}

	default String getHandlerName() {
		return getClass().getSimpleName();
	}

	/**
	 * If empty => all is supported
	 */
	default Set<StorageStateClass> getSupportedStorageStateClasses() {
		return Set.of(ONLINE);
	}

	boolean canHandle(MediaAsset asset, ActivityEventType eventType, RealmStorageConfiguredEnv storedOn);

	HandlingResult handle(MediaAsset asset,
						  ActivityEventType eventType,
						  RealmStorageConfiguredEnv storedOn) throws Exception; // NOSONAR S112

}
