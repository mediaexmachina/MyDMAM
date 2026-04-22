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
package media.mexm.mydmam.activity.component;

import static java.io.File.createTempFile;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FULL_PREVIEW;
import static media.mexm.mydmam.service.MediaAssetService.FULL_TEXT_PDF;
import static org.apache.commons.io.FileUtils.touch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.FlatMetadataThesaurusService;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.component.XPDF;
import media.mexm.mydmam.component.XPDF.PageInfo;
import media.mexm.mydmam.configuration.ExternalToolsConf;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.configuration.XPDFConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusDefinitionWriter;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefDublinCore;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefPDF;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefXMP;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.repository.FileMetadataDao;
import media.mexm.mydmam.service.MediaAssetService;
import media.mexm.mydmam.service.MediaRenderedFilesUtilsService;
import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class PDFHandlerTest {

    @MockitoBean
    MyDMAMConfigurationProperties configuration;
    @MockitoBean
    XPDF xpdf;
    @MockitoBean
    MediaAssetService mediaAssetService;
    @MockitoBean
    MediaRenderedFilesUtilsService mediaRenderedFilesUtilsService;
    @MockitoBean
    FileMetadataDao fileMetadataDao;

    @Mock
    ActivityEventType eventType;
    @Mock
    RealmStorageConfiguredEnv storedOn;
    @Mock
    FileEntity fileEntity;
    @Mock
    Map<String, String> pdfInfo;
    @Mock
    ExternalToolsConf externalToolsConf;
    @Mock
    XPDFConf xPDFConf;

    @Fake(min = 10, max = 100)
    int pageCount;
    @Fake(min = 200, max = 1000)
    int maxPageCount;

    @Autowired
    FlatMetadataThesaurusService metadataThesaurusService;
    @Autowired
    PDFHandler pdf;

    File assetFile;

    @BeforeEach
    void init() {
        metadataThesaurusService.reset();
        assetFile = new File(".").getAbsoluteFile();
        when(storedOn.getLocalInternalFile(fileEntity)).thenReturn(assetFile);

        when(configuration.tools()).thenReturn(externalToolsConf);
        when(externalToolsConf.xpdf()).thenReturn(xPDFConf);
        when(xPDFConf.maxPageCount()).thenReturn(maxPageCount);
    }

    @AfterEach
    void ends() {
        metadataThesaurusService.endChecks(fileEntity);
        verifyNoMoreInteractions(
                xpdf,
                mediaAssetService,
                mediaRenderedFilesUtilsService,
                fileMetadataDao);
    }

    @Test
    void testGetLimitPolicy() {
        assertEquals(FILE_INFORMATION, pdf.getLimitPolicy());
    }

    @Test
    void testGetMetadataOriginName() {
        assertEquals("xpdf", pdf.getMetadataOriginName());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testIsEnabled(final boolean enabledPdfInfo) {
        when(xpdf.isEnabledPdfInfo()).thenReturn(enabledPdfInfo);
        assertEquals(enabledPdfInfo, pdf.isEnabled());
        verify(xpdf, times(1)).isEnabledPdfInfo();

    }

    @Test
    void testCanHandle() {
        metadataThesaurusService.setMimeType("null/null");

        assertFalse(pdf.canHandle(fileEntity, eventType, storedOn));

        when(storedOn.isDAS()).thenReturn(true);
        assertFalse(pdf.canHandle(fileEntity, eventType, storedOn));

        when(storedOn.haveWorkingDir()).thenReturn(true);
        assertFalse(pdf.canHandle(fileEntity, eventType, storedOn));

        metadataThesaurusService.setMimeType("application/pdf");
        assertTrue(pdf.canHandle(fileEntity, eventType, storedOn));

        verify(storedOn, atLeastOnce()).isDAS();
        verify(storedOn, atLeastOnce()).haveWorkingDir();
        metadataThesaurusService.endChecks(fileEntity);
    }

    @Test
    void testHandle_noPages() throws Exception {
        when(xpdf.pdfInfo(assetFile)).thenReturn(pdfInfo);
        when(xpdf.getInfo(pdfInfo, "Pages")).thenReturn(Optional.ofNullable("0"));

        pdf.handle(fileEntity, eventType, storedOn);

        verify(storedOn, times(1)).getLocalInternalFile(fileEntity);
        verify(xpdf, times(1)).pdfInfo(assetFile);
        verify(xpdf, times(1)).getInfo(pdfInfo, "Pages");
    }

    @Nested
    class Handle {

        @Mock
        RealmConf realmConf;

        @Fake
        String encrypted;
        @Fake
        String pdfVersion;
        @Fake
        String form;
        @Fake
        String producer;
        @Fake
        String keywords;
        @Fake
        int pageWidthMm;
        @Fake
        int pageHeightMm;
        @Fake
        int pageRotated;
        @Fake
        String creator;
        @Fake
        String metadataDate;
        @Fake
        String title;
        @Fake
        String subject;
        @Fake
        String author;
        @Fake(min = 10_000, max = 1_000_000)
        long createDate;
        @Fake(min = 10_000, max = 1_000_000)
        long modifyDate;

        @Fake(min = 10, max = 1000)
        float pageInfoW;
        @Fake(min = 10, max = 1000)
        float pageInfoH;
        @Fake(min = 0, max = 359)
        int pageInfoRotated;
        @Fake
        int id;
        @Fake
        boolean exists;

        @Captor
        ArgumentCaptor<Collection<FileMetadataEntity>> itemsCaptor;

        String optimized = Faker.instance().random().nextBoolean() ? "yes" : "no";
        String javascript = Faker.instance().random().nextBoolean() ? "yes" : "no";
        String tagged = Faker.instance().random().nextBoolean() ? "yes" : "no";
        String permissionPrint = Faker.instance().random().nextBoolean() ? "yes" : "no";
        String permissionCopy = Faker.instance().random().nextBoolean() ? "yes" : "no";
        String permissionChange = Faker.instance().random().nextBoolean() ? "yes" : "no";
        String permissionAddNotes = Faker.instance().random().nextBoolean() ? "yes" : "no";

        List<PageInfo> pagesFormats;
        MetadataThesaurusDefinitionWriter<MtdThesaurusDefPDF> pdfWriter;
        MtdThesaurusDefPDF mtdThesaurusDefPDF;

        File workingDirectory;
        File pdfExportDir;
        File pdfText;
        Map<Integer, File> exportedImagesByPages;
        String creationDate;
        String modDate;

        @BeforeEach
        void init() throws IOException {
            pdfWriter = metadataThesaurusService.getWriter(pdf, fileEntity, MtdThesaurusDefPDF.class);
            mtdThesaurusDefPDF = metadataThesaurusService.makeInstance(MtdThesaurusDefPDF.class);
            workingDirectory = new File(FileUtils.getTempDirectory(), "temp-mydmam");
            pdfExportDir = new File(workingDirectory, "pdftoppm-" + id);
            pdfText = new File(workingDirectory, "pdftotxt-" + id + ".txt");
            creationDate = Instant.ofEpochSecond(createDate).toString();
            modDate = Instant.ofEpochSecond(modifyDate).toString();

            when(fileEntity.getId()).thenReturn(id);
            when(xpdf.pdfInfo(assetFile)).thenReturn(pdfInfo);
            when(xpdf.getInfo(pdfInfo, "Pages"))
                    .thenReturn(Optional.ofNullable(String.valueOf(pageCount)));
            when(xpdf.getInfo(pdfInfo, "Encrypted"))
                    .thenReturn(Optional.ofNullable(String.valueOf(encrypted)));
            when(xpdf.getInfo(pdfInfo, "PDF version"))
                    .thenReturn(Optional.ofNullable(String.valueOf(pdfVersion)));
            when(xpdf.getInfo(pdfInfo, "Form"))
                    .thenReturn(Optional.ofNullable(String.valueOf(form)));
            when(xpdf.getInfo(pdfInfo, "Optimized"))
                    .thenReturn(Optional.ofNullable(String.valueOf(optimized)));
            when(xpdf.getInfo(pdfInfo, "JavaScript"))
                    .thenReturn(Optional.ofNullable(String.valueOf(javascript)));
            when(xpdf.getInfo(pdfInfo, "Tagged"))
                    .thenReturn(Optional.ofNullable(String.valueOf(tagged)));
            when(xpdf.getInfo(pdfInfo, "Producer"))
                    .thenReturn(Optional.ofNullable(String.valueOf(producer)));
            when(xpdf.getInfo(pdfInfo, "Keywords"))
                    .thenReturn(Optional.ofNullable(String.valueOf(keywords)));
            when(xpdf.getInfo(pdfInfo, "Creator"))
                    .thenReturn(Optional.ofNullable(String.valueOf(creator)));
            when(xpdf.getInfo(pdfInfo, "MetadataDate"))
                    .thenReturn(Optional.ofNullable(String.valueOf(metadataDate)));
            when(xpdf.getInfo(pdfInfo, "CreationDate"))
                    .thenReturn(Optional.ofNullable(String.valueOf(creationDate)));
            when(xpdf.getInfo(pdfInfo, "ModDate"))
                    .thenReturn(Optional.ofNullable(String.valueOf(modDate)));
            when(xpdf.getInfo(pdfInfo, "Title"))
                    .thenReturn(Optional.ofNullable(String.valueOf(title)));
            when(xpdf.getInfo(pdfInfo, "Subject"))
                    .thenReturn(Optional.ofNullable(String.valueOf(subject)));
            when(xpdf.getInfo(pdfInfo, "Author"))
                    .thenReturn(Optional.ofNullable(String.valueOf(author)));

            pagesFormats = IntStream.range(1, pageCount + 1)
                    .mapToObj(page -> new PageInfo(page, pageInfoW, pageInfoH, pageInfoRotated))
                    .toList();
            when(xpdf.extractPagesFormats(pdfInfo, maxPageCount)).thenReturn(pagesFormats);
            when(storedOn.haveRenderedDir()).thenReturn(true);
            when(storedOn.haveWorkingDir()).thenReturn(true);
            when(storedOn.realm()).thenReturn(realmConf);
            when(storedOn.getActivityLimitPolicy()).thenReturn(FULL_PREVIEW);
            when(realmConf.workingDirectory()).thenReturn(workingDirectory);
        }

        @AfterEach
        void ends() throws IOException {
            verify(storedOn, times(1)).getLocalInternalFile(fileEntity);
            verify(xpdf, times(1)).pdfInfo(assetFile);
            verify(xpdf, times(1)).getInfo(pdfInfo, "Pages");

            verify(configuration, atLeastOnce()).tools();
            verify(externalToolsConf, times(1)).xpdf();
            verify(xPDFConf, times(1)).maxPageCount();
        }

        void checkIfAddedMtd(final boolean samePages) {
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefPDF.class, pageCount).pageCount();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefPDF.class, encrypted).encrypted();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefPDF.class, pdfVersion).pdfVersion();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefPDF.class, form).form();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefPDF.class, optimized == "yes").optimized();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefPDF.class, javascript == "yes").javascript();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefPDF.class, tagged == "yes").tagged();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefPDF.class, producer).producer();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefPDF.class, keywords).keywords();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefPDF.class, samePages).samePagesFormat();

            metadataThesaurusService.checkIfAdded(MtdThesaurusDefXMP.class, createDate).createDate();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefXMP.class, modifyDate).modifyDate();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefXMP.class, creator).creatorTool();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefXMP.class, metadataDate).metadataDate();

            metadataThesaurusService.checkIfAdded(MtdThesaurusDefDublinCore.class, title).title();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefDublinCore.class, subject).description();
            metadataThesaurusService.checkIfAdded(MtdThesaurusDefDublinCore.class, author).creator();

            verify(xpdf, atLeastOnce()).getInfo(eq(pdfInfo), anyString());
            verify(xpdf, times(1)).extractPagesFormats(pdfInfo, maxPageCount);
        }

        @ParameterizedTest
        @ValueSource(booleans = { false, true })
        void testInfoOnly_noRenderers(final boolean samePages) throws Exception {
            if (samePages == false) {
                pagesFormats = IntStream.range(1, pageCount + 1)
                        .mapToObj(page -> new PageInfo(
                                page,
                                page == 1 ? pageInfoW / 2 : pageInfoW,
                                page == 1 ? pageInfoH / 2 : pageInfoH,
                                page == 1 ? pageInfoRotated / 2 : pageInfoRotated))
                        .toList();
                when(xpdf.extractPagesFormats(pdfInfo, maxPageCount)).thenReturn(pagesFormats);
            }

            when(storedOn.haveRenderedDir()).thenReturn(false);
            when(storedOn.haveWorkingDir()).thenReturn(false);

            pdf.handle(fileEntity, eventType, storedOn);

            checkIfAddedMtd(samePages);

            verify(xpdf, times(1)).extractPermissions(pdfInfo, pdfWriter);

            verify(fileMetadataDao, times(1)).addUpdateEntries(eq(fileEntity), itemsCaptor.capture());
            final var items = itemsCaptor.getValue();

            assertThat(items).hasSize(samePages ? 3 : 3 * pageCount);
            assertThat(items).map(FileMetadataEntity::getFile).containsOnly(fileEntity);
            assertThat(items).map(FileMetadataEntity::getOrigin).containsOnly("xpdf");

            if (samePages) {
                assertThat(items).map(FileMetadataEntity::getLayer).containsOnly(0);
            } else {
                final var layerList = IntStream.range(1, pageCount + 1)
                        .mapToObj(i -> i)
                        .flatMap(i -> Stream.of(i, i, i))
                        .toList();
                assertThat(items).map(FileMetadataEntity::getLayer).isEqualTo(layerList);
            }

            assertThat(items).map(FileMetadataEntity::getClassifier).containsOnly("pdf");

            final var rotatedAssert = assertThat(items.stream()
                    .filter(f -> f.getKey().equals(mtdThesaurusDefPDF.pageRotated().key())))
                            .map(FileMetadataEntity::getValue);
            if (samePages) {
                rotatedAssert.containsOnly(
                        String.valueOf(pagesFormats.get(0).rotated()));
            } else {
                rotatedAssert.containsOnly(
                        String.valueOf(pagesFormats.get(0).rotated()),
                        String.valueOf(pagesFormats.get(1).rotated()));
            }

            final var pageWidthAssert = assertThat(items.stream()
                    .filter(f -> f.getKey().equals(mtdThesaurusDefPDF.pageWidthMm().key())))
                            .map(FileMetadataEntity::getValue);
            if (samePages) {
                pageWidthAssert.containsOnly(
                        String.valueOf(pagesFormats.get(0).getWMm()));
            } else {
                pageWidthAssert.containsOnly(
                        String.valueOf(pagesFormats.get(0).getWMm()),
                        String.valueOf(pagesFormats.get(1).getWMm()));
            }

            final var pageHeightAssert = assertThat(items.stream()
                    .filter(f -> f.getKey().equals(mtdThesaurusDefPDF.pageHeightMm().key())))
                            .map(FileMetadataEntity::getValue);
            if (samePages) {
                pageHeightAssert.containsOnly(
                        String.valueOf(pagesFormats.get(0).getHMm()));
            } else {
                pageHeightAssert.containsOnly(
                        String.valueOf(pagesFormats.get(0).getHMm()),
                        String.valueOf(pagesFormats.get(1).getHMm()));
            }

            verify(storedOn, atLeastOnce()).haveRenderedDir();
            verify(storedOn, atLeastOnce()).haveWorkingDir();
            verify(storedOn, atLeastOnce()).realmName();
            verify(storedOn, atLeastOnce()).storageName();
            verify(storedOn, atLeastOnce()).getActivityLimitPolicy();
            reset(fileEntity);
        }

        @Test
        void testInfoOnly_withRenderers() throws Exception {
            pdf.handle(fileEntity, eventType, storedOn);

            checkIfAddedMtd(true);

            verify(fileMetadataDao, times(1)).addUpdateEntries(eq(fileEntity), any());
            verify(storedOn, atLeastOnce()).haveRenderedDir();
            verify(storedOn, atLeastOnce()).haveWorkingDir();
            verify(storedOn, atLeastOnce()).getActivityLimitPolicy();
            verify(xpdf, times(1)).extractPermissions(pdfInfo, pdfWriter);
            verify(xpdf, times(1)).isEnabledPdfToPpm();
            verify(xpdf, times(1)).isEnabledPdfToText();
            reset(fileEntity);
        }

        @ParameterizedTest
        @ValueSource(booleans = { false, true })
        void testPdfToPpm_noThumbnails(final boolean exists) throws Exception {
            if (exists) {
                FileUtils.forceMkdir(pdfExportDir);
            }

            exportedImagesByPages = Map.of();
            when(xpdf.pdfToPPM(assetFile, pdfExportDir, pageCount)).thenReturn(exportedImagesByPages);
            when(xpdf.isEnabledPdfToPpm()).thenReturn(true);

            pdf.handle(fileEntity, eventType, storedOn);

            checkIfAddedMtd(true);

            verify(fileMetadataDao, times(1)).addUpdateEntries(eq(fileEntity), any());
            verify(storedOn, atLeastOnce()).haveRenderedDir();
            verify(storedOn, atLeastOnce()).haveWorkingDir();
            verify(storedOn, atLeastOnce()).realm();
            verify(storedOn, atLeastOnce()).getActivityLimitPolicy();
            verify(realmConf, atLeastOnce()).workingDirectory();
            verify(xpdf, times(1)).extractPermissions(pdfInfo, pdfWriter);
            verify(xpdf, times(1)).isEnabledPdfToPpm();
            verify(xpdf, times(1)).isEnabledPdfToText();
            verify(xpdf, times(1)).pdfToPPM(assetFile, pdfExportDir, pageCount);
            reset(fileEntity);
        }

        @Test
        void testPdfToPpm() throws Exception {
            final var exportedImage1 = createTempFile("mydmam-temp-" + getClass().getSimpleName(), ".image1");
            final var exportedImage2 = createTempFile("mydmam-temp-" + getClass().getSimpleName(), ".image2");
            assertThat(exportedImage1).exists();
            assertThat(exportedImage2).exists();

            exportedImagesByPages = Map.of(
                    1, exportedImage1,
                    2, exportedImage2);
            when(xpdf.pdfToPPM(assetFile, pdfExportDir, pageCount)).thenReturn(exportedImagesByPages);
            when(xpdf.isEnabledPdfToPpm()).thenReturn(true);

            pdf.handle(fileEntity, eventType, storedOn);

            checkIfAddedMtd(true);

            verify(fileMetadataDao, times(1)).addUpdateEntries(eq(fileEntity), any());
            verify(storedOn, atLeastOnce()).haveRenderedDir();
            verify(storedOn, atLeastOnce()).haveWorkingDir();
            verify(storedOn, atLeastOnce()).realm();
            verify(storedOn, atLeastOnce()).getActivityLimitPolicy();
            verify(realmConf, atLeastOnce()).workingDirectory();
            verify(xpdf, times(1)).extractPermissions(pdfInfo, pdfWriter);
            verify(xpdf, times(1)).isEnabledPdfToPpm();
            verify(xpdf, times(1)).isEnabledPdfToText();
            verify(xpdf, times(1)).pdfToPPM(assetFile, pdfExportDir, pageCount);
            verify(mediaRenderedFilesUtilsService, times(1))
                    .makeImageThumbnails(fileEntity, storedOn, exportedImage1, false, 0);
            verify(mediaRenderedFilesUtilsService, times(1))
                    .makeImageThumbnails(fileEntity, storedOn, exportedImage2, false, 2);
            reset(fileEntity);

            assertThat(exportedImage1).doesNotExist();
            assertThat(exportedImage2).doesNotExist();
            assertThat(pdfExportDir).doesNotExist();
        }

        @Test
        void testPdfToPpm_lowerActivityLimitPolicy() throws Exception {
            when(storedOn.getActivityLimitPolicy()).thenReturn(FILE_INFORMATION);

            pdf.handle(fileEntity, eventType, storedOn);

            checkIfAddedMtd(true);

            verify(fileMetadataDao, times(1)).addUpdateEntries(eq(fileEntity), any());
            verify(storedOn, atLeastOnce()).getActivityLimitPolicy();
            verify(storedOn, atLeastOnce()).realmName();
            verify(storedOn, atLeastOnce()).storageName();
            verify(xpdf, times(1)).extractPermissions(pdfInfo, pdfWriter);
            reset(fileEntity);
        }

        @ParameterizedTest
        @ValueSource(booleans = { false, true })
        void testPdfToText(final boolean exists) throws Exception {
            if (exists) {
                touch(pdfText);
            }

            when(xpdf.isEnabledPdfToText()).thenReturn(true);

            pdf.handle(fileEntity, eventType, storedOn);

            checkIfAddedMtd(true);

            verify(fileMetadataDao, times(1)).addUpdateEntries(eq(fileEntity), any());
            verify(storedOn, atLeastOnce()).haveRenderedDir();
            verify(storedOn, atLeastOnce()).haveWorkingDir();
            verify(storedOn, atLeastOnce()).realm();
            verify(storedOn, atLeastOnce()).getActivityLimitPolicy();
            verify(realmConf, atLeastOnce()).workingDirectory();
            verify(xpdf, times(1)).extractPermissions(pdfInfo, pdfWriter);
            verify(xpdf, times(1)).isEnabledPdfToPpm();
            verify(xpdf, times(1)).isEnabledPdfToText();
            verify(xpdf, times(1)).pdfToText(assetFile, pdfText, pageCount);

            if (exists) {
                verify(mediaAssetService, times(1)).declareTextExtractedFile(fileEntity, pdfText, FULL_TEXT_PDF);
            }

            reset(fileEntity);
        }

    }

}
