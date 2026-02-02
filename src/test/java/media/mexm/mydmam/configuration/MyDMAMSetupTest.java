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
package media.mexm.mydmam.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.sqlite.SQLiteConfig;

import eu.medsea.mimeutil.MimeUtil2;
import media.mexm.mydmam.tools.ImageMagick;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class MyDMAMSetupTest {

	@Autowired
	SQLiteConfig sqLiteConfig;
	@Autowired
	MimeUtil2 magicMimeUtil;
	@Autowired
	MimeUtil2 extensionMimeUtil;
	@Autowired
	ExecutableFinder executableFinder;
	@Autowired
	ImageMagick imageMagick;

	@Test
	void testGetSqliteConfig() {
		assertThat(sqLiteConfig.getClass()).isAssignableTo(SQLiteConfig.class);
	}

	@Test
	void testGetMagicMimeMimeDetector() {
		assertThat(magicMimeUtil.getClass()).isAssignableTo(MimeUtil2.class);
		assertThat(magicMimeUtil).isNotEqualTo(extensionMimeUtil);
	}

	@Test
	void testGetExtensionMimeDetector() {
		assertThat(extensionMimeUtil.getClass()).isAssignableTo(MimeUtil2.class);
	}

	@Test
	void testGetExecutableFinder() {
		assertThat(executableFinder.getClass()).isAssignableTo(ExecutableFinder.class);
	}

	@Test
	void testGetImageMagick() {
		assertThat(imageMagick.getClass()).isAssignableTo(ImageMagick.class);
	}

}
