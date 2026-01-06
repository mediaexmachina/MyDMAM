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

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Optional;
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
import media.mexm.mydmam.tools.SortOrder;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.transfertfiles.CachedFileAttributes;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles({ "Default" })
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
	@Fake(min = ITEMS_TO_ADD / 10, max = ITEMS_TO_ADD - 10)
	int from;

	@Fake
	SortOrder soName;
	@Fake
	SortOrder soType;
	@Fake
	SortOrder soDate;
	@Fake
	SortOrder soSize;

	String parentPath;
	String parentHashPath;
	FileSort sort;

	@BeforeEach
	@Transactional
	void init() {
		parentPath = "/" + basePath;
		parentHashPath = FileEntity.hashPath(realm, storage, parentPath);
		sort = new FileSort(soName, soType, soDate, soSize);

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
		final var result = fileDao.getByParentHashPath(parentHashPath, from, size, empty());
		assertEquals(size, result.size());
		assertFalse(result.isEmpty());
		assertEquals("/" + basePath + "/" + baseName + from, result.get(0).getPath());
	}

	/**
	 * Lazy check
	 */
	@Test
	void testGetByParentHashPath_sort() {
		final var result = fileDao.getByParentHashPath(parentHashPath, 0, ITEMS_TO_ADD, Optional.ofNullable(sort));
		assertEquals(ITEMS_TO_ADD, result.size());

		final var allPaths = result.stream().map(FileEntity::getPath).toList();
		assertThat(allPaths).contains("/" + basePath + "/" + baseName + from);
	}

	/**
	 * Lazy check
	 */
	@Test
	void testGetByParentHashPath_sort2() {
		final var size = (ITEMS_TO_ADD - from) / 2;
		final var result = fileDao.getByParentHashPath(parentHashPath, from, size, Optional.ofNullable(sort));
		assertEquals(size, result.size());
	}

	@Test
	void testCountParentHashPathItems() {
		assertEquals(ITEMS_TO_ADD, fileDao.countParentHashPathItems(realm, storage, parentHashPath));
		assertEquals(0, fileDao.countParentHashPathItems(realm + "NOPE", storage, parentHashPath));
		assertEquals(0, fileDao.countParentHashPathItems(realm, storage + "NOPE", parentHashPath));
	}

	@Test
	void testGetAllFromRealm() {
		final var collectedFiles = new HashSet<FileEntity>(ITEMS_TO_ADD);
		fileDao.getAllFromRealm(realm, collectedFiles::add);
		assertThat(collectedFiles).size().isEqualTo(ITEMS_TO_ADD);
	}

}
