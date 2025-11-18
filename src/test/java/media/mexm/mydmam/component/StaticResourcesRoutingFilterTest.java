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
package media.mexm.mydmam.component;

import static media.mexm.mydmam.App.CONTROLLER_BASE_MAPPING_API_PATH;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class StaticResourcesRoutingFilterTest {

	@Mock
	HttpServletRequest httpServletRequest;
	@Mock
	ServletResponse response;
	@Mock
	FilterChain chain;
	@Mock
	FilterConfig filterConfig;
	@Mock
	RequestDispatcher requestDispatcher;

	@Fake
	String resourceName;

	StaticResourcesRoutingFilter f;

	@BeforeEach
	void init() {
		f = new StaticResourcesRoutingFilter();
	}

	@Test
	void testDoFilter_api() throws IOException, ServletException {
		when(httpServletRequest.getRequestURI()).thenReturn(CONTROLLER_BASE_MAPPING_API_PATH + "/" + resourceName);

		f.doFilter(httpServletRequest, response, chain);

		verify(chain, times(1)).doFilter(httpServletRequest, response);
		verify(httpServletRequest, atLeastOnce()).getRequestURI();
	}

	@Test
	void testDoFilter_static() throws IOException, ServletException {
		when(httpServletRequest.getRequestURI()).thenReturn("/");

		f.doFilter(httpServletRequest, response, chain);

		verify(chain, times(1)).doFilter(httpServletRequest, response);
		verify(httpServletRequest, atLeastOnce()).getRequestURI();
	}

	@Test
	void testDoFilter_other() throws IOException, ServletException {
		when(httpServletRequest.getRequestURI()).thenReturn(resourceName);
		when(httpServletRequest.getRequestDispatcher("/")).thenReturn(requestDispatcher);

		f.doFilter(httpServletRequest, response, chain);

		verify(httpServletRequest, atLeastOnce()).getRequestURI();
		verify(httpServletRequest, times(1)).getRequestDispatcher("/");
		verify(requestDispatcher, times(1)).forward(httpServletRequest, response);
	}

	@Test
	void testInit() throws ServletException { // NOSONAR S2699
		f.init(filterConfig);
	}

}
