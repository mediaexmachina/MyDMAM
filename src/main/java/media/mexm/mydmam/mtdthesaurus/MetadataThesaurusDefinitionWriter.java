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

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class MetadataThesaurusDefinitionWriter<T> {

    private T instance;
    private final AtomicReference<WritedLayerValue> currentValue;

    public MetadataThesaurusDefinitionWriter() {
        currentValue = new AtomicReference<>();
    }

    public record WritedLayerValue(int layer, String value) {
    }

    public void setInstance(final T instance) {
        if (this.instance != null) {
            throw new IllegalArgumentException("Can't set twice instance");
        }
        this.instance = instance;
    }

    public Optional<WritedLayerValue> getAndRemoveCurrentValue() {
        final var result = Optional.ofNullable(currentValue.get());
        currentValue.set(null);
        return result;
    }

    public T set(final Object value) {
        return set(0, value);
    }

    public T set(final int layer, final Object value) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance is not set");
        }
        if (value == null) {
            currentValue.set(null);
        } else if (value instanceof final String s) {
            currentValue.set(new WritedLayerValue(layer, s));
        } else if (value instanceof final Duration d) {
            currentValue.set(new WritedLayerValue(layer, String.valueOf(d.toMillis())));
        } else if (value instanceof final Optional<?> o) {
            if (o.isPresent()) {
                return set(layer, o.get());
            } else {
                currentValue.set(null);
            }
        } else {
            currentValue.set(new WritedLayerValue(layer, String.valueOf(value)));
        }

        return instance;
    }

}
