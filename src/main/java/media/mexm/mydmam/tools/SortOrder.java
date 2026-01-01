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
 * Copyright (C) hdsdi3g for hd3g.tv 2026
 *
 */
package media.mexm.mydmam.tools;

import java.util.Optional;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

public enum SortOrder {
	none, // NOSONAR S115
	asc, // NOSONAR S115
	desc; // NOSONAR S115

	public <T> Optional<Order> applyOrder(final String attributeName,
										  final Root<T> root,
										  final CriteriaBuilder criteriaBuilder) { // TODO test
		if (equals(asc)) {
			return Optional.ofNullable(criteriaBuilder.asc(root.get(attributeName)));
		} else if (equals(desc)) {
			return Optional.ofNullable(criteriaBuilder.desc(root.get(attributeName)));
		}
		return Optional.empty();
	}

}
