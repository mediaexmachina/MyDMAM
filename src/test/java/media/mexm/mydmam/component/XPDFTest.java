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
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static media.mexm.mydmam.component.XPDF.FORM_FEED;
import static media.mexm.mydmam.component.XPDF.splitFirstColon;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.getTempDirectoryPath;
import static org.apache.commons.io.FileUtils.touch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.processlauncher.cmdline.Parameters.bulk;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import media.mexm.mydmam.FlatMetadataThesaurusService;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.component.XPDF.PageInfo;
import media.mexm.mydmam.configuration.ExternalToolsConf;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.XPDFConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusRegister;
import media.mexm.mydmam.tools.ExternalExecCapabilityEvaluator;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.InvalidExecution;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@ExtendWith(MockToolsExtendsJunit.class)
class XPDFTest {

    private static final int MAX_PAGE_COUNT = 100;
    private static final File PDF_TEMP_DIR = new File(getTempDirectoryPath(), "mydmam-test-pdf-temp");
    static ScheduledExecutorService maxExecTimeScheduler;
    static ExecutableFinder executableFinder;

    @Mock
    MyDMAMConfigurationProperties configuration;
    @Mock
    ExternalToolsConf tools;
    @Mock
    XPDFConf xpdfConf;

    @Mock
    ActivityHandler handler;
    @Mock
    FileEntity fileEntity;
    @Mock
    ExternalExecCapabilities externalExecCapabilities;
    @Mock
    CapturedStdOutErrTextRetention capturedStdOutErrTextRetention;
    @Captor
    ArgumentCaptor<Predicate<ExternalExecCapabilityEvaluator>> evaluatorCaptor;
    @Mock
    ExternalExecCapabilityEvaluator evaluator;

    @Fake
    boolean print;
    @Fake
    boolean copy;
    @Fake
    boolean change;
    @Fake
    boolean addNotes;
    @Fake
    int rotated;
    @Fake
    int page;

    FlatMetadataThesaurusService metadataThesaurusService;
    MetadataThesaurusRegister assertThesaurus;
    XPDF xpdf;

    @BeforeAll
    static void initAll() throws IOException {
        forceMkdir(PDF_TEMP_DIR);
        maxExecTimeScheduler = newScheduledThreadPool(1);
        executableFinder = new ExecutableFinder();
    }

    @BeforeEach
    void init() {
        xpdf = new XPDF(executableFinder, maxExecTimeScheduler, configuration, externalExecCapabilities);

        metadataThesaurusService = new FlatMetadataThesaurusService();
        assertThesaurus = metadataThesaurusService.getAssertThesaurus();

        when(handler.getMetadataOriginName()).thenReturn("");
        reset(handler);

        when(configuration.tools()).thenReturn(tools);
        when(tools.xpdf()).thenReturn(xpdfConf);
        when(xpdfConf.maxPageCount()).thenReturn(MAX_PAGE_COUNT);
        when(xpdfConf.tempDir()).thenReturn(PDF_TEMP_DIR.getAbsolutePath());
        when(xpdfConf.resolution()).thenReturn(75);
        when(xpdfConf.maxExecTime()).thenReturn(ofSeconds(10));
        when(externalExecCapabilities.getPassingPlaybookNames("pdfinfo")).thenReturn(Set.of("info"));
        when(externalExecCapabilities.getPassingPlaybookNames("pdftoppm")).thenReturn(Set.of("image"));
        when(externalExecCapabilities.getPassingPlaybookNames("pdftotext")).thenReturn(Set.of("text"));
    }

    @AfterEach
    void ends() {
        metadataThesaurusService.check();
        reset(fileEntity);
    }

    @Test
    void testGetInternalServiceName() {
        assertEquals("XPDF", xpdf.getInternalServiceName());
    }

    @Test
    void testInternalServiceStart_noConf() throws Exception {
        when(tools.xpdf()).thenReturn(null);
        xpdf.internalServiceStart();
        verify(configuration, times(1)).tools();
        verify(tools, times(1)).xpdf();
        assertFalse(xpdf.isEnabledPdfInfo());
        assertFalse(xpdf.isEnabledPdfToPpm());
        assertFalse(xpdf.isEnabledPdfToText());
    }

    private void checkExternalExecCapabilities() {
        verify(externalExecCapabilities, atLeast(0))
                .addPlaybook(eq("pdfinfo"), eq("info"), eq(bulk("-v")), evaluatorCaptor.capture());
        verify(externalExecCapabilities, atLeast(0))
                .addPlaybook(eq("pdftoppm"), eq("image"), eq(bulk("-v")), evaluatorCaptor.capture());
        verify(externalExecCapabilities, atLeast(0))
                .addPlaybook(eq("pdftotext"), eq("text"), eq(bulk("-v")), evaluatorCaptor.capture());

        evaluatorCaptor.getValue().test(evaluator);

        verify(evaluator, atLeast(0)).haveReturnCode(0, 99);
        verify(externalExecCapabilities, atLeastOnce())
                .tearDown("pdfinfo");
        verify(externalExecCapabilities, atLeastOnce())
                .tearDown("pdftoppm");
        verify(externalExecCapabilities, atLeastOnce())
                .tearDown("pdftotext");
        verify(externalExecCapabilities, atLeastOnce())
                .getPassingPlaybookNames("pdfinfo");
        verify(externalExecCapabilities, atLeastOnce())
                .getPassingPlaybookNames("pdftoppm");
        verify(externalExecCapabilities, atLeastOnce())
                .getPassingPlaybookNames("pdftotext");
    }

    @Test
    void testInternalServiceStart() throws Exception {
        xpdf.internalServiceStart();

        assertTrue(xpdf.isEnabledPdfInfo());
        assertTrue(xpdf.isEnabledPdfToPpm());
        assertTrue(xpdf.isEnabledPdfToText());

        verify(configuration, times(1)).tools();
        verify(tools, times(1)).xpdf();
        verify(xpdfConf, times(1)).maxPageCount();
        verify(xpdfConf, times(1)).resolution();
        verify(xpdfConf, times(1)).tempDir();
        verify(xpdfConf, times(1)).maxExecTime();
        checkExternalExecCapabilities();
    }

    @Test
    void testIsEnabled() throws Exception {
        assertFalse(xpdf.isEnabledPdfInfo());
        assertFalse(xpdf.isEnabledPdfToPpm());
        assertFalse(xpdf.isEnabledPdfToText());

        setup();

        assertTrue(xpdf.isEnabledPdfInfo());
        assertTrue(xpdf.isEnabledPdfToPpm());
        assertTrue(xpdf.isEnabledPdfToText());
    }

    private void setup() throws Exception {
        xpdf.internalServiceStart();

        verify(configuration, atLeastOnce()).tools();
        verify(tools, atLeastOnce()).xpdf();
        verify(xpdfConf, atLeastOnce()).maxExecTime();
        verify(xpdfConf, atLeastOnce()).tempDir();
        verify(xpdfConf, atLeastOnce()).maxPageCount();
        verify(xpdfConf, atLeastOnce()).resolution();

        checkExternalExecCapabilities();
    }

    static File getImagePdf() {
        final var imagePdf = new File("src/test/resources/image.pdf");
        assertThat(imagePdf).exists();
        return imagePdf;
    }

    @Test
    void testPdfInfo() throws Exception {
        /**
         * Generated with Inkscape from "image.svg"
         */
        final var imagePdf = getImagePdf();

        assertThrows(IOException.class,
                () -> xpdf.pdfInfo(imagePdf));

        setup();
        final var pdfInfo = xpdf.pdfInfo(imagePdf);

        assertThat(pdfInfo)
                .hasSizeGreaterThanOrEqualTo(11)
                .containsKeys(
                        "page    1 size",
                        "creationdate",
                        "producer",
                        "creator")
                .containsEntry("pdf version", "1.5")
                .containsEntry("pages", "1")
                .containsEntry("form", "none")
                .containsEntry("encrypted", "no")
                .containsEntry("tagged", "no")
                .containsEntry("optimized", "no")
                .containsEntry("javascript", "no");

        assertThat(pdfInfo.values())
                .anyMatch(v -> v.contains("29 x 29 pts"))
                .anyMatch(v -> v.contains("cairo"))
                .anyMatch(v -> v.contains("Inkscape"));
    }

    @Test
    void testExtractPagesFormats() {
        final var pdfInfo = Map.ofEntries(
                Map.entry("pdf version", "1.5"),
                Map.entry("page    1 size", "29.1 x 29.2 pts (rotated 50 degrees)"),
                Map.entry("page    2 size", "29 x 29 pts"),
                /**
                 * Non managed (old versions)
                 */
                Map.entry("page    2 rot", "20"),
                Map.entry("pages", "1"),
                /**
                 * Bad page entries
                 */
                Map.entry("page    30 size", "29"),
                Map.entry("page    31 size", "29 NOPE 29 pts (rotated 0 degrees)"),
                Map.entry("page    32 size", "29 x 29 NOPE (rotated 0 degrees)"),
                Map.entry("page    33 size", "29 x 29 pts (rotated 0 NOPE"),
                Map.entry("page    34 size", "29 x 29 pts NOPE 0 degrees)"),
                Map.entry("page 3 size", "29 x 29 pts (A4) (rotated 50 degrees)"),
                Map.entry("page   4 size", "29 x 29 pts (rotated 0 degrees)"),
                Map.entry("page    5 size", "30 x 30 pts (rotated 10 degrees)"));

        final var pagesInfos = xpdf.extractPagesFormats(pdfInfo, 4);
        assertThat(pagesInfos)
                .hasSize(4)
                .containsExactly(
                        new PageInfo(1, Float.valueOf("29.1"), Float.valueOf("29.2"), 50),
                        new PageInfo(2, Float.valueOf("29"), Float.valueOf("29"), 0),
                        new PageInfo(3, Float.valueOf("29"), Float.valueOf("29"), 50),
                        new PageInfo(4, Float.valueOf("29"), Float.valueOf("29"), 0));
    }

    @Test
    void testGetInfo() {
        final var pdfInfo = Map.of(
                "pdf version", "1",
                "pages", "2");
        assertThat(xpdf.getInfo(pdfInfo, "Pages")).contains("2");
        assertThat(xpdf.getInfo(pdfInfo, "pages")).contains("2");
        assertThat(xpdf.getInfo(pdfInfo, "Nope")).isEmpty();
    }

    @Test
    void testExtractPermissions() {
        final var perms = new StringBuilder();
        perms.append("print:");
        perms.append(print ? "yes" : "no");
        perms.append(" copy:");
        perms.append(copy ? "yes" : "no");
        perms.append(" change:");
        perms.append(change ? "yes" : "no");
        perms.append(" NOPE");
        perms.append(" addNotes:");
        perms.append(addNotes ? "yes" : "no");

        final var pdfInfo = Map.of(
                "pdf version", "1",
                "permissions", perms.toString());

        xpdf.extractPermissions(pdfInfo, metadataThesaurusService.getThesaurus(handler, fileEntity).pdf());

        assertThesaurus.pdf().permissionPrint().set(print);
        assertThesaurus.pdf().permissionCopy().set(copy);
        assertThesaurus.pdf().permissionChange().set(change);
        assertThesaurus.pdf().permissionAddNotes().set(addNotes);
        metadataThesaurusService.check(fileEntity).check(handler);
    }

    @Test
    void testExtractPermissions_empty() {
        xpdf.extractPermissions(Map.of("pdf version", "1"),
                metadataThesaurusService.getThesaurus(handler, fileEntity).pdf());

        assertThesaurus.pdf().permissionPrint().set(true);
        assertThesaurus.pdf().permissionCopy().set(true);
        assertThesaurus.pdf().permissionChange().set(true);
        assertThesaurus.pdf().permissionAddNotes().set(true);
        metadataThesaurusService.check(fileEntity).check(handler);
    }

    @Test
    void testPdfToPPM() throws Exception {
        /**
         * Generated with Inkscape from "image.svg"
         */
        final var imagePdf = getImagePdf();

        assertThrows(IOException.class,
                () -> xpdf.pdfToPPM(imagePdf, PDF_TEMP_DIR, 1));
        setup();
        assertThrows(IllegalArgumentException.class,
                () -> xpdf.pdfToPPM(imagePdf, PDF_TEMP_DIR, MAX_PAGE_COUNT + 1));

        final var producedFiles = xpdf.pdfToPPM(imagePdf, PDF_TEMP_DIR, 1);

        assertThat(producedFiles).hasSize(1).containsKey(1);
        final var producedFile = producedFiles.get(1);
        assertThat(producedFile).exists().size().isGreaterThan(10);
        assertThat(producedFile.getParentFile().getAbsolutePath())
                .isEqualTo(PDF_TEMP_DIR.getAbsolutePath());
        deleteQuietly(producedFile);
    }

    @Test
    void testPdfToPPM_badPdf() throws Exception {
        final var imagePdf = new File("src/test/resources/white.png");
        setup();

        assertThrows(InvalidExecution.class,
                () -> xpdf.pdfToPPM(imagePdf, PDF_TEMP_DIR, 1));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testPdfToText(final boolean exists) throws Exception {
        /**
         * Generated with Inkscape from "image.svg"
         */
        final var imagePdf = getImagePdf();
        final var destFile = new File(PDF_TEMP_DIR, "temp-" + System.currentTimeMillis() + ".txt");
        if (exists) {
            touch(destFile);
        }

        assertThrows(IOException.class,
                () -> xpdf.pdfToText(imagePdf, destFile, 1));
        setup();
        assertThrows(IllegalArgumentException.class,
                () -> xpdf.pdfToText(imagePdf, destFile, MAX_PAGE_COUNT + 1));

        xpdf.pdfToText(imagePdf, destFile, 1);

        final char[] formFeed = { FORM_FEED };
        assertThat(destFile)
                .exists()
                .content(UTF_8)
                .startsWith("MyDMAM")
                .endsWith(String.valueOf(formFeed));
        deleteQuietly(destFile);
    }

    @Test
    void testPdfToText_badPdf() throws Exception {
        final var imagePdf = new File("src/test/resources/white.png");
        final var destFile = new File(PDF_TEMP_DIR, "temp-" + System.currentTimeMillis() + ".txt");
        setup();

        assertThrows(InvalidExecution.class,
                () -> xpdf.pdfToText(imagePdf, destFile, 1));
    }

    @Test
    void testPageInfo() {
        final var w = 21 * (72 / 25.4);
        final var h = 29.7 * (72 / 25.4);
        final var pi = new PageInfo(page, (float) w, (float) h, rotated);

        assertEquals("21.0", pi.getWMm());
        assertEquals("29.7", pi.getHMm());
    }

    @Test
    void testSplitFirstColon() {
        assertThat(splitFirstColon("")).isEmpty();
        assertThat(splitFirstColon(":NOPE")).isEmpty();
        assertThat(splitFirstColon("NOPE:")).isEmpty();
        assertThat(splitFirstColon(" A : B ")).containsOnly(Map.entry("a", "B"));
    }

}
