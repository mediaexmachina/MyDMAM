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
 * Copyright (C) hdsdi3g for hd3g.tv 2025
 *
 */
package media.mexm.mydmam.component;

import static media.mexm.mydmam.App.CONTROLLER_BASE_MAPPING_API_PATH;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * https://stackoverflow.com/questions/47564695/spring-boot-angular-entering-url-in-address-bar-results-in-404
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StaticResourcesRoutingFilter implements Filter {

	private final Set<String> staticResourcesNames;

	public StaticResourcesRoutingFilter() {
		staticResourcesNames = new HashSet<>();
		staticResourcesNames.add("/");
	}

	@Override
	public void doFilter(final ServletRequest request,
						 final ServletResponse response,
						 final FilterChain chain) throws IOException, ServletException {
		final var httpServletRequest = (HttpServletRequest) request;
		final var requestURI = httpServletRequest.getRequestURI();

		if (requestURI.startsWith(CONTROLLER_BASE_MAPPING_API_PATH)
			|| staticResourcesNames.contains(requestURI)) {
			chain.doFilter(request, response);
		} else {
			request.getRequestDispatcher("/").forward(request, response);
		}
	}

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		try {
			final var resolver = new PathMatchingResourcePatternResolver();
			Arrays.stream(resolver.getResources("classpath*:static/*"))
					.map(Resource::getFilename)
					.forEach(f -> staticResourcesNames.add("/" + f));
		} catch (final IOException e) {
			throw new ServletException("Can't load internal resources", e);
		}
	}

}