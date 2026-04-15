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

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.io.File.createTempFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.getTempDirectoryPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.processlauncher.cmdline.Parameters.bulk;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import media.mexm.mydmam.configuration.ExternalToolsConf;
import media.mexm.mydmam.configuration.MagickConf;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.tools.ExternalExecCapabilityEvaluator;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@ExtendWith(MockToolsExtendsJunit.class)
class ImageMagickTest {

    private static final File MAGICK_TEMP_DIR = new File(getTempDirectoryPath(), "mydmam-test-magick-temp");
    static ScheduledExecutorService maxExecTimeScheduler;
    static ExecutableFinder executableFinder;

    @Mock
    MyDMAMConfigurationProperties configuration;
    @Mock
    ExternalToolsConf tools;
    @Mock
    XmlMapperWrapper xmlMapper;
    @Mock
    XmlMapper xmlMapperInternal;
    @Mock
    ObjectWriter xmlObjectWriter;
    @Mock
    ExternalExecCapabilities externalExecCapabilities;
    @Mock
    CapturedStdOutErrTextRetention capturedStdOutErrTextRetention;
    @Captor
    ArgumentCaptor<Predicate<ExternalExecCapabilityEvaluator>> evaluatorCaptor;
    @Mock
    ExternalExecCapabilityEvaluator evaluator;

    @Mock
    MagickConf magickConf;

    ObjectMapper objectMapper;
    File policyFile;
    ImageMagick im;

    @BeforeAll
    static void initAll() throws IOException {
        forceMkdir(MAGICK_TEMP_DIR);
        maxExecTimeScheduler = newScheduledThreadPool(1);
        executableFinder = new ExecutableFinder();
    }

    @BeforeEach
    void init() {
        objectMapper = new ObjectMapper();

        im = new ImageMagick(
                executableFinder,
                maxExecTimeScheduler,
                configuration,
                xmlMapper,
                objectMapper,
                externalExecCapabilities);

        when(configuration.tools()).thenReturn(tools);
        when(tools.magick()).thenReturn(magickConf);
        when(magickConf.maxExecTime()).thenReturn(ofSeconds(10));
        when(magickConf.tempDir()).thenReturn(MAGICK_TEMP_DIR.getAbsolutePath());
        when(magickConf.confDir()).thenReturn(MAGICK_TEMP_DIR.getAbsolutePath());
        when(magickConf.maxThreadCount()).thenReturn(1);

        when(xmlMapper.getXmlMapper()).thenReturn(xmlMapperInternal);
        when(xmlMapperInternal.writer()).thenReturn(xmlObjectWriter);
        when(xmlObjectWriter.withRootName("policymap")).thenReturn(xmlObjectWriter);
        when(xmlObjectWriter.with(INDENT_OUTPUT)).thenReturn(xmlObjectWriter);

        try {
            executableFinder.get("magick");
            when(externalExecCapabilities.getPassingPlaybookNames("magick")).thenReturn(Set.of("im"));
        } catch (final FileNotFoundException e) {
            when(externalExecCapabilities.getPassingPlaybookNames("magick")).thenReturn(Set.of());
        }
        try {
            executableFinder.get("convert");
            when(externalExecCapabilities.getPassingPlaybookNames("convert")).thenReturn(Set.of("im"));
        } catch (final FileNotFoundException e) {
            when(externalExecCapabilities.getPassingPlaybookNames("convert")).thenReturn(Set.of());
        }

        policyFile = new File(MAGICK_TEMP_DIR, "policy.xml");
    }

    @Test
    void testInit_noConf() {
        when(tools.magick()).thenReturn(null);
        im.internalServiceStart();
        verify(configuration, times(1)).tools();
        verify(tools, times(1)).magick();
        assertFalse(im.isEnabled());
    }

    @Test
    void testInit() throws IOException {
        im.internalServiceStart();

        verify(configuration, times(1)).tools();
        verify(tools, times(1)).magick();
        verify(magickConf, atLeastOnce()).maxExecTime();
        verify(magickConf, atLeastOnce()).tempDir();
        verify(magickConf, atLeastOnce()).confDir();
        verify(magickConf, atLeastOnce()).maxThreadCount();
        verify(magickConf, atLeastOnce()).maxMap();
        verify(magickConf, atLeastOnce()).maxMemory();
        verify(magickConf, atLeastOnce()).maxMemoryRequest();
        verify(magickConf, atLeastOnce()).maxDisk();
        verify(magickConf, atLeastOnce()).maxWidth();
        verify(magickConf, atLeastOnce()).maxHeight();

        verify(xmlMapper, atLeastOnce()).getXmlMapper();
        verify(xmlMapperInternal, atLeastOnce()).writer();
        verify(xmlObjectWriter, atLeastOnce()).withRootName("policymap");
        verify(xmlObjectWriter, atLeastOnce()).with(INDENT_OUTPUT);
        verify(xmlObjectWriter, times(1)).writeValue(eq(policyFile), any());

        assertTrue(im.isEnabled());

        checkExternalExecCapabilities();
    }

    private void setup() {
        im.internalServiceStart();
        reset(configuration, tools, magickConf, xmlMapper, xmlMapperInternal, xmlObjectWriter);
    }

    @Test
    void testGetMagickVersion() {
        assertThat(im.getMagickVersion()).isNull();

        when(evaluator.haveReturnCode(0)).thenReturn(true);
        when(evaluator.captured()).thenReturn(capturedStdOutErrTextRetention);
        when(capturedStdOutErrTextRetention.getStdoutLines(false)).then(_ -> Stream.of(
                "Version: ImageMagick 7.1.2-13 Q16-HDRI x64 dd991e2:20260119 https://imagemagick.org",
                "Copyright: (C) 1999 ImageMagick Studio LLC",
                "License: https://imagemagick.org/license/",
                "Features: Channel-masks(64-bit) Cipher DPC HDRI Modules OpenCL OpenMP(2.0)",
                "Delegates (built-in): bzlib cairo freetype gslib heic jng jp2 jpeg jxl lcms lqr lzma openexr pangocairo png ps raqm raw rsvg tiff webp xml zip zlib",
                "Compiler: Visual Studio 2022 (194435222)"));

        setup();

        checkExternalExecCapabilities();
        assertThat(im.getMagickVersion()).isEqualTo("7.1.2-13");

        verify(evaluator, atLeastOnce()).haveReturnCode(0);
        verify(evaluator, atLeastOnce()).captured();
        verify(evaluator, atLeastOnce()).name();
        verify(capturedStdOutErrTextRetention, atLeastOnce()).getStdoutLines(false);
    }

    private void checkExternalExecCapabilities() {
        verify(externalExecCapabilities, atLeast(0))
                .addPlaybook(eq("magick"), eq("im"), eq(bulk("-version")), evaluatorCaptor.capture());
        evaluatorCaptor.getValue().test(evaluator);

        verify(externalExecCapabilities, atLeast(0))
                .addPlaybook(eq("convert"), eq("im"), eq(bulk("-version")), evaluatorCaptor.capture());
        Optional.ofNullable(evaluatorCaptor.getValue()).ifPresent(c -> c.test(evaluator));

        verify(evaluator, atLeast(0)).haveReturnCode(0);

        verify(externalExecCapabilities, atLeast(0))
                .tearDown("magick");
        verify(externalExecCapabilities, atLeast(0))
                .tearDown("convert");
        verify(externalExecCapabilities, atLeast(0))
                .getPassingPlaybookNames("magick");
        verify(externalExecCapabilities, atLeast(0))
                .getPassingPlaybookNames("convert");
    }

    @Test
    void testIsEnabled() {
        assertFalse(im.isEnabled());
        setup();
        assertTrue(im.isEnabled());
        checkExternalExecCapabilities();
    }

    static File getWhitePng() {
        final var whitePng = new File("src/test/resources/white.png");
        assertThat(whitePng).exists();
        return whitePng;
    }

    @Test
    void testExtractIdentifyJsonFile() throws IOException {
        /**
         * Generated with "magick -size 16x16 xc:white -strip white.png"
         */
        final var whitePng = getWhitePng();

        final var saveJsonDest = createTempFile("mydmam-" + getClass().getSimpleName(), "identify.json");
        forceDelete(saveJsonDest);

        assertThrows(IOException.class,
                () -> im.extractIdentifyJsonFile(whitePng, saveJsonDest));
        setup();

        im.extractIdentifyJsonFile(whitePng, saveJsonDest);

        assertThat(whitePng).exists();
        assertThat(saveJsonDest)
                .exists()
                .content(UTF_8)
                .contains(
                        "\"version\":\"1.0\"",
                        whitePng.getName(),
                        "PNG",
                        "Portable Network Graphics");
        forceDelete(saveJsonDest);

        checkExternalExecCapabilities();
    }

    @Test
    void testGetManagedRasterMimeTypes() {
        assertThat(im.getManagedRasterMimeTypes())
                .hasSizeGreaterThan(10)
                .contains("image/jpeg", "image/png", "image/x-ms-bmp")
                .doesNotContain("application/pdf");
    }

    @Test
    void testConvertImage() throws IOException {
        final var whitePng = getWhitePng();

        final var createdImage = createTempFile("mydmam-" + getClass().getSimpleName(), "image.webp");
        forceDelete(createdImage);

        final var parameters = "<%INPUTFILE%>[0] -thumbnail 64x64 -profile <%ICCPROFILE%> <%OUTPUTFILE%>";

        assertThrows(IOException.class,
                () -> im.convertImage(parameters, whitePng, createdImage));
        setup();
        im.convertImage(parameters, whitePng, createdImage);

        assertThat(whitePng).exists();
        assertThat(createdImage).exists().size().isGreaterThan(100);
        forceDelete(createdImage);
        checkExternalExecCapabilities();
    }

}
