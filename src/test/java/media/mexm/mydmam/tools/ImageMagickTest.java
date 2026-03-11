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
package media.mexm.mydmam.tools;

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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import media.mexm.mydmam.ConditionalExternalExecTest;
import media.mexm.mydmam.component.XmlMapperWrapper;
import media.mexm.mydmam.configuration.MagickConf;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@ExtendWith(MockToolsExtendsJunit.class)
class ImageMagickTest {

	private static final File MAGICK_TEMP_DIR = new File(getTempDirectoryPath(), "mydmam-test-magick-temp");
	static ScheduledExecutorService maxExecTimeScheduler;
	static ExecutableFinder executableFinder;

	@Mock
	MyDMAMConfigurationProperties configuration;
	@Mock
	XmlMapperWrapper xmlMapper;
	@Mock
	XmlMapper xmlMapperInternal;
	@Mock
	ObjectWriter xmlObjectWriter;

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

		im = new ImageMagick(executableFinder, maxExecTimeScheduler, configuration, xmlMapper, objectMapper);

		when(configuration.magick()).thenReturn(magickConf);
		when(magickConf.maxExecTime()).thenReturn(ofSeconds(10));
		when(magickConf.tempDir()).thenReturn(MAGICK_TEMP_DIR.getAbsolutePath());
		when(magickConf.confDir()).thenReturn(MAGICK_TEMP_DIR.getAbsolutePath());
		when(magickConf.maxThreadCount()).thenReturn(1);

		when(xmlMapper.getXmlMapper()).thenReturn(xmlMapperInternal);
		when(xmlMapperInternal.writer()).thenReturn(xmlObjectWriter);
		when(xmlObjectWriter.withRootName("policymap")).thenReturn(xmlObjectWriter);
		when(xmlObjectWriter.with(INDENT_OUTPUT)).thenReturn(xmlObjectWriter);

		policyFile = new File(MAGICK_TEMP_DIR, "policy.xml");
	}

	@Test
	void testInit_noConf() {
		when(configuration.magick()).thenReturn(null);
		im.init();
		verify(configuration, times(1)).magick();
		assertFalse(im.isEnabled());
	}

	@Test
	void testInit() throws IOException {
		im.init();

		verify(configuration, times(1)).magick();
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
	}

	private void setup() {
		im.init();
		reset(configuration, magickConf, xmlMapper, xmlMapperInternal, xmlObjectWriter);
	}

	@Test
	void testGetMagickVersion() {
		assertThat(im.getMagickVersion()).isNull();
		setup();
		assertThat(im.getMagickVersion()).isNotBlank();
	}

	@Test
	@ConditionalExternalExecTest
	void testIsEnabled() {
		assertFalse(im.isEnabled());
		setup();
		assertTrue(im.isEnabled());
	}

	@Test
	@ConditionalExternalExecTest
	void testExtractIdentifyJsonFile() throws IOException {
		/**
		 * Generated with "magick -size 16x16 xc:white -strip white.png"
		 */
		final var whitePng = new File("src/test/resources/white.png");
		assertThat(whitePng).exists();

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
	}

}
