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
package media.mexm.mydmam.service;

import static java.io.File.createTempFile;
import static java.io.File.separator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static media.mexm.mydmam.audittrail.AuditTrailObjectType.RENDERED_FILE_ENTRY;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static media.mexm.mydmam.service.MediaAssetService.MEDIA_ASSET_AUDIT_ISSUER;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.apache.commons.io.FileUtils.touch;
import static org.apache.commons.io.FileUtils.write;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.asset.DeclaredRenderedFile;
import media.mexm.mydmam.asset.RenderedFileSpecs;
import media.mexm.mydmam.audittrail.AuditTrailBatchInsertObject;
import media.mexm.mydmam.audittrail.RealmAuditTrail;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.component.ImageMagick;
import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.AssetTextExtractedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import media.mexm.mydmam.indexer.RealmIndexer;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.repository.AssetRenderedFileDao;
import media.mexm.mydmam.repository.AssetRenderedFileRepository;
import media.mexm.mydmam.repository.AssetTextExtractedFileDao;
import media.mexm.mydmam.repository.AssetTextExtractedFileRepository;
import media.mexm.mydmam.repository.FileMetadataDao;
import media.mexm.mydmam.repository.FileMetadataRepository;
import media.mexm.mydmam.repository.FileRepository;
import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.transfertfiles.FileAttributesReference;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class MediaAssetServiceTest {

    @MockitoBean
    FileRepository fileRepository;
    @MockitoBean
    AssetRenderedFileDao assetRenderedFileDao;
    @MockitoBean
    FileMetadataRepository fileMetadataRepository;
    @MockitoBean
    FileMetadataDao fileMetadataDao;
    @MockitoBean
    MyDMAMConfigurationProperties configuration;
    @MockitoBean
    AssetRenderedFileRepository assetRenderedFileRepository;
    @MockitoBean
    AssetTextExtractedFileRepository assetTextExtractedFileRepository;
    @MockitoBean
    AssetTextExtractedFileDao assetTextExtractedFileDao;
    @MockitoBean
    Indexer indexer;
    @MockitoBean
    AuditTrail auditTrail;
    @MockitoBean
    MimeTypeDetector mimeTypeDetector;
    @MockitoBean
    ImageMagick imageMagick;

    @Mock
    FileAttributesReference fileAttributesReference;
    @Mock
    MediaAssetService injectedService;
    @Mock
    RealmConf realmConf;
    @Mock
    AssetRenderedFileEntity assetRenderedFileEntity;
    @Mock
    AssetTextExtractedFileEntity assetTextExtractedFileEntity;
    @Mock
    FileMetadataEntity fileMetadataEntity;
    @Mock
    RealmAuditTrail realmAuditTrail;
    @Mock
    Map<String, Serializable> auditTrailPayload;
    @Mock
    RenderedFileSpecs renderedFileSpecs;
    @Mock
    RealmStorageConfiguredEnv storedOn;
    @Mock
    RealmConf realm;
    @Mock
    RealmIndexer realmIndexer;
    @Mock
    FileEntity fileEntity;

    @Captor
    ArgumentCaptor<AssetTextExtractedFileEntity> assetTextExtractedFileEntityCaptor;

    @Fake
    String realmName;
    @Fake
    String storageName;
    @Fake
    String filePath;
    @Fake
    String mimeType;
    @Fake
    String fileHashpath;
    @Fake
    String fileName;
    @Fake
    int fileId;
    @Fake
    long etag;
    @Fake
    int renderedId;
    @Fake
    String renderedName;
    @Fake
    int indexRef;

    @Autowired
    MediaAssetService mas;

    File renderedMetadataDirectory;

    @BeforeEach
    void init() {
        renderedMetadataDirectory = getTempDirectory();
        fileHashpath = hashPath(realmName, storageName, filePath);

        when(fileMetadataEntity.getAuditTrailPayload())
                .thenReturn(auditTrailPayload);
        when(auditTrail.getAuditTrailByRealm(realmName))
                .thenReturn(Optional.ofNullable(realmAuditTrail));
        when(assetRenderedFileEntity.getAuditTrailPayload(any()))
                .thenReturn(auditTrailPayload);
        when(assetRenderedFileEntity.getEtag()).thenReturn(etag);
        when(assetRenderedFileEntity.getName()).thenReturn(renderedName);
        when(assetRenderedFileEntity.getFile()).thenReturn(fileEntity);
        when(assetRenderedFileEntity.getId()).thenReturn(renderedId);
        when(assetRenderedFileEntity.getIndexref()).thenReturn(indexRef);
        when(assetRenderedFileEntity.isGzipEncoded()).thenReturn(false);

        when(assetTextExtractedFileEntity.getAuditTrailPayload(any())).thenReturn(auditTrailPayload);
        when(assetTextExtractedFileEntity.getName()).thenReturn(renderedName);
        when(assetTextExtractedFileEntity.getFile()).thenReturn(fileEntity);
        when(assetTextExtractedFileEntity.getId()).thenReturn(renderedId);
        when(assetTextExtractedFileEntity.getIndexref()).thenReturn(indexRef);
        when(assetTextExtractedFileEntity.isGzipEncoded()).thenReturn(false);

        when(storedOn.realm()).thenReturn(realm);
        when(fileEntity.getHashPath()).thenReturn(fileHashpath);
        when(fileEntity.getId()).thenReturn(fileId);
        when(fileEntity.getRealm()).thenReturn(realmName);
        when(indexer.getIndexerByRealm(realmName)).thenReturn(Optional.ofNullable(realmIndexer));
        when(fileAttributesReference.getPath()).thenReturn(filePath);
        when(fileRepository.getByHashPath(fileHashpath, realmName)).thenReturn(fileEntity);

        when(configuration.getRealmByName(realmName)).thenReturn(Optional.ofNullable(realmConf));
        when(realmConf.renderedMetadataDirectory()).thenReturn(renderedMetadataDirectory);
        when(assetRenderedFileRepository.getRenderedForFileByEtag(eq(fileId), any()))
                .thenReturn(assetRenderedFileEntity);

        when(assetTextExtractedFileRepository.getAllTextExtracted(fileEntity)).thenReturn(Set.of());
        when(assetTextExtractedFileRepository.getTextExtractedByName(fileId, renderedName))
                .thenReturn(assetTextExtractedFileEntity);
    }

    @AfterEach
    void ends() {
        verifyNoMoreInteractions(
                fileRepository,
                assetRenderedFileDao,
                assetRenderedFileRepository,
                fileMetadataDao,
                fileMetadataRepository,
                assetTextExtractedFileRepository,
                assetTextExtractedFileDao,
                indexer,
                auditTrail,
                mimeTypeDetector,
                imageMagick);
    }

    @Test
    void testGetFromWatchfolder() {
        final var result = mas.getFromWatchfolder(realmName, storageName, fileAttributesReference);
        assertEquals(result, fileEntity);

        verify(fileAttributesReference, times(1)).getPath();
        verify(fileRepository, times(1)).getByHashPath(fileHashpath, realmName);
    }

    @Test
    void testPurgeAssetArtefacts() {
        assertDoesNotThrow(() -> mas.purgeAssetArtefacts(realmName, storageName, fileAttributesReference));
    }

    @Nested
    class DeclareRenderedStaticFiles {

        @Mock
        DeclaredRenderedFile declaredRenderedFile;

        @Fake
        String previewType;
        @Fake
        String renderedContent;

        File renderedMetadataDirectory;
        File workingFile;
        File expectedRenderedFile;

        @BeforeEach
        void init() throws IOException {
            renderedMetadataDirectory = getTempDirectory();

            workingFile = createTempFile(DeclareRenderedStaticFiles.class.getSimpleName(), "workingFile");
            write(workingFile, renderedContent, UTF_8, false);

            expectedRenderedFile = mas.getAbsolutePath(fileEntity, assetRenderedFileEntity);
            when(declaredRenderedFile.name()).thenReturn(renderedName);
            when(declaredRenderedFile.workingFile()).thenReturn(workingFile);
            when(declaredRenderedFile.index()).thenReturn(indexRef);
            when(declaredRenderedFile.mimeType()).thenReturn(mimeType);
            when(declaredRenderedFile.previewType()).thenReturn(previewType);
            when(declaredRenderedFile.toGzip()).thenReturn(false);

            when(realmConf.renderedMetadataDirectory()).thenReturn(renderedMetadataDirectory);
        }

        @AfterEach
        void ends() {
            deleteQuietly(workingFile);
        }

        @Test
        void testDeclare() throws IOException {
            mas.declareRenderedStaticFile(fileEntity, declaredRenderedFile);

            verify(assetRenderedFileRepository, times(1)).saveAndFlush(any());

            assertThat(expectedRenderedFile).exists().hasContent(renderedContent);
            assertThat(workingFile).doesNotExist();

            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getId();
            verify(fileEntity, atLeastOnce()).getHashPath();
            verify(configuration, atLeastOnce()).getRealmByName(realmName);
            verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
            verify(assetRenderedFileRepository, times(1)).getRenderedForFileByEtag(eq(fileId), any());
            verify(declaredRenderedFile, atLeastOnce()).name();
            verify(declaredRenderedFile, atLeastOnce()).workingFile();
            verify(declaredRenderedFile, atLeastOnce()).index();
            verify(declaredRenderedFile, atLeastOnce()).mimeType();
            verify(declaredRenderedFile, atLeastOnce()).previewType();
            verify(declaredRenderedFile, atLeastOnce()).toGzip();
            verify(assetRenderedFileEntity, times(1)).getAuditTrailPayload(expectedRenderedFile);
            verify(assetRenderedFileEntity, atLeastOnce()).getId();
            verify(assetRenderedFileEntity, atLeastOnce()).getIndexref();
            verify(assetRenderedFileEntity, atLeastOnce()).getName();
            verify(assetRenderedFileEntity, atLeastOnce()).isGzipEncoded();

            verify(auditTrail, times(1)).getAuditTrailByRealm(realmName);
            verify(realmAuditTrail, times(1))
                    .asyncPersist(MEDIA_ASSET_AUDIT_ISSUER, "save-rendered-file",
                            new AuditTrailBatchInsertObject(
                                    RENDERED_FILE_ENTRY,
                                    fileHashpath,
                                    List.of(auditTrailPayload)));
        }

        @Test
        void testDeclare_explicit() throws IOException {
            mas.declareRenderedStaticFile(fileEntity, workingFile, renderedName, false, indexRef, previewType);

            verify(assetRenderedFileRepository, times(1)).saveAndFlush(any());

            assertThat(expectedRenderedFile).exists().hasContent(renderedContent);
            assertThat(workingFile).doesNotExist();

            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getId();
            verify(fileEntity, atLeastOnce()).getHashPath();
            verify(configuration, atLeastOnce()).getRealmByName(realmName);
            verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
            verify(assetRenderedFileRepository, times(1)).getRenderedForFileByEtag(eq(fileId), any());
            verify(assetRenderedFileEntity, times(1)).getAuditTrailPayload(expectedRenderedFile);
            verify(assetRenderedFileEntity, atLeastOnce()).getId();
            verify(assetRenderedFileEntity, atLeastOnce()).getIndexref();
            verify(assetRenderedFileEntity, atLeastOnce()).getName();
            verify(assetRenderedFileEntity, atLeastOnce()).isGzipEncoded();
            verify(mimeTypeDetector, times(1)).getMimeType(workingFile);

            verify(auditTrail, times(1)).getAuditTrailByRealm(realmName);
            verify(realmAuditTrail, times(1))
                    .asyncPersist(MEDIA_ASSET_AUDIT_ISSUER, "save-rendered-file",
                            new AuditTrailBatchInsertObject(
                                    RENDERED_FILE_ENTRY,
                                    fileHashpath,
                                    List.of(auditTrailPayload)));
        }

        @Test
        void testRenderedFileExists() throws IOException {
            write(expectedRenderedFile, "NOPE", UTF_8, false);

            assertThrows(IOException.class,
                    () -> mas.declareRenderedStaticFile(fileEntity, declaredRenderedFile));

            verify(assetRenderedFileRepository, times(1)).saveAndFlush(any());

            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getId();
            verify(configuration, atLeastOnce()).getRealmByName(realmName);
            verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
            verify(assetRenderedFileEntity, atLeastOnce()).getId();
            verify(assetRenderedFileEntity, atLeastOnce()).getIndexref();
            verify(assetRenderedFileEntity, atLeastOnce()).getName();
            verify(assetRenderedFileEntity, atLeastOnce()).isGzipEncoded();
            verify(declaredRenderedFile, atLeastOnce()).name();
            verify(declaredRenderedFile, atLeastOnce()).workingFile();
            verify(declaredRenderedFile, atLeastOnce()).index();
            verify(declaredRenderedFile, atLeastOnce()).mimeType();
            verify(declaredRenderedFile, atLeastOnce()).previewType();
            verify(declaredRenderedFile, atLeastOnce()).toGzip();
            verify(assetRenderedFileRepository, times(1)).getRenderedForFileByEtag(eq(fileId), any());
        }

    }

    @Nested
    class GetPhysicalRenderedFile {

        File tempFile;

        @BeforeEach
        void init() {
            tempFile = mas.getAbsolutePath(fileEntity, assetRenderedFileEntity);
        }

        @AfterEach
        void ends() {
            deleteQuietly(tempFile);
            verify(configuration, atLeastOnce()).getRealmByName(realmName);
            verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
        }

        @Test
        void testGetFile() throws IOException {
            write(tempFile, Faker.instance().lorem().paragraph(5), UTF_8);
            when(assetRenderedFileEntity.getLength()).thenReturn(tempFile.length());

            final var result = mas.getPhysicalRenderedFile(fileEntity, assetRenderedFileEntity, realmName);
            assertThat(result).isEqualTo(tempFile);

            verify(assetRenderedFileEntity, atLeastOnce()).getLength();
            verify(assetRenderedFileEntity, atLeastOnce()).getId();
            verify(assetRenderedFileEntity, atLeastOnce()).getIndexref();
            verify(assetRenderedFileEntity, atLeastOnce()).getName();
            verify(assetRenderedFileEntity, atLeastOnce()).isGzipEncoded();
            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getId();
        }

        @Test
        void testInvalidSize() throws IOException {
            touch(tempFile);
            when(assetRenderedFileEntity.getLength()).thenReturn(1l);
            assertThrows(UncheckedIOException.class,
                    () -> mas.getPhysicalRenderedFile(fileEntity, assetRenderedFileEntity, realmName));

            verify(assetRenderedFileEntity, atLeastOnce()).getLength();
            verify(assetRenderedFileEntity, atLeastOnce()).getId();
            verify(assetRenderedFileEntity, atLeastOnce()).getIndexref();
            verify(assetRenderedFileEntity, atLeastOnce()).getName();
            verify(assetRenderedFileEntity, atLeastOnce()).isGzipEncoded();
            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getId();
        }

        @Test
        void testNotFound() {
            assertThrows(UncheckedIOException.class,
                    () -> mas.getPhysicalRenderedFile(fileEntity, assetRenderedFileEntity, realmName));

            verify(assetRenderedFileEntity, atLeastOnce()).getId();
            verify(assetRenderedFileEntity, atLeastOnce()).getIndexref();
            verify(assetRenderedFileEntity, atLeastOnce()).getName();
            verify(assetRenderedFileEntity, atLeastOnce()).isGzipEncoded();
            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getId();
        }
    }

    @Nested
    class ResetDetectedMetadatas {

        @Captor
        ArgumentCaptor<FileEntity> fileCaptor;

        Set<Integer> fileIdsToReset;
        File renderedMetadataDirectory;
        File tempFile;

        @BeforeEach
        void init() throws IOException {
            fileIdsToReset = Set.of(fileId);

            renderedMetadataDirectory = getTempDirectory();
            tempFile = File.createTempFile(
                    "mydmam-" + getClass().getSimpleName(),
                    "physicalRenderedFile",
                    renderedMetadataDirectory);
            forceDelete(tempFile);

            when(realmConf.renderedMetadataDirectory()).thenReturn(renderedMetadataDirectory);
            when(assetRenderedFileDao.deleteRenderedFilesByFileId(fileIdsToReset))
                    .thenReturn(Map.of(realmName, Set.of(assetRenderedFileEntity)));
            when(fileRepository.getByIds(fileIdsToReset)).thenReturn(Set.of(fileEntity));
        }

        @Test
        void empty() {
            final var result = mas.resetDetectedMetadatas(List.of());
            assertThat(result).isEmpty();
        }

        @Test
        void test() {
            final var result = mas.resetDetectedMetadatas(List.of(fileEntity));
            assertThat(result).hasSize(1);
            assertThat(result.iterator().next()).isEqualTo(fileEntity);

            verify(fileEntity, atLeastOnce()).getId();
            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileMetadataRepository, times(1)).deleteByFileId(fileIdsToReset);
            verify(assetRenderedFileDao, times(1)).deleteRenderedFilesByFileId(fileIdsToReset);
            verify(configuration, atLeastOnce()).getRealmByName(realmName);
            verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
            verify(fileRepository, times(1)).getByIds(fileIdsToReset);
            verify(indexer, atLeastOnce()).getIndexerByRealm(realmName);
            verify(realmIndexer, times(1)).updateAsset(fileEntity, List.of(), List.of());
            verify(assetTextExtractedFileDao, times(1)).deleteTextExtractedFilesByFileId(fileIdsToReset);
            verify(assetRenderedFileEntity, atLeastOnce()).getFile();
            verify(assetRenderedFileEntity, atLeastOnce()).getId();
            verify(assetRenderedFileEntity, atLeastOnce()).getIndexref();
            verify(assetRenderedFileEntity, atLeastOnce()).getName();
            verify(assetRenderedFileEntity, atLeastOnce()).isGzipEncoded();
            verify(fileEntity, atLeastOnce()).getRealm();
            verify(fileEntity, atLeastOnce()).getId();
        }

    }

    @Test
    void testUpdateIndexer() {
        when(fileMetadataRepository.getByFile(fileEntity)).thenReturn(Set.of());

        mas.updateIndexer(fileEntity);

        verify(indexer, times(1)).getIndexerByRealm(realmName);
        verify(realmIndexer, times(1)).updateAsset(fileEntity, Set.of(), List.of());
        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileMetadataRepository, times(1)).getByFile(fileEntity);
        verify(assetTextExtractedFileRepository, times(1)).getAllTextExtracted(fileEntity);
    }

    @Test
    void testDeclareTextExtractedFile() throws IOException {
        final var workingTextFile = File.createTempFile("mydmam-test-" + getClass().getName(), ".txt");
        final var fullText = Faker.instance().lorem().paragraphs(5).stream().collect(joining());
        write(workingTextFile, fullText, UTF_8);

        mas.declareTextExtractedFile(fileEntity, workingTextFile, renderedName);

        verify(assetTextExtractedFileRepository, times(1)).saveAndFlush(assetTextExtractedFileEntityCaptor.capture());
        final var currentAssetTextExtractedFileEntity = assetTextExtractedFileEntityCaptor.getValue();
        assertThat(currentAssetTextExtractedFileEntity.getFile()).isEqualTo(fileEntity);
        assertThat(currentAssetTextExtractedFileEntity.getName()).isEqualTo(renderedName);
        assertThat(currentAssetTextExtractedFileEntity.getLength()).isLessThan(fullText.length());

        final var renderedFile = mas.getAbsolutePath(fileEntity, assetTextExtractedFileEntity);

        assertThat(workingTextFile).doesNotExist();
        assertThat(renderedFile)
                .exists()
                .hasSize(currentAssetTextExtractedFileEntity.getLength());
        deleteQuietly(renderedFile);

        verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileEntity, atLeastOnce()).getId();
        verify(fileEntity, atLeastOnce()).getHashPath();
        verify(assetTextExtractedFileEntity, times(1)).getAuditTrailPayload(renderedFile);
        verify(assetTextExtractedFileEntity, atLeastOnce()).getId();
        verify(assetTextExtractedFileEntity, atLeastOnce()).getIndexref();
        verify(assetTextExtractedFileEntity, atLeastOnce()).getName();
        verify(assetTextExtractedFileEntity, atLeastOnce()).isGzipEncoded();
        verify(assetTextExtractedFileRepository, times(1)).getTextExtractedByName(fileId, renderedName);
        verify(auditTrail, times(1)).getAuditTrailByRealm(realmName);
        verify(realmAuditTrail, times(1))
                .asyncPersist(MEDIA_ASSET_AUDIT_ISSUER, "save-text-extracted-file",
                        new AuditTrailBatchInsertObject(
                                RENDERED_FILE_ENTRY,
                                fileHashpath,
                                List.of(auditTrailPayload)));
    }

    @Test
    void testForEachTextExtractedFile() throws IOException {
        final var renderedFile = mas.getAbsolutePath(fileEntity, assetTextExtractedFileEntity);
        forceMkdirParent(renderedFile);
        final var fullText = Faker.instance().lorem().paragraphs(5).stream().collect(joining());
        try (final var fso = new GZIPOutputStream(new FileOutputStream(renderedFile))) {
            fso.write(fullText.getBytes(UTF_8));
        }

        when(assetTextExtractedFileRepository.getAllTextExtracted(fileEntity))
                .thenReturn(Set.of(assetTextExtractedFileEntity));

        final var count = new AtomicInteger(0);
        final BiConsumer<AssetTextExtractedFileEntity, String> onTextExtracted = (atefe, text) -> {
            assertThat(atefe).isEqualTo(assetTextExtractedFileEntity);
            assertThat(text).isEqualTo(fullText);
            count.incrementAndGet();
        };

        mas.forEachTextExtractedFile(fileEntity, onTextExtracted);

        assertThat(renderedFile).exists();
        deleteQuietly(renderedFile);

        verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileEntity, atLeastOnce()).getId();
        verify(assetTextExtractedFileRepository, times(1)).getAllTextExtracted(fileEntity);
        verify(assetTextExtractedFileEntity, atLeastOnce()).getId();
        verify(assetTextExtractedFileEntity, atLeastOnce()).getIndexref();
        verify(assetTextExtractedFileEntity, atLeastOnce()).getName();
        verify(assetTextExtractedFileEntity, atLeastOnce()).isGzipEncoded();
        assertThat(count.get()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testGetRelativePath(final boolean isGzip) {
        when(fileEntity.getId()).thenReturn(0x1234ABCD);
        when(assetRenderedFileEntity.isGzipEncoded()).thenReturn(isGzip);

        final var path = mas.getRelativePath(fileEntity, assetRenderedFileEntity);

        var suffix = "";
        if (isGzip) {
            suffix = ".gz";
        }
        assertThat(path).isEqualTo("/1234/ABCD/" + renderedId + "." + indexRef + "." + renderedName + suffix);

        verify(fileEntity, atLeastOnce()).getId();
        verify(assetRenderedFileEntity, atLeastOnce()).getId();
        verify(assetRenderedFileEntity, atLeastOnce()).getIndexref();
        verify(assetRenderedFileEntity, atLeastOnce()).getName();
        verify(assetRenderedFileEntity, atLeastOnce()).isGzipEncoded();
    }

    @Test
    void testGetAbsolutePath() {
        final var file = mas.getAbsolutePath(fileEntity, assetRenderedFileEntity);

        assertThat(file).doesNotExist();
        assertThat(file.getPath()).isEqualTo(file.getAbsolutePath());

        final var path = mas.getRelativePath(fileEntity, assetRenderedFileEntity).replace("/", separator);
        assertThat(file.getPath()).endsWith(path);

        verify(realmConf, atLeastOnce()).renderedMetadataDirectory();
        verify(assetRenderedFileEntity, atLeastOnce()).getId();
        verify(assetRenderedFileEntity, atLeastOnce()).getIndexref();
        verify(assetRenderedFileEntity, atLeastOnce()).getName();
        verify(assetRenderedFileEntity, atLeastOnce()).isGzipEncoded();
        verify(fileEntity, atLeastOnce()).getRealm();
        verify(fileEntity, atLeastOnce()).getId();

    }

}
