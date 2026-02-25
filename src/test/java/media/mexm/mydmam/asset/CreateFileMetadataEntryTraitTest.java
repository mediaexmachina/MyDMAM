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
package media.mexm.mydmam.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.activity.ActivityHandler;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class CreateFileMetadataEntryTraitTest {

	class Impl implements CreateFileMetadataEntryTrait {

		final CreateFileMetadataEntryTraitTest ref;

		Impl(final CreateFileMetadataEntryTraitTest ref) {
			this.ref = ref;
		}

		@Override
		public void createFileMetadataEntry(final String originHandler,
											final String classifier,
											final int layer,
											final String key,
											final String value) {
			assertEquals(ref.originHandler, originHandler);
			assertEquals(ref.classifier, classifier);
			assertEquals(ref.layer, layer);
			assertEquals(ref.key, key);
			assertEquals(ref.value, value);
			callCount++;
		}

	}

	@Mock
	ActivityHandler hander;
	@Mock
	MetadataExtractorHandler mtdHander;

	@Fake
	String originHandler;
	@Fake
	String classifier;
	@Fake
	int layer;
	@Fake
	String key;
	@Fake
	String value;

	int callCount;

	Impl impl;

	@BeforeEach
	void init() {
		when(hander.getHandlerName()).thenReturn(originHandler);
		when(mtdHander.getMetadataOriginName()).thenReturn(originHandler);
		callCount = 0;
		impl = new Impl(this);
	}

	@Test
	void testCreateFileMetadataEntryActivityHandlerStringIntStringString() {
		impl.createFileMetadataEntry(hander, classifier, layer, key, value);
		verify(hander, times(1)).getHandlerName();
		assertThat(callCount).isEqualTo(1);
	}

	@Test
	void testCreateFileMetadataEntryMetadataExtractorHandlerStringIntStringString() {
		impl.createFileMetadataEntry(mtdHander, classifier, layer, key, value);
		verify(mtdHander, times(1)).getMetadataOriginName();
		assertThat(callCount).isEqualTo(1);
	}

	@Test
	void testCreateFileMetadataEntryStringStringIntMapOfStringString() {
		impl.createFileMetadataEntry(originHandler, classifier, layer, Map.of(key, value));
		assertThat(callCount).isEqualTo(1);
	}

	@Test
	void testCreateFileMetadataEntryActivityHandlerStringIntMapOfStringString() {
		impl.createFileMetadataEntry(hander, classifier, layer, Map.of(key, value));

		verify(hander, times(1)).getHandlerName();
		assertThat(callCount).isEqualTo(1);
	}

}
