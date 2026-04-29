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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Deprecated
public class MetadataThesaurusEntryImpl implements MetadataThesaurusEntry {

    private final String classifier;
    private final String key;
    private final Optional<String> value;

    private MetadataThesaurusEntryImpl(final String classifier, final String key, final Optional<String> value) {
        this.classifier = Objects.requireNonNull(classifier, "\"classifier\" can't to be null");
        this.key = Objects.requireNonNull(key, "\"key\" can't to be null");
        this.value = Objects.requireNonNull(value, "\"value\" can't to be null");
    }

    @Deprecated
    @Override
    public MetadataThesaurusEntry copyWithValue(final Optional<String> value) {
        return new MetadataThesaurusEntryImpl(classifier, key, value);
    }

    @Deprecated
    public MetadataThesaurusEntryImpl(final String classifier, final String parent, final String key) {
        requireNonNull(parent, "\"parent\" can't to be null");
        this(classifier, parent.isEmpty() ? key : parent + "." + key, empty());
    }

    @Deprecated
    @Override
    public String classifier() {
        return classifier;
    }

    @Deprecated
    @Override
    public String key() {
        return key;
    }

    @Deprecated
    @Override
    public Optional<String> value() {
        return value;
    }

    @Deprecated
    @Override
    public int intValue(final int defaultValue) {
        return value.map(Integer::parseInt).orElse(defaultValue);
    }

    @Deprecated
    @Override
    public Optional<String> get() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Optional<String> get(final int layer) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Map<Integer, String> getAll() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public Map<Integer, Integer> getAllInt() {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public int getAsInt(final int defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public int getAsInt(final int layer, final int defaultValue) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void set(final int layer, final Object value) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    public void set(final Object value) {
        throw new UnsupportedOperationException();
    }

}
