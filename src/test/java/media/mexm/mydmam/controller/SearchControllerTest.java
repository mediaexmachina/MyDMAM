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

import static media.mexm.mydmam.indexer.SearchConstraintRange.NO_RANGE;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.rightPad;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.dto.OpenSearchResponse;
import media.mexm.mydmam.dto.SearchConstraintsRequest;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.indexer.FileSearchConstraints;
import media.mexm.mydmam.indexer.FileSearchResult;
import media.mexm.mydmam.indexer.RealmIndexer;
import media.mexm.mydmam.indexer.SearchConstraintCondition;
import media.mexm.mydmam.indexer.SearchResult;
import media.mexm.mydmam.repository.FileRepository;
import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class SearchControllerTest {
	static final Faker faker = net.datafaker.Faker.instance();

	private static final RequestMapping REQUEST_MAPPING = SearchController.class.getAnnotation(
			RequestMapping.class);
	private static final String BASE_MAPPING = REQUEST_MAPPING.value()[0];
	private static final ResultMatcher CONTENT_TYPE = content().contentType(REQUEST_MAPPING.produces()[0]);
	private static final ResultMatcher STATUS_OK = status().isOk();
	private static final ResultMatcher STATUS_UNPROCESSABLE_ENTITY = status().isUnprocessableEntity();

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper objectMapper;
	@Value("${mydmamConsts.searchResultMaxSize:100}")
	int searchResultMaxSize;

	@MockitoBean
	Indexer indexer;
	@MockitoBean
	FileRepository fileRepository;
	@Mock
	RealmIndexer realmIndexer;
	@Mock
	FileEntity fileEntity;

	@Fake
	String realm;
	@Fake
	String q;
	@Fake
	int totalFounded;
	@Fake
	String hashPath;
	@Fake
	String storage;
	@Fake
	String name;
	@Fake
	String parentPath;
	@Fake
	float score;
	@Fake
	String explain;

	HttpHeaders baseHeaders;
	FileSearchResult foundedFile;
	SearchResult searchResult;
	int limit;
	SearchConstraintsRequest constraintsRequest;
	FileSearchConstraints fileConstraints;

	@BeforeEach
	void init() {
		baseHeaders = new HttpHeaders();
		baseHeaders.setContentType(APPLICATION_JSON);
		foundedFile = new FileSearchResult(hashPath, storage, name, parentPath, score, explain);
		searchResult = new SearchResult(List.of(foundedFile), totalFounded);
		limit = searchResultMaxSize / 2;
		q = rightPad(leftPad(q, faker.number().numberBetween(1, 10)), faker.number().numberBetween(1, 10));

		fileConstraints = new FileSearchConstraints(
				faker.options().option(SearchConstraintCondition.class),
				faker.options().option(SearchConstraintCondition.class),
				faker.options().option(SearchConstraintCondition.class),
				faker.options().option(SearchConstraintCondition.class),
				NO_RANGE,
				NO_RANGE,
				List.of(faker.numerify("storage###")),
				parentPath,
				faker.numerify("parentHashPath###"));
		constraintsRequest = new SearchConstraintsRequest(fileConstraints);

		when(indexer.getIndexerByRealm(realm)).thenReturn(Optional.ofNullable(realmIndexer));
		when(realmIndexer.openSearch(any(), any(), anyInt())).thenReturn(searchResult);
		when(fileRepository.getByHashPath(anySet())).thenReturn(Set.of(fileEntity));

		when(fileEntity.getRealm()).thenReturn(realm);
		when(fileEntity.getStorage()).thenReturn(faker.numerify("storage###"));
		when(fileEntity.isDirectory()).thenReturn(faker.bool().bool());
		when(fileEntity.getHashPath()).thenReturn(hashPath);
		when(fileEntity.getPath()).thenReturn(faker.numerify("path###"));
		when(fileEntity.getModified()).thenReturn(new Timestamp(System.currentTimeMillis()));
		when(fileEntity.isWatchMarkedAsDone()).thenReturn(true);
		when(fileEntity.getLength()).thenReturn(faker.number().randomNumber());
	}

	@AfterEach
	void ends() {
		reset(fileEntity);
		verifyNoMoreInteractions(indexer, fileRepository);
	}

	@Test
	void testOpenSearch_badRealm() throws Exception {
		when(indexer.getIndexerByRealm(realm)).thenReturn(Optional.empty());

		mvc.perform(get(BASE_MAPPING + "/" + realm)
				.headers(baseHeaders)
				.queryParam("q", q))
				.andExpect(STATUS_UNPROCESSABLE_ENTITY);

		verify(indexer, times(1)).getIndexerByRealm(realm);
	}

	@Test
	void testOpenSearch_simple() throws Exception {
		final var content = mvc.perform(get(BASE_MAPPING + "/" + realm)
				.headers(baseHeaders)
				.queryParam("q", q))
				.andExpect(STATUS_OK)
				.andExpect(CONTENT_TYPE)
				.andExpect(jsonPath("$.q").exists())
				.andReturn()
				.getResponse()
				.getContentAsString();

		final var response = objectMapper.readValue(content, OpenSearchResponse.class);
		assertThat(response.limit()).isEqualTo(searchResultMaxSize);
		assertThat(response.q()).isEqualTo(q.trim());
		assertThat(response.relatedFiles()).isEmpty();
		assertNull(response.constraints());
		assertThat(response.result()).isEqualTo(searchResult);

		verify(indexer, times(1)).getIndexerByRealm(realm);
		verify(realmIndexer, times(1)).openSearch(q.trim(), Optional.empty(), searchResultMaxSize);
	}

	@Test
	void testOpenSearch_limited() throws Exception {
		final var content = mvc.perform(get(BASE_MAPPING + "/" + realm)
				.headers(baseHeaders)
				.queryParam("q", q)
				.queryParam("limit", String.valueOf(limit)))
				.andExpect(STATUS_OK)
				.andExpect(CONTENT_TYPE)
				.andExpect(jsonPath("$.q").exists())
				.andReturn()
				.getResponse()
				.getContentAsString();

		final var response = objectMapper.readValue(content, OpenSearchResponse.class);
		assertThat(response.limit()).isEqualTo(limit);
		assertThat(response.q()).isEqualTo(q.trim());
		assertThat(response.relatedFiles()).isEmpty();
		assertNull(response.constraints());
		assertThat(response.result()).isEqualTo(searchResult);

		verify(indexer, times(1)).getIndexerByRealm(realm);
		verify(realmIndexer, times(1)).openSearch(q.trim(), Optional.empty(), limit);
	}

	@Test
	void testOpenSearch_resolveHashPaths() throws Exception {
		final var content = mvc.perform(get(BASE_MAPPING + "/" + realm)
				.headers(baseHeaders)
				.queryParam("q", q)
				.queryParam("resolveHashPaths", "1"))
				.andExpect(STATUS_OK)
				.andExpect(CONTENT_TYPE)
				.andExpect(jsonPath("$.q").exists())
				.andReturn()
				.getResponse()
				.getContentAsString();

		final var response = objectMapper.readValue(content, OpenSearchResponse.class);
		assertThat(response.limit()).isEqualTo(searchResultMaxSize);
		assertThat(response.q()).isEqualTo(q.trim());
		assertNull(response.constraints());
		assertThat(response.result()).isEqualTo(searchResult);
		assertThat(response.relatedFiles()).size().isEqualTo(1);
		assertThat(response.relatedFiles()).containsKey(hashPath);
		assertThat(response.relatedFiles().get(hashPath).hashPath()).isEqualTo(hashPath);

		verify(indexer, times(1)).getIndexerByRealm(realm);
		verify(realmIndexer, times(1)).openSearch(q.trim(), Optional.empty(), searchResultMaxSize);
		verify(fileRepository, times(1)).getByHashPath(Set.of(hashPath));
	}

	@Test
	void testOpenSearch_constraints() throws Exception {
		final var content = mvc.perform(get(BASE_MAPPING + "/" + realm)
				.headers(baseHeaders)
				.queryParam("q", q)
				.content(objectMapper.writeValueAsString(constraintsRequest)))
				.andExpect(STATUS_OK)
				.andExpect(CONTENT_TYPE)
				.andExpect(jsonPath("$.q").exists())
				.andReturn()
				.getResponse()
				.getContentAsString();

		final var response = objectMapper.readValue(content, OpenSearchResponse.class);
		assertThat(response.limit()).isEqualTo(searchResultMaxSize);
		assertThat(response.q()).isEqualTo(q.trim());
		assertThat(response.relatedFiles()).isEmpty();
		assertThat(response.constraints()).isEqualTo(constraintsRequest);
		assertThat(response.result()).isEqualTo(searchResult);

		verify(indexer, times(1)).getIndexerByRealm(realm);
		verify(realmIndexer, times(1)).openSearch(q.trim(), Optional.ofNullable(fileConstraints), searchResultMaxSize);
	}

	@Test
	void testOpenSearch_empty() throws Exception {
		searchResult = new SearchResult(List.of(), 0);
		when(realmIndexer.openSearch(any(), any(), anyInt())).thenReturn(searchResult);

		final var content = mvc.perform(get(BASE_MAPPING + "/" + realm)
				.headers(baseHeaders)
				.queryParam("q", q))
				.andExpect(STATUS_OK)
				.andExpect(CONTENT_TYPE)
				.andExpect(jsonPath("$.q").exists())
				.andReturn()
				.getResponse()
				.getContentAsString();

		final var response = objectMapper.readValue(content, OpenSearchResponse.class);
		assertThat(response.limit()).isEqualTo(searchResultMaxSize);
		assertThat(response.q()).isEqualTo(q.trim());
		assertThat(response.relatedFiles()).isEmpty();
		assertNull(response.constraints());
		assertThat(response.result()).isEqualTo(searchResult);

		verify(indexer, times(1)).getIndexerByRealm(realm);
		verify(realmIndexer, times(1)).openSearch(q.trim(), Optional.empty(), searchResultMaxSize);
	}

	@Test
	void testReset() throws Exception {
		mvc.perform(post(BASE_MAPPING + "/reset-all-indexes")
				.headers(baseHeaders))
				.andExpect(STATUS_OK);

		verify(indexer, times(1)).reset("admin-ops");
	}

}
