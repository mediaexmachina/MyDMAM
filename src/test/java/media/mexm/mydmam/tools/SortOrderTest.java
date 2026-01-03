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

import static org.assertj.core.api.Assertions.assertThat;
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
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class SortOrderTest {

	@Fake
	String attributeName;
	@Mock
	Root<Object> root;
	@Mock
	CriteriaBuilder criteriaBuilder;
	@Mock
	Order order;
	@Mock
	Path<Object> path;

	@BeforeEach
	void init() {
		when(criteriaBuilder.asc(any())).thenReturn(order);
		when(criteriaBuilder.desc(any())).thenReturn(order);
		when(root.get(anyString())).thenReturn(path);
	}

	@Test
	void testApplyOrder() {
		var result = SortOrder.none.applyOrder(attributeName, root, criteriaBuilder);
		assertThat(result).isEmpty();

		result = SortOrder.asc.applyOrder(attributeName, root, criteriaBuilder);
		assertThat(result).contains(order);

		result = SortOrder.desc.applyOrder(attributeName, root, criteriaBuilder);
		assertThat(result).contains(order);

		verify(root, times(2)).get(attributeName);
		verify(criteriaBuilder, times(1)).asc(path);
		verify(criteriaBuilder, times(1)).desc(path);
	}

}
