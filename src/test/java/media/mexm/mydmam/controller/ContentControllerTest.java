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

import static java.util.Optional.empty;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpRange.createByteRange;
import static org.springframework.http.HttpStatusCode.valueOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRange;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpServletRequest;
import media.mexm.mydmam.component.GetFileRequestComponent;
import media.mexm.mydmam.component.GetFileRequestComponent.GetFileRequest;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.AssetRenderedFileRepository;
import media.mexm.mydmam.service.MediaAssetService;
import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class ContentControllerTest {

    private static final RequestMapping REQUEST_MAPPING = ContentController.class.getAnnotation(
            RequestMapping.class);
    private static final String BASE_MAPPING = REQUEST_MAPPING.value()[0];
    private static final ResultMatcher STATUS_BAD_REQUEST = status().isBadRequest();
    private static final ResultMatcher STATUS_NOT_FOUND = status().isNotFound();
    private static final ResultMatcher STATUS_TEAPOT = status().isIAmATeapot();

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MyDMAMConfigurationProperties conf;
    @MockitoBean
    AssetRenderedFileRepository assetRenderedFileRepository;
    @MockitoBean
    GetFileRequestComponent getFileRequestComponent;
    @MockitoBean
    MediaAssetService mediaAssetService;

    @Mock
    HttpServletRequest request;
    @Mock
    RealmConf realmConf;
    @Mock
    AssetRenderedFileEntity renderedFileEntity;
    @Mock
    FileEntity fileEntity;

    @Captor
    ArgumentCaptor<GetFileRequest> requestCaptor;

    @Fake
    String realm;
    @Fake
    String hashPath;
    @Fake
    String name;
    @Fake(min = 0, max = 1)
    int index;

    @Fake
    String renderedFileEntityName;
    @Fake
    String renderedFileEntityRelativePath;
    @Fake
    String renderedFileEntityHexETag;
    @Fake
    String renderedFileEntityMimeType;
    @Fake
    String renderedFileEntityEncoded;

    @Fake(min = 10, max = 100)
    long firstBytePos;
    @Fake(min = 101, max = 1000)
    long lastBytePos;
    @Fake
    String ifNoneMatch;

    File renderedMetadataDirectory;
    HttpHeaders baseHeaders;
    String rangeHeader;
    HttpMethod method;
    String getAssetRenderedFilesURL;
    File returnFile;

    @BeforeEach
    void init() throws IOException {
        renderedMetadataDirectory = getTempDirectory();
        method = Faker.instance().random().nextBoolean() ? GET : HEAD;
        baseHeaders = new HttpHeaders();

        final var ranges = List.of(createByteRange(firstBytePos, lastBytePos));
        rangeHeader = HttpRange.toString(ranges);
        baseHeaders.setRange(ranges);
        baseHeaders.setIfNoneMatch(ifNoneMatch);

        returnFile = File.createTempFile("mydmam-" + getClass().getSimpleName(), "returnFile");
        deleteQuietly(returnFile);

        when(getFileRequestComponent.makeResponseEntity(any()))
                .thenReturn(new ResponseEntity<>(valueOf(418)));
        when(conf.getRealmByName(realm)).thenReturn(Optional.ofNullable(realmConf));
        when(realmConf.renderedMetadataDirectory()).thenReturn(renderedMetadataDirectory);

        when(renderedFileEntity.getName()).thenReturn(renderedFileEntityName);
        when(renderedFileEntity.getFile()).thenReturn(fileEntity);
        when(renderedFileEntity.getLength()).thenReturn(0l);
        when(renderedFileEntity.getHexETag()).thenReturn(renderedFileEntityHexETag);
        when(renderedFileEntity.getMimeType()).thenReturn(renderedFileEntityMimeType);
        when(renderedFileEntity.getEncoded()).thenReturn(renderedFileEntityEncoded);
        when(mediaAssetService.getPhysicalRenderedFile(fileEntity, renderedFileEntity, realm))
                .thenReturn(returnFile);

        getAssetRenderedFilesURL = Stream.of(
                BASE_MAPPING,
                "rendered",
                realm,
                hashPath,
                name)
                .collect(joining("/"));
    }

    @AfterEach
    void ends() {
        verifyNoMoreInteractions(
                request,
                assetRenderedFileRepository,
                getFileRequestComponent,
                mediaAssetService);
    }

    @Test
    void testGetAssetRenderedFiles_badRealm() throws Exception {
        when(conf.getRealmByName(realm)).thenReturn(empty());

        mvc.perform(request(method, getAssetRenderedFilesURL)
                .headers(baseHeaders))
                .andExpect(STATUS_BAD_REQUEST)
                .andReturn()
                .getResponse();
    }

    @Test
    void testGetAssetRenderedFiles_notFound() throws Exception {
        when(assetRenderedFileRepository.getRenderedFile(hashPath, realm, name, index))
                .thenReturn(null);

        mvc.perform(request(method, getAssetRenderedFilesURL + "?index=" + index)
                .headers(baseHeaders))
                .andExpect(STATUS_NOT_FOUND)
                .andReturn()
                .getResponse();

        verify(assetRenderedFileRepository, times(1)).getRenderedFile(hashPath, realm, name, index);
    }

    @Test
    void testGetAssetRenderedFiles() throws Exception {
        when(assetRenderedFileRepository.getRenderedFile(hashPath, realm, name, index))
                .thenReturn(renderedFileEntity);

        mvc.perform(request(method, getAssetRenderedFilesURL + "?index=" + index)
                .headers(baseHeaders))
                .andExpect(STATUS_TEAPOT)
                .andReturn()
                .getResponse();

        verify(assetRenderedFileRepository, times(1)).getRenderedFile(hashPath, realm, name, index);

        verify(renderedFileEntity, atLeastOnce()).getName();
        verify(renderedFileEntity, atLeastOnce()).getHexETag();
        verify(renderedFileEntity, atLeastOnce()).getMimeType();
        verify(renderedFileEntity, atLeastOnce()).getEncoded();
        verify(renderedFileEntity, atLeastOnce()).getFile();
        verify(mediaAssetService, times(1)).getPhysicalRenderedFile(fileEntity, renderedFileEntity, realm);

        verify(getFileRequestComponent, times(1)).makeResponseEntity(requestCaptor.capture());
        assertThat(requestCaptor.getValue())
                .isEqualTo(new GetFileRequest(
                        returnFile,
                        method,
                        rangeHeader,
                        ifNoneMatch,
                        renderedFileEntityHexETag,
                        renderedFileEntityMimeType,
                        renderedFileEntityEncoded,
                        empty()));
    }

    @Test
    void testGetAssetRenderedFiles_download() throws Exception {
        when(assetRenderedFileRepository.getRenderedFile(hashPath, realm, name, index))
                .thenReturn(renderedFileEntity);

        mvc.perform(request(method, getAssetRenderedFilesURL + "?index=" + index + "&download=1")
                .headers(baseHeaders))
                .andExpect(STATUS_TEAPOT)
                .andReturn()
                .getResponse();

        verify(assetRenderedFileRepository, times(1)).getRenderedFile(hashPath, realm, name, index);

        verify(renderedFileEntity, atLeastOnce()).getName();
        verify(renderedFileEntity, atLeastOnce()).getHexETag();
        verify(renderedFileEntity, atLeastOnce()).getMimeType();
        verify(renderedFileEntity, atLeastOnce()).getEncoded();
        verify(renderedFileEntity, atLeastOnce()).getFile();
        verify(mediaAssetService, times(1)).getPhysicalRenderedFile(fileEntity, renderedFileEntity, realm);

        verify(getFileRequestComponent, times(1)).makeResponseEntity(requestCaptor.capture());
        final var fRequest = requestCaptor.getValue();
        assertThat(fRequest.contentEncoded()).isEqualTo(renderedFileEntityEncoded);
        assertThat(fRequest.etag()).isEqualTo(renderedFileEntityHexETag);
        assertThat(fRequest.contentType()).isEqualTo(renderedFileEntityMimeType);
        assertThat(fRequest.method()).isEqualTo(method);
        assertThat(fRequest.fileToSend())
                .isEqualTo(returnFile);
        assertThat(fRequest.rangeHeader()).isEqualTo(rangeHeader);
        assertThat(fRequest.ifNoneMatch()).isEqualTo(ifNoneMatch);

        if (index > 0) {
            assertThat(fRequest.oDownloadedFileName()).asString().contains("_" + index + ".");
        } else {
            assertThat(fRequest.oDownloadedFileName()).contains(renderedFileEntityName);
        }

    }

}
