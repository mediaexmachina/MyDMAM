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

import java.util.List;

import media.mexm.mydmam.asset.MediaAsset;

public interface FileIOActivityHander {

	default List<ActivityHander> onFoundNewAsset(final MediaAsset asset) {
		return List.of();
	}

	default List<ActivityHander> onUpdateAsset(final MediaAsset asset) {
		return List.of();
	}

	default List<ActivityHander> onDeleteAsset(final MediaAsset asset) {
		return List.of();
	}

}
