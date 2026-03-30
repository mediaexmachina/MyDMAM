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
package media.mexm.mydmam.mtdthesaurus;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;

import java.util.Optional;

public record MetadataThesaurusEntry(String classifier, String key, Optional<String> value) {

    public MetadataThesaurusEntry {
        requireNonNull(classifier, "\"classifier\" can't to be null");
        requireNonNull(key, "\"key\" can't to be null");
        requireNonNull(value, "\"value\" can't to be null");
    }

    public MetadataThesaurusEntry(final String classifier, final String parent, final String key) {
        requireNonNull(parent, "\"parent\" can't to be null");
        this(classifier, parent.isEmpty() ? key : parent + "." + key, empty());
    }

    MetadataThesaurusEntry copyWithValue(final Optional<String> value) {
        return new MetadataThesaurusEntry(classifier, key, value);
    }

    public int intValue(final int defaultValue) {
        return value.map(Integer::parseInt).orElse(defaultValue);
    }

}
