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
package media.mexm.mydmam.controller;

import static media.mexm.mydmam.activity.ActivityEventType.MANUAL_RESET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.dto.ResetActivitiesRequest;
import media.mexm.mydmam.service.PendingActivityService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class AssetControllerTest {

	private static final RequestMapping REQUEST_MAPPING = AssetController.class.getAnnotation(
			RequestMapping.class);
	private static final String BASE_MAPPING = REQUEST_MAPPING.value()[0];
	private static final ResultMatcher ACCEPTED = status().isAccepted();

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	FlatJobKitEngine flatJobKitEngine;

	@MockitoBean
	PendingActivityService pendingActivityService;

	@Fake
	String realm;
	@Fake
	boolean recursive;
	@Fake
	String hashPath;

	HttpHeaders baseHeaders;
	ResetActivitiesRequest request;
	String content;

	@BeforeEach
	void init() throws JsonProcessingException {
		baseHeaders = new HttpHeaders();
		baseHeaders.setContentType(APPLICATION_JSON);

		request = new ResetActivitiesRequest(Set.of(hashPath), recursive);
		content = objectMapper.writeValueAsString(request);
	}

	@Test
	void testResetActivities() throws Exception {
		mvc.perform(post(BASE_MAPPING + "/reset-activities/" + realm)
				.headers(baseHeaders)
				.content(content))
				.andExpect(ACCEPTED);

		verify(pendingActivityService, times(1))
				.startsActivities(realm, Set.of(hashPath), recursive, MANUAL_RESET);
	}

	@Test
	void testResetActivities_jobError() throws Exception {
		doThrow(RuntimeException.class).when(pendingActivityService)
				.startsActivities(anyString(), anySet(), anyBoolean(), any(ActivityEventType.class));

		mvc.perform(post(BASE_MAPPING + "/reset-activities/" + realm)
				.headers(baseHeaders)
				.content(content))
				.andExpect(ACCEPTED);

		verify(pendingActivityService, times(1))
				.startsActivities(realm, Set.of(hashPath), recursive, MANUAL_RESET);

		final var endEventsList = flatJobKitEngine.getEndEventsList();
		assertThat(endEventsList).isNotEmpty();
		endEventsList.clear();
	}

	@AfterEach
	void ends() {
		assertThat(flatJobKitEngine.getEndEventsList()).isEmpty();
		assertThat(flatJobKitEngine.isEmptyActiveServicesList()).isTrue();
	}

}
