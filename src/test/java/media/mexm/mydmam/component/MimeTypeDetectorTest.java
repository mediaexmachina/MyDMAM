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

import static media.mexm.mydmam.component.MimeTypeDetector.DEFAULT_MIME_TYPE;
import static media.mexm.mydmam.component.MimeTypeDetector.castList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil2;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class MimeTypeDetectorTest {

	@MockitoBean
	MimeUtil2 magicMimeUtil;
	@MockitoBean
	MimeUtil2 extensionMimeUtil;

	@Captor
	ArgumentCaptor<File> fileCaptor;

	@Fake
	String baseFilename;
	@Fake
	String extension;
	@Fake
	String mimeCodeType;
	@Fake
	String otherMimeCodeType;
	@Mock
	MimeType mimeType;
	@Mock
	MimeType otherMimeType;

	@Autowired
	MimeTypeDetector mtd;

	File source;

	@BeforeEach
	void init() {
		source = new File(baseFilename + "." + extension);
		when(mimeType.toString()).thenReturn(mimeCodeType);
		when(otherMimeType.toString()).thenReturn(otherMimeCodeType);
	}

	@Test
	void testGetMimeTypeString() {
		final var filename = baseFilename + "." + extension;
		when(extensionMimeUtil.getMimeTypes(any(File.class))).thenReturn(List.of(mimeType));
		when(mimeType.getSpecificity()).thenReturn(1);

		assertEquals(mimeCodeType, mtd.getMimeType(filename));

		verify(extensionMimeUtil, times(1)).getMimeTypes(fileCaptor.capture());
		assertThat(fileCaptor.getValue().getPath()).endsWith("." + extension);
		verify(mimeType, atLeast(1)).getSpecificity();
	}

	@Test
	void testGetMimeTypeFile_doubleDetection() {
		when(magicMimeUtil.getMimeTypes(source)).thenReturn(List.of(mimeType));
		when(extensionMimeUtil.getMimeTypes(source)).thenReturn(List.of(mimeType));

		assertEquals(mimeCodeType, mtd.getMimeType(source));

		verify(magicMimeUtil, times(1)).getMimeTypes(source);
		verify(extensionMimeUtil, times(1)).getMimeTypes(source);
	}

	@Test
	void testGetMimeTypeFile_magicDetection() {
		when(magicMimeUtil.getMimeTypes(source)).thenReturn(List.of(mimeType));
		when(extensionMimeUtil.getMimeTypes(source)).thenReturn(List.of());

		assertEquals(mimeCodeType, mtd.getMimeType(source));

		verify(magicMimeUtil, times(1)).getMimeTypes(source);
		verify(extensionMimeUtil, times(1)).getMimeTypes(source);
	}

	@Test
	void testGetMimeTypeFile_extensionDetection() {
		when(magicMimeUtil.getMimeTypes(source)).thenReturn(List.of());
		when(extensionMimeUtil.getMimeTypes(source)).thenReturn(List.of(mimeType));

		assertEquals(mimeCodeType, mtd.getMimeType(source));

		verify(magicMimeUtil, times(1)).getMimeTypes(source);
		verify(extensionMimeUtil, times(1)).getMimeTypes(source);
	}

	@Test
	void testGetMimeTypeFile_nothing() {
		when(magicMimeUtil.getMimeTypes(source)).thenReturn(List.of());
		when(extensionMimeUtil.getMimeTypes(source)).thenReturn(List.of());

		assertEquals(DEFAULT_MIME_TYPE, mtd.getMimeType(source));

		verify(magicMimeUtil, times(1)).getMimeTypes(source);
		verify(extensionMimeUtil, times(1)).getMimeTypes(source);
	}

	@Test
	void testGetMimeTypeFile_compare() {
		when(magicMimeUtil.getMimeTypes(source)).thenReturn(List.of(mimeType));
		when(extensionMimeUtil.getMimeTypes(source)).thenReturn(List.of(otherMimeType));
		when(mimeType.compareTo(otherMimeType)).thenReturn(-1);
		when(otherMimeType.compareTo(mimeType)).thenReturn(1);

		assertEquals(mimeCodeType, mtd.getMimeType(source));

		verify(magicMimeUtil, times(1)).getMimeTypes(source);
		verify(extensionMimeUtil, times(1)).getMimeTypes(source);
	}

	@Test
	void testCastList() {
		final var list = List.of(mimeType, otherMimeType);
		assertThat(castList(list)).isEqualTo(list);

		when(mimeType.toString()).thenReturn(DEFAULT_MIME_TYPE);
		assertThat(castList(list)).isEqualTo(List.of(otherMimeType));
	}

}
