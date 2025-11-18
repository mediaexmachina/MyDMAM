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
package media.mexm.mydmam.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;
import media.mexm.mydmam.entity.FileEntity;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.transfertfiles.CachedFileAttributes;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles({ "FlatJobKit" })
@ExtendWith(MockToolsExtendsJunit.class)
class FileDaoImplTest {

	static final int ITEMS_TO_ADD = 50;

	@Autowired
	FileDao fileDao;
	@Autowired
	FileRepository fileRepository;

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
	@Fake(min = ITEMS_TO_ADD / 10, max = ITEMS_TO_ADD - 1)
	int from;

	String parentPath;
	String parentHashPath;

	@BeforeEach
	@Transactional
	void init() {
		parentPath = "/" + basePath;
		parentHashPath = FileEntity.hashPath(realm, storage, parentPath);

		fileRepository.deleteAll();

		fileRepository.saveAllAndFlush(IntStream.range(0, ITEMS_TO_ADD)
				.mapToObj(i -> {
					final var path = "/" + basePath + "/" + baseName + i;

					when(firstDetectionFile.getPath()).thenReturn(path);
					when(firstDetectionFile.getParentPath()).thenReturn(parentPath);
					when(firstDetectionFile.lastModified()).thenReturn(modified);
					when(firstDetectionFile.length()).thenReturn(length);
					when(firstDetectionFile.isDirectory()).thenReturn(false);
					return new FileEntity(realm, storage, firstDetectionFile);
				}).toList());
		reset(firstDetectionFile);
	}

	@Test
	void testGetByParentHashPath() {
		final var size = (ITEMS_TO_ADD - from) / 2;
		final var result = fileDao.getByParentHashPath(parentHashPath, from, size);
		assertEquals(size, result.size());
		assertFalse(result.isEmpty());
		assertEquals("/" + basePath + "/" + baseName + from, result.get(0).getPath());
	}

	@Test
	void testCountParentHashPathItems() {
		assertEquals(ITEMS_TO_ADD, fileDao.countParentHashPathItems(realm, storage, parentHashPath));
		assertEquals(0, fileDao.countParentHashPathItems(realm + "NOPE", storage, parentHashPath));
		assertEquals(0, fileDao.countParentHashPathItems(realm, storage + "NOPE", parentHashPath));
	}

}
