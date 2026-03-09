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
package media.mexm.mydmam.tools;

import static org.apache.commons.io.FilenameUtils.wildcardMatch;
import static org.apache.commons.io.IOCase.INSENSITIVE;

import java.util.List;
import java.util.Optional;

public record AllowBlockLists(
							  /** Empty/null list allows all, except in block list. You can use "*" and "?" wildcards. */
							  List<String> allows,
							  /** Empty/null list blocks nothing, only in allow list. You can use "*" and "?" wildcards. */
							  List<String> blocks,
							  /** By default (false), allow checks before block checks */
							  boolean blockBeforeAllow) {

	public boolean pass(final String candidate) {
		final var allowsList = Optional.ofNullable(allows).orElse(List.of());

		final boolean allow;
		if (allowsList.isEmpty()) {
			allow = true;
		} else {
			allow = allowsList.stream()
					.anyMatch(entry -> wildcardMatch(candidate, entry, INSENSITIVE));
		}

		final var block = Optional.ofNullable(blocks)
				.orElse(List.of())
				.stream()
				.anyMatch(entry -> wildcardMatch(candidate, entry, INSENSITIVE));

		if (blockBeforeAllow) {
			if (block) {
				return false;
			}
			return allow;
		} else {
			if (allowsList.isEmpty() == false) {
				return allow;
			}
			return block == false;
		}
	}

}
