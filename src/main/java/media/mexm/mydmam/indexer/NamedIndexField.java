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
package media.mexm.mydmam.indexer;

import java.util.Objects;
import java.util.Optional;

public class NamedIndexField {

	public static final String DOCUMENT_TYPE = "type";

	public static final NamedIndexField FILE = new NamedIndexField("file");
	public static final String DOCUMENT_TYPE_FILE = FILE.toString();
	public static final String FILE_STORAGE = new NamedIndexField(FILE, "storage").toString();
	public static final String FILE_DIRECTORY = new NamedIndexField(FILE, "directory").toString();
	public static final String FILE_HIDDEN = new NamedIndexField(FILE, "hidden").toString();
	public static final String FILE_LINK = new NamedIndexField(FILE, "link").toString();
	public static final String FILE_SPECIAL = new NamedIndexField(FILE, "special").toString();
	public static final String FILE_DATE = new NamedIndexField(FILE, "date").toString();
	public static final String FILE_LENGTH = new NamedIndexField(FILE, "length").toString();
	public static final String FILE_NAME = new NamedIndexField(FILE, "name").toString();
	public static final String FILE_BASE_NAME = new NamedIndexField(FILE, "baseName").toString();
	public static final String FILE_HASH_PATH = new NamedIndexField(FILE, "hashPath").toString();
	public static final String FILE_PARENT_PATH = new NamedIndexField(FILE, "parentPath").toString();
	public static final String FILE_PARENT_HASH_PATH = new NamedIndexField(FILE, "parentHashPath").toString();

	private final Optional<NamedIndexField> oParent;
	private final String name;

	public NamedIndexField(final NamedIndexField parent, final String name) {
		oParent = Optional.ofNullable(parent);
		this.name = Objects.requireNonNull(name, "\"name\" can't to be null");
	}

	public NamedIndexField(final String name) {
		oParent = Optional.empty();
		this.name = Objects.requireNonNull(name, "\"name\" can't to be null");
	}

	@Override
	public String toString() {
		if (oParent.isPresent()) {
			return oParent.get().toString() + "." + name;
		}
		return name;
	}

}
