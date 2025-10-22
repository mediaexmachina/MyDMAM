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
package media.mexm.mydmam.controller;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import media.mexm.mydmam.controller.PingController.PingRequest;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockToolsExtendsJunit.class)
class PingControllerTest {

	private static final RequestMapping REQUEST_MAPPING = PingController.class.getAnnotation(RequestMapping.class);
	private static final String BASE_MAPPING = REQUEST_MAPPING.value()[0];
	private static final ResultMatcher CONTENT_TYPE = content().contentType(REQUEST_MAPPING.produces()[0]);
	private static final ResultMatcher STATUS_OK = status().isOk();
	private static final ResultMatcher STATUS_BAD_REQUEST = status().isBadRequest();

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;

	@Mock
	HttpServletRequest request;
	@Fake(min = 5, max = 10)
	String payload;

	HttpHeaders baseHeaders;

	@BeforeEach
	void init() throws Exception {
		openMocks(this).close();
		baseHeaders = new HttpHeaders();
	}

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(request);
	}

	@Test
	void testPostPing() throws Exception {
		baseHeaders.setContentType(APPLICATION_JSON);

		final var pRequest = new PingRequest(payload);

		mvc.perform(post(BASE_MAPPING)
				.headers(baseHeaders)
				.content(objectMapper.writeValueAsString(pRequest)))
				.andExpect(STATUS_OK)
				.andExpect(CONTENT_TYPE)
				.andExpect(jsonPath("$.payload").value("pong: " + payload.toUpperCase()));
	}

	@Test
	void testPostInvalidPing() throws Exception {
		baseHeaders.setContentType(APPLICATION_JSON);

		mvc.perform(post(BASE_MAPPING)
				.headers(baseHeaders))
				.andExpect(STATUS_BAD_REQUEST);
	}

	@Test
	void testGetFail() throws Exception {
		mvc.perform(get(BASE_MAPPING + "/fail"))
				.andExpect(status().isIAmATeapot())
				.andExpect(CONTENT_TYPE)
				.andExpect(jsonPath("$.why").value("Fail!"));
	}

}
