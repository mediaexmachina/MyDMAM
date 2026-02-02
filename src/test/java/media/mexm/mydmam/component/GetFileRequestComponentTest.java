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
package media.mexm.mydmam.component;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMinutes;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static net.datafaker.Faker.instance;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.ContentDisposition.attachment;
import static org.springframework.http.HttpHeaders.ACCEPT_RANGES;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_RANGE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PARTIAL_CONTENT;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.test.context.ActiveProfiles;

import media.mexm.mydmam.component.GetFileRequestComponent.GetFileRequest;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class GetFileRequestComponentTest {

	@Fake
	String contentType;

	File fileToSend;
	String content;
	String etag;
	String contentEncoded;

	long lastModified;
	String downloadedFileName;
	CacheControl cacheControl;

	@Autowired
	GetFileRequestComponent gfrc;

	@BeforeEach
	void init() throws IOException {
		content = instance().lorem().paragraph(20);
		fileToSend = File.createTempFile("mydmam-" + getClass().getSimpleName(), "");
		write(fileToSend, content, UTF_8, false);
		contentType = contentType.toLowerCase();
		contentType = contentType + "/" + contentType;

		lastModified = System.currentTimeMillis();
		etag = randomUUID().toString();
		contentEncoded = randomUUID().toString();
		downloadedFileName = randomUUID().toString();

		cacheControl = maxAge(ofMinutes(1)).noTransform().cachePrivate().mustRevalidate();
	}

	@AfterEach
	void ends() {
		deleteQuietly(fileToSend);
	}

	@Test
	void testMakeResponseEntity() throws IOException {
		final var response = gfrc.makeResponseEntity(new GetFileRequest(
				fileToSend,
				GET,
				null,
				null,
				etag,
				contentType,
				contentEncoded,
				empty()));

		assertEquals(OK.value(),
				response.getStatusCode().value());

		final var headers = response.getHeaders();
		assertEquals("\"" + etag + "\"", headers.getETag());
		assertEquals(cacheControl.getHeaderValue(), headers.getCacheControl());
		assertEquals(contentType, headers.getContentType().toString());
		assertEquals(contentEncoded, headers.get(CONTENT_ENCODING).get(0));
		assertEquals("bytes", headers.get(ACCEPT_RANGES).get(0));
		assertEquals(fileToSend.length(), headers.getContentLength());
		assertEquals(ContentDisposition.empty(), headers.getContentDisposition());

		final var body = response.getBody();
		final var baos = new ByteArrayOutputStream((int) fileToSend.length());
		body.writeTo(baos);
		assertThat(baos.toString(UTF_8)).isEqualTo(content);
	}

	@Test
	void testMakeResponseEntity_ifNoneMatch() {
		final var response = gfrc.makeResponseEntity(new GetFileRequest(
				fileToSend,
				GET,
				null,
				etag,
				etag,
				contentType,
				contentEncoded,
				empty()));

		assertEquals(NOT_MODIFIED.value(),
				response.getStatusCode().value());

		final var headers = response.getHeaders();
		assertEquals("\"" + etag + "\"", headers.getETag());
		assertEquals(cacheControl.getHeaderValue(), headers.getCacheControl());
		assertEquals(contentType, headers.getContentType().toString());
		assertEquals(contentEncoded, headers.get(CONTENT_ENCODING).get(0));
		assertEquals("bytes", headers.get(ACCEPT_RANGES).get(0));
		assertEquals(-1, headers.getContentLength());
		assertEquals(ContentDisposition.empty(), headers.getContentDisposition());
		assertNull(response.getBody());
	}

	@Test
	void testMakeResponseEntity_partial() throws IOException {
		final var halfSize = (int) fileToSend.length() / 2;
		final var response = gfrc.makeResponseEntity(new GetFileRequest(
				fileToSend,
				GET,
				"bytes=0-" + (halfSize - 1),
				null,
				etag,
				contentType,
				contentEncoded,
				empty()));

		assertEquals(PARTIAL_CONTENT.value(),
				response.getStatusCode().value());

		final var headers = response.getHeaders();
		assertEquals("\"" + etag + "\"", headers.getETag());
		assertEquals(cacheControl.getHeaderValue(), headers.getCacheControl());
		assertEquals(contentType, headers.getContentType().toString());
		assertEquals(contentEncoded, headers.get(CONTENT_ENCODING).get(0));
		assertEquals("bytes", headers.get(ACCEPT_RANGES).get(0));
		assertEquals("bytes 0-" + (halfSize - 1) + "/" + fileToSend.length(),
				headers.get(CONTENT_RANGE).get(0));
		assertEquals(fileToSend.length() / 2, headers.getContentLength());
		assertEquals(ContentDisposition.empty(), headers.getContentDisposition());

		final var body = response.getBody();
		final var baos = new ByteArrayOutputStream(halfSize);
		body.writeTo(baos);
		assertThat(baos.toString(UTF_8))
				.isEqualTo(content.substring(0, halfSize));
	}

	@Test
	void testMakeResponseEntity_head_partial() {
		final var halfSize = (int) fileToSend.length() / 2;
		final var response = gfrc.makeResponseEntity(new GetFileRequest(
				fileToSend,
				HEAD,
				"bytes=0-" + (halfSize - 1),
				null,
				etag,
				contentType,
				contentEncoded,
				empty()));

		assertEquals(PARTIAL_CONTENT.value(),
				response.getStatusCode().value());

		final var headers = response.getHeaders();
		assertEquals("\"" + etag + "\"", headers.getETag());
		assertEquals(cacheControl.getHeaderValue(), headers.getCacheControl());
		assertEquals(contentType, headers.getContentType().toString());
		assertEquals(contentEncoded, headers.get(CONTENT_ENCODING).get(0));
		assertEquals("bytes", headers.get(ACCEPT_RANGES).get(0));
		assertEquals("bytes 0-" + (halfSize - 1) + "/" + fileToSend.length(),
				headers.get(CONTENT_RANGE).get(0));
		assertEquals(fileToSend.length() / 2, headers.getContentLength());
		assertEquals(ContentDisposition.empty(), headers.getContentDisposition());
		assertNull(response.getBody());
	}

	@Test
	void testMakeResponseEntity_downloaded() throws IOException {
		final var response = gfrc.makeResponseEntity(new GetFileRequest(
				fileToSend,
				GET,
				null,
				null,
				etag,
				contentType,
				contentEncoded,
				Optional.ofNullable(downloadedFileName)));

		assertEquals(OK.value(),
				response.getStatusCode().value());

		final var headers = response.getHeaders();
		assertEquals("\"" + etag + "\"", headers.getETag());
		assertEquals(cacheControl.getHeaderValue(), headers.getCacheControl());
		assertEquals(contentType, headers.getContentType().toString());
		assertEquals(contentEncoded, headers.get(CONTENT_ENCODING).get(0));
		assertEquals("bytes", headers.get(ACCEPT_RANGES).get(0));
		assertEquals(fileToSend.length(), headers.getContentLength());

		assertEquals(attachment()
				.filename(downloadedFileName, UTF_8)
				.build(),
				headers.getContentDisposition());

		final var body = response.getBody();
		final var baos = new ByteArrayOutputStream((int) fileToSend.length());
		body.writeTo(baos);
		assertThat(baos.toString(UTF_8)).isEqualTo(content);
	}

	@Test
	void testMakeResponseEntity_badFile() throws IOException {
		forceDelete(fileToSend);
		final var response = gfrc.makeResponseEntity(new GetFileRequest(
				fileToSend,
				GET,
				null,
				null,
				etag,
				contentType,
				contentEncoded,
				empty()));

		assertEquals(INTERNAL_SERVER_ERROR.value(),
				response.getStatusCode().value());
		assertNull(response.getBody());
	}

}
