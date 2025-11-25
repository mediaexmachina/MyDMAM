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

import static media.mexm.mydmam.indexer.NamedIndexField.FILE_DATE;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_DIRECTORY;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_HIDDEN;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_LENGTH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_LINK;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_PARENT_HASH_PATH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_PARENT_PATH;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_SPECIAL;
import static media.mexm.mydmam.indexer.NamedIndexField.FILE_STORAGE;
import static media.mexm.mydmam.indexer.SearchConstraintCondition.IGNORE;
import static org.apache.lucene.document.IntField.newExactQuery;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

public record FileSearchConstraints(@NotNull SearchConstraintCondition directory,
									@NotNull SearchConstraintCondition hidden,
									@NotNull SearchConstraintCondition link,
									@NotNull SearchConstraintCondition special,
									@NotNull SearchConstraintRange date,
									@NotNull SearchConstraintRange size,
									@NotNull List<String> storages,
									@Nullable String parentPath,
									@Nullable String parentHashPath) {

	void apply(final BooleanQuery.Builder booleanQuery) {
		if (directory != IGNORE) {
			booleanQuery.add(newExactQuery(FILE_DIRECTORY, directory.getIndexedValue()), MUST);
		}
		if (hidden != IGNORE) {
			booleanQuery.add(newExactQuery(FILE_HIDDEN, hidden.getIndexedValue()), MUST);
		}
		if (link != IGNORE) {
			booleanQuery.add(newExactQuery(FILE_LINK, link.getIndexedValue()), MUST);
		}
		if (special != IGNORE) {
			booleanQuery.add(newExactQuery(FILE_SPECIAL, special.getIndexedValue()), MUST);
		}
		date.apply(booleanQuery, FILE_DATE);
		size.apply(booleanQuery, FILE_LENGTH);

		if (storages != null && storages.isEmpty() == false) {
			final var storageQuery = new BooleanQuery.Builder();
			storages.forEach(storage -> storageQuery.add(new TermQuery(new Term(FILE_STORAGE, storage)), SHOULD));
			booleanQuery.add(storageQuery.build(), MUST);
		}

		if (parentPath != null && parentPath.isEmpty() == false) {
			booleanQuery.add(new PrefixQuery(new Term(FILE_PARENT_PATH, parentPath)), MUST);
		} else if (parentHashPath != null && parentHashPath.isEmpty() == false) {
			booleanQuery.add(new TermQuery(new Term(FILE_PARENT_HASH_PATH, parentHashPath)), MUST);
		}
	}

}
