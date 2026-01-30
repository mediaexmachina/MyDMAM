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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMinutes;
import static java.util.Optional.empty;
import static media.mexm.mydmam.App.CONTROLLER_BASE_MAPPING_API_PATH;
import static media.mexm.mydmam.entity.AssetRenderedFileEntity.NAME_LEN;
import static media.mexm.mydmam.entity.FileEntity.HASH_STRING_LEN;
import static media.mexm.mydmam.entity.FileEntity.MAX_NAME_SIZE;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.ContentDisposition.attachment;
import static org.springframework.http.HttpHeaders.ACCEPT_RANGES;
import static org.springframework.http.HttpHeaders.CONTENT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_RANGE;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.PARTIAL_CONTENT;
import static org.springframework.http.HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.HEAD;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.repository.AssetRenderedFileRepository;
import media.mexm.mydmam.tools.HTTPRangeRequest;
import media.mexm.mydmam.tools.TransfertFileRangeToOutputStream;

@Controller
@Validated
@Slf4j
@RequestMapping(value = CONTROLLER_BASE_MAPPING_API_PATH + "/content")
public class ContentController { // TODO test

	@Autowired
	MyDMAMConfigurationProperties conf;
	@Autowired
	AssetRenderedFileRepository assetRenderedFileRepository;

	ResponseEntity<StreamingResponseBody> sendFile(final File fileToSend, // NOSONAR S107
												   final HttpMethod method,
												   final String rangeHeader,
												   final long lastModified,
												   final String ifNoneMatch,
												   final String etag,
												   final String contentType,
												   final String contentEncoded,
												   final Optional<String> oDownloadedFileName) {
		final var responseHeaders = new HttpHeaders();
		responseHeaders.setLastModified(lastModified);
		responseHeaders.setETag(etag);
		responseHeaders.setCacheControl(maxAge(ofMinutes(1)).noTransform().cachePrivate().mustRevalidate());
		responseHeaders.set(CONTENT_TYPE, contentType);
		responseHeaders.set(CONTENT_ENCODING, contentEncoded);
		responseHeaders.set(ACCEPT_RANGES, "bytes");

		if (etag.equalsIgnoreCase(ifNoneMatch)) {
			return new ResponseEntity<>(responseHeaders, NOT_MODIFIED);
		}

		final var fileLength = fileToSend.length();
		final var rangeRequest = new HTTPRangeRequest(rangeHeader, fileLength);
		if (rangeRequest.end() > fileLength) {
			return new ResponseEntity<>(REQUESTED_RANGE_NOT_SATISFIABLE);
		}
		responseHeaders.setContentLength(rangeRequest.contentLength());

		if (rangeRequest.partial()) {
			responseHeaders.add(CONTENT_RANGE,
					"bytes" + " " + rangeRequest.start() + "-" + rangeRequest.end() + "/" + fileLength);
		}

		final var status = rangeRequest.partial() ? PARTIAL_CONTENT : OK;

		if (HttpMethod.HEAD.equals(method)) {
			return new ResponseEntity<>(
					responseHeaders,
					status);
		}

		oDownloadedFileName.ifPresent(downloadedFileName -> {
			final var contentDisposition = attachment()
					.filename(downloadedFileName, UTF_8)
					.build();
			responseHeaders.setContentDisposition(contentDisposition);
		});

		try {
			final var renderedFile = fileToSend.getAbsoluteFile().getCanonicalFile();
			if (renderedFile.exists() == false) {
				throw new FileNotFoundException(fileToSend.getPath());
			}
			return new ResponseEntity<>(
					output -> new TransfertFileRangeToOutputStream(renderedFile, rangeRequest).send(output),
					responseHeaders,
					status);
		} catch (final IOException e) {
			log.error("Can't access to file {}", fileToSend, e);
			return new ResponseEntity<>(INTERNAL_SERVER_ERROR);
		}
	}

	@RequestMapping(value = "/rendered/{realm}/{hashPath}/{name}", method = { GET, HEAD })
	@ResponseBody
	public ResponseEntity<StreamingResponseBody> getAssetRenderedFiles(@PathVariable @NotBlank @Size(max = MAX_NAME_SIZE) final String realm,
																	   @PathVariable @NotBlank @Size(max = HASH_STRING_LEN) final String hashPath,
																	   @PathVariable @NotBlank @Size(max = NAME_LEN) final String name,
																	   @RequestParam(required = false,
																					 defaultValue = "0") @Min(0) final Integer index,
																	   @RequestParam(required = false,
																					 defaultValue = "0") @Min(0) @Max(1) final Integer download,
																	   @RequestHeader(value = "Range",
																					  required = false) final String rangeHeader,
																	   @RequestHeader(value = "If-None-Match",
																					  required = false) final String ifNoneMatch,
																	   final HttpMethod method) {
		final var realmConf = conf.getRealmByName(realm);
		if (realmConf.isEmpty()) {
			return new ResponseEntity<>(BAD_REQUEST);
		}

		final var renderedFileEntity = assetRenderedFileRepository.getRenderedFile(hashPath, realm, name, index);
		if (renderedFileEntity == null) {
			return new ResponseEntity<>(NOT_FOUND);
		}

		final var renderedFileEntityName = renderedFileEntity.getName();
		final String downloadedFileName;
		if (index > 0) {
			downloadedFileName = getBaseName(renderedFileEntityName) + "_" + index + "." + getExtension(
					renderedFileEntityName);
		} else {
			downloadedFileName = renderedFileEntityName;
		}

		final var fileToSend = new File(
				realmConf.get().renderedMetadataDirectory() + renderedFileEntity.getRelativePath());
		final var fileLength = renderedFileEntity.getLength();

		if (fileLength != fileToSend.length()) {
			log.error("Invalid file size from renderedFileEntity={}, file={}",
					renderedFileEntity, fileToSend.length());
			return new ResponseEntity<>(BAD_REQUEST);
		}

		return sendFile(
				fileToSend,
				method,
				rangeHeader,
				renderedFileEntity.getCreateDate().getTime(),
				ifNoneMatch,
				renderedFileEntity.getHexETag(),
				renderedFileEntity.getMimeType(),
				renderedFileEntity.getEncoded(),
				download == 1 ? Optional.ofNullable(downloadedFileName) : empty());
	}

}
