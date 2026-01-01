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
package media.mexm.mydmam.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.tools.SortOrder;

public record FileSort(SortOrder name,
					   SortOrder type,
					   SortOrder date,
					   SortOrder size) {
	// TODO test

	public List<Order> makeOrderBy(final Root<FileEntity> root, final CriteriaBuilder criteriaBuilder) {
		return Stream.of(
				type.applyOrder("directory", root, criteriaBuilder),
				date.applyOrder("modified", root, criteriaBuilder),
				size.applyOrder("length", root, criteriaBuilder),
				name.applyOrder("path", root, criteriaBuilder))
				.flatMap(Optional::stream)
				.toList();
	}

}
