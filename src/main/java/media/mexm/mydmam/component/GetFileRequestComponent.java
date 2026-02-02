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
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.ContentDisposition.attachment;
import static org.springframework.http.HttpHeaders.ACCEPT_RANGES;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_RANGE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PARTIAL_CONTENT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.tools.HTTPRangeRequest;
import media.mexm.mydmam.tools.TransfertFileRangeToOutputStream;

@Slf4j
@Component
public class GetFileRequestComponent {

	public ResponseEntity<StreamingResponseBody> makeResponseEntity(final GetFileRequest request) {
		final var responseHeaders = new HttpHeaders();
		responseHeaders.setETag(request.etag);
		responseHeaders.setCacheControl(maxAge(ofMinutes(1)).noTransform().cachePrivate().mustRevalidate());
		responseHeaders.set(CONTENT_TYPE, request.contentType);
		responseHeaders.set(CONTENT_ENCODING, request.contentEncoded);
		responseHeaders.set(ACCEPT_RANGES, "bytes");

		if (request.etag.equalsIgnoreCase(request.ifNoneMatch)) {
			return new ResponseEntity<>(responseHeaders, NOT_MODIFIED);
		}

		final var fileLength = request.fileToSend.length();
		final var rangeRequest = new HTTPRangeRequest(request.rangeHeader, fileLength);
		responseHeaders.setContentLength(rangeRequest.contentLength());

		if (rangeRequest.partial()) {
			responseHeaders.add(CONTENT_RANGE,
					"bytes " + rangeRequest.start() + "-" + rangeRequest.end() + "/" + fileLength);
		}

		final var status = rangeRequest.partial() ? PARTIAL_CONTENT : OK;

		if (HttpMethod.HEAD.equals(request.method)) {
			return new ResponseEntity<>(
					responseHeaders,
					status);
		}

		request.oDownloadedFileName.ifPresent(downloadedFileName -> {
			final var contentDisposition = attachment()
					.filename(downloadedFileName, UTF_8)
					.build();
			responseHeaders.setContentDisposition(contentDisposition);
		});

		try {
			final var renderedFile = request.fileToSend.getAbsoluteFile().getCanonicalFile();
			if (renderedFile.exists() == false) {
				throw new FileNotFoundException(request.fileToSend.getPath());
			}
			return new ResponseEntity<>(
					output -> new TransfertFileRangeToOutputStream(renderedFile, rangeRequest).send(output),
					responseHeaders,
					status);
		} catch (final IOException e) {
			log.error("Can't access to file {}", request.fileToSend, e);
			return new ResponseEntity<>(INTERNAL_SERVER_ERROR);
		}
	}

	public static record GetFileRequest(File fileToSend,
										HttpMethod method,
										String rangeHeader,
										String ifNoneMatch,
										String etag,
										String contentType,
										String contentEncoded,
										Optional<String> oDownloadedFileName) {
	}

}
