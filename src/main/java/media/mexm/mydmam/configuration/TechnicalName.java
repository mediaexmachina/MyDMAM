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
package media.mexm.mydmam.configuration;

import static java.text.Normalizer.normalize;
import static java.text.Normalizer.Form.NFKD;
import static java.util.Objects.requireNonNull;
import static media.mexm.mydmam.App.REPLACE_NORMALIZED;
import static media.mexm.mydmam.entity.FileEntity.MAX_NAME_SIZE;

import java.util.regex.Pattern;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.NotEmpty;

@Validated
public record TechnicalName(@JsonValue @NotEmpty String name) {

	private static final Pattern REMOVE_NON_VALID_CHARS = Pattern.compile("[^a-z^A-Z^0-9^\\-^\\_]+"); // NOSONAR S5869

	public TechnicalName {
		requireNonNull(name, "You must set name");
		final var rawname = name.trim();
		if (rawname.isEmpty()) {
			throw new IllegalArgumentException("TechnicalName can't be empty");
		}
		final var normalized = REPLACE_NORMALIZED.matcher(normalize(rawname, NFKD)).replaceAll("");
		final var normalizedName = REMOVE_NON_VALID_CHARS.matcher(normalized).replaceAll("_");
		name = normalizedName.substring(0, Math.min(normalizedName.length(), MAX_NAME_SIZE));
	}

	@Override
	public final String toString() {
		return name;
	}

}
