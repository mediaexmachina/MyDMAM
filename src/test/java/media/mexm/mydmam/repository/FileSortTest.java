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
package media.mexm.mydmam.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.tools.SortOrder;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class FileSortTest {

	@Mock
	Root<FileEntity> root;
	@Mock
	CriteriaBuilder criteriaBuilder;
	@Mock
	Order order;
	@Mock
	Path<Object> path;

	SortOrder name = SortOrder.asc;
	SortOrder type = SortOrder.desc;
	SortOrder date = SortOrder.asc;
	SortOrder size = SortOrder.none;

	FileSort fs;

	@BeforeEach
	void init() {
		fs = new FileSort(name, type, date, size);

		when(criteriaBuilder.asc(any())).thenReturn(order);
		when(criteriaBuilder.desc(any())).thenReturn(order);
		when(root.get(anyString())).thenReturn(path);
	}

	@Test
	void testMakeOrderBy() {
		final var list = fs.makeOrderBy(root, criteriaBuilder);
		assertThat(list).size().isEqualTo(3);
		assertEquals(list.get(0), order);
		assertEquals(list.get(1), order);
		assertEquals(list.get(2), order);

		verify(criteriaBuilder, times(2)).asc(path);
		verify(criteriaBuilder, times(1)).desc(path);
		verify(root, times(1)).get("path");
		verify(root, times(1)).get("directory");
		verify(root, times(1)).get("modified");
	}

}
