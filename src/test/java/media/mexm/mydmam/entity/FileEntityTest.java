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
 * Copyright (C) Media ex Machina 2025
 *
 */
package media.mexm.mydmam.entity;

import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.transfertfiles.CachedFileAttributes;

/**
 * Poor tests, need real e2e watchfolder tests
 */
@ExtendWith(MockToolsExtendsJunit.class)
class FileEntityTest {

	@Fake
	String basePath;
	@Fake
	String baseName;
	@Fake
	String realm;
	@Fake
	String storage;
	@Fake(min = 1, max = 100)
	long length;
	@Fake(min = 10000, max = 1000000)
	long modified;

	@Mock
	CachedFileAttributes firstDetectionFile;

	FileEntity file;
	String parentPath;
	String path;

	@BeforeEach
	void init() {
		path = "/" + basePath + "/" + baseName;
		parentPath = "/" + basePath;

		when(firstDetectionFile.getPath()).thenReturn(path);
		when(firstDetectionFile.getParentPath()).thenReturn(parentPath);
		when(firstDetectionFile.lastModified()).thenReturn(modified);
		when(firstDetectionFile.length()).thenReturn(length);
		when(firstDetectionFile.isDirectory()).thenReturn(false);

		file = new FileEntity();
		file = new FileEntity(realm, storage, firstDetectionFile);
	}

	@AfterEach
	void ends() {
		reset(firstDetectionFile);
	}

	@Test
	void testHashPath() {
		assertEquals("0b9d90f1f0e4cb576cecc17e0c722939ed425f5ea792dc3404ef72c54ad2f9bc",
				hashPath("realm", "storage", "basePath"));
		assertThrows(IllegalArgumentException.class, () -> hashPath("re:alm", "storage", "basePath"));
		assertThrows(IllegalArgumentException.class, () -> hashPath("realm", "sto:rage", "basePath"));
		assertThrows(IllegalArgumentException.class, () -> hashPath("realm", "storage", "bas:ePath"));
	}

	@Test
	void testUpdate() {
		assertEquals(file, file.update(firstDetectionFile));
	}

	@Test
	void testIsTimeQualified() {
		when(firstDetectionFile.isDirectory()).thenReturn(true);
		file = new FileEntity(realm, storage, firstDetectionFile);
		assertTrue(file.isTimeQualified(Duration.ZERO));
	}

	@Test
	void testResetDoneButChanged() {
		assertEquals(file, file.resetDoneButChanged());
	}

	@Test
	void testSetMarkedAsDone() {
		assertEquals(file, file.setMarkedAsDone());
	}

	@Test
	void testToFileAttributesReference() {
		assertNotNull(file.toFileAttributesReference(true));
	}

}
