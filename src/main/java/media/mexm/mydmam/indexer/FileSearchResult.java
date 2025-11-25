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

import static java.lang.Float.compare;

public record FileSearchResult(String hashPath,
							   String storage,
							   String name,
							   String parentPath,
							   float score,
							   String explain) implements
							  Comparable<FileSearchResult> {

	@Override
	public int compareTo(final FileSearchResult o) {
		return compare(o.score, score);
	}

}
