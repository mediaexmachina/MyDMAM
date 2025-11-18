/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2025
 *
 */
package media.mexm.mydmam.asset;

import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.service.MediaAssetService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class MediaAssetTest {

	@Mock
	MediaAssetService service;

	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	String parentPath;
	@Fake
	String basePath;
	@Fake
	String fileExt;

	String path;
	MediaAsset ma;

	@BeforeEach
	void init() {
		path = "/" + parentPath + "/" + basePath + "." + fileExt;
		ma = new MediaAsset(service, realmName, storageName, path);
	}

	@Test
	void testGetHashPath() {
		assertEquals(hashPath(realmName, storageName, path), ma.getHashPath());
	}

	@Test
	void testGetName() {
		assertEquals(basePath + "." + fileExt, ma.getName());
	}

	@Test
	void testToString() {
		assertThat(ma.toString()).contains(realmName, storageName, path);
	}

}
