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
package media.mexm.mydmam.indexer;

import static org.apache.lucene.document.LongField.newRangeQuery;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;

import org.apache.lucene.search.BooleanQuery;

public record SearchConstraintRange(boolean restricted,
									long min,
									long max) {

	public static final SearchConstraintRange NO_RANGE = new SearchConstraintRange(false, 0, 0);

	void apply(final BooleanQuery.Builder booleanQuery, final String field) {
		if (restricted == false) {
			return;
		}

		booleanQuery.add(newRangeQuery(field, min, max), MUST);
	}

}
