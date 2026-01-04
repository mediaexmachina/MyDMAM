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
package media.mexm.mydmam.dto;

public enum StorageStateClass {
	/**
	 * Content are directly accessible
	 */
	ONLINE,
	/**
	 * Content accessibility is limited by retrieval files before process
	 */
	NEARLINE,
	/**
	 * Content are not automatically accessible
	 */
	OFFLINE;
}
