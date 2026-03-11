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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static media.mexm.mydmam.asset.FileMetadataResolutionTrait.MTD_TECHNICAL_CLASSIFIER;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.tools.ImageMagick;
import media.mexm.mydmam.tools.JsonPathHelper;
import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class ImageInfoExtractionActivityTest {

	@MockitoBean
	ImageMagick imageMagick;
	@MockitoBean
	MimeTypeDetector mimeTypeDetector;

	@Mock
	MediaAsset asset;
	@Mock
	ActivityEventType eventType;
	@Mock
	RealmStorageConfiguredEnv storedOn;

	@Autowired
	ImageInfoExtractionActivity iiea;

	@AfterEach
	void ends() {
		verifyNoMoreInteractions(imageMagick, mimeTypeDetector);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testIsEnabled(final boolean enabled) {
		when(imageMagick.isEnabled()).thenReturn(enabled);
		assertEquals(enabled, iiea.isEnabled());
		verify(imageMagick, times(1)).isEnabled();
	}

	@Test
	void testGetManagedMimeTypes() {
		assertThat(iiea.getManagedMimeTypes())
				.hasSizeGreaterThan(10)
				.contains("image/jpeg", "image/png");
	}

	@Test
	void testGetProducedPreviewTypes() {
		assertThat(iiea.getProducedPreviewTypes()).containsOnly("image-format");
	}

	@Test
	void testGetMetadataOriginName() {
		assertThat(iiea.getMetadataOriginName()).isEqualTo("imagemagick");
	}

	@Test
	void testCanHandle() {
		when(storedOn.isDAS()).thenReturn(false);
		when(storedOn.haveWorkingDir()).thenReturn(false);
		when(storedOn.haveRenderedDir()).thenReturn(false);
		when(asset.getMimeType()).thenReturn(Optional.ofNullable("nope/nope"));

		assertFalse(iiea.canHandle(asset, eventType, storedOn));

		when(storedOn.isDAS()).thenReturn(true);
		assertFalse(iiea.canHandle(asset, eventType, storedOn));

		when(storedOn.haveWorkingDir()).thenReturn(true);
		assertFalse(iiea.canHandle(asset, eventType, storedOn));

		when(storedOn.haveRenderedDir()).thenReturn(true);
		assertFalse(iiea.canHandle(asset, eventType, storedOn));

		when(asset.getMimeType()).thenReturn(Optional.ofNullable("image/jpeg"));
		assertTrue(iiea.canHandle(asset, eventType, storedOn));

		verify(storedOn, atLeastOnce()).isDAS();
		verify(storedOn, atLeastOnce()).haveWorkingDir();
		verify(storedOn, atLeastOnce()).haveRenderedDir();
		verify(asset, atLeastOnce()).getMimeType();
	}

	@Nested
	class Handle {

		@Mock
		PathIndexingStorage storage;
		@Mock
		RealmConf realm;
		@Mock
		FileEntity file;
		@Mock
		JsonPathHelper jsonNode;

		@Fake(min = 1, max = 10000)
		int fileId;
		@Fake
		String mimeType;

		@Fake
		String imageMimeType;
		@Fake(min = 1, max = 100)
		int width;
		@Fake(min = 1, max = 100)
		int height;
		@Fake
		String colorspace;
		@Fake
		String orientation;
		@Fake
		String imageType;

		File assetFile;
		File workingFile;

		@BeforeEach
		void init() throws IOException {
			assetFile = createTempFile("mydmam-" + getClass().getSimpleName(), "assetFile");
			workingFile = createTempFile("mydmam-" + getClass().getSimpleName(), "workingFile");
			writeStringToFile(workingFile, Faker.instance().lorem().paragraph(5), UTF_8);

			when(storedOn.storage()).thenReturn(storage);
			when(storedOn.realm()).thenReturn(realm);
			when(asset.getLocalInternalFile(storage)).thenReturn(assetFile);
			when(realm.makeWorkingFile(any(), any())).thenReturn(workingFile);
			when(asset.getFile()).thenReturn(file);
			when(file.getId()).thenReturn(fileId);
			when(imageMagick.extractIdentifyJsonFile(assetFile, workingFile))
					.thenReturn(jsonNode);
			when(mimeTypeDetector.getMimeType(workingFile)).thenReturn(mimeType);

			when(jsonNode.read("$.version", String.class))
					.thenReturn(Optional.ofNullable("1.0"));
			when(jsonNode.read("$.image.mimeType", String.class))
					.thenReturn(Optional.ofNullable(imageMimeType));
			when(jsonNode.read("$.image.geometry.width", Integer.class))
					.thenReturn(Optional.ofNullable(width));
			when(jsonNode.read("$.image.geometry.height", Integer.class))
					.thenReturn(Optional.ofNullable(height));
			when(jsonNode.read("$.image.colorspace", String.class))
					.thenReturn(Optional.ofNullable(colorspace));
			when(jsonNode.read("$.image.orientation", String.class))
					.thenReturn(Optional.ofNullable(orientation));
			when(jsonNode.read("$.image.type", String.class))
					.thenReturn(Optional.ofNullable(imageType));
		}

		@AfterEach
		void ends() {
			deleteQuietly(assetFile);
			deleteQuietly(workingFile);
		}

		@Test
		void testHandle() throws Exception {
			iiea.handle(asset, eventType, storedOn);

			verify(storedOn, atLeastOnce()).storage();
			verify(storedOn, atLeastOnce()).realm();
			verify(asset, atLeastOnce()).getLocalInternalFile(storage);
			verify(realm, atLeastOnce()).makeWorkingFile(eq(fileId + "-identify.json"), any());
			verify(asset, atLeastOnce()).getFile();
			verify(file, atLeastOnce()).getId();
			verify(imageMagick, times(1)).extractIdentifyJsonFile(assetFile, workingFile);

			verify(asset, times(1))
					.declareRenderedStaticFile(workingFile, "identify.json", true, mimeTypeDetector, 0, "image-format");

			verify(jsonNode, atLeastOnce()).read(anyString(), eq(String.class));
			verify(jsonNode, atLeastOnce()).read(anyString(), eq(Integer.class));

			verify(asset, times(1)).setMimeType(iiea, imageMimeType);
			verify(asset, times(1))
					.createFileMetadataEntry(iiea, MTD_TECHNICAL_CLASSIFIER, 0, Map.of(
							"colorspace", colorspace,
							"orientation", orientation,
							"type", imageType.toLowerCase()));
			verify(asset, times(1)).setResolution(iiea, width, height);
		}

		@Test
		void testHandle_badVersion() throws Exception {
			when(jsonNode.read("$.version", String.class)).thenReturn(empty());

			assertThrows(IllegalArgumentException.class,
					() -> iiea.handle(asset, eventType, storedOn));

			verify(storedOn, atLeastOnce()).storage();
			verify(storedOn, atLeastOnce()).realm();
			verify(asset, atLeastOnce()).getLocalInternalFile(storage);
			verify(realm, atLeastOnce()).makeWorkingFile(eq(fileId + "-identify.json"), any());
			verify(asset, atLeastOnce()).getFile();
			verify(file, atLeastOnce()).getId();
			verify(imageMagick, times(1)).extractIdentifyJsonFile(assetFile, workingFile);

			assertThat(workingFile).exists();

			verify(jsonNode, atLeastOnce()).read(anyString(), eq(String.class));
		}

	}
}
