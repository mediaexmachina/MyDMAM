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
package media.mexm.mydmam.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import jakarta.transaction.Transactional;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.FileRepository;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;
import tv.hd3g.jobkit.watchfolder.WatchedFileScanner;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.CachedFileAttributes;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "FlatJobKit" })
class PathIndexerServiceWFLogicTest {

	@Mock
	CachedFileAttributes file;
	@Mock
	CachedFileAttributes anotherFile;
	@Mock
	ObservedFolder observedFolder;
	@Mock
	WatchedFileScanner watchedFileScanner;
	@Mock
	AbstractFileSystemURL fileSystem;
	@Value("${watchfolder.maxDeep:100}")
	int maxDeep;

	@Fake
	String realm;
	@Fake
	String storage;
	@Fake
	String label;
	@Fake
	String path;
	@Fake
	String anotherPath;
	@Fake(min = 0, max = 1000000)
	long length;
	@Fake(min = 0, max = 1000000)
	long anotherLength;
	@Fake
	boolean directory;
	@Fake
	boolean anotherDirectory;

	@Autowired
	FileRepository repository;
	@Autowired
	PathIndexerService pis;

	long lastModified;
	FileEntity entity;
	WatchedFiles watchedFilesResult;

	@BeforeEach
	@Transactional
	void init() {
		repository.deleteAll();
		path = "/" + path;
		anotherPath = "/" + anotherPath;
		lastModified = System.currentTimeMillis();

		when(observedFolder.getLabel()).thenReturn(label);
		when(file.getPath()).thenReturn(path);
		when(file.isDirectory()).thenReturn(directory);
		when(file.length()).thenReturn(length);
		when(file.lastModified()).thenReturn(lastModified);
		when(file.getParentPath()).thenReturn("/parent/" + path);

		when(anotherFile.getPath()).thenReturn(anotherPath);
		when(anotherFile.isDirectory()).thenReturn(anotherDirectory);
		when(anotherFile.length()).thenReturn(anotherLength);
		when(anotherFile.lastModified()).thenReturn(lastModified - 1);
		when(anotherFile.getParentPath()).thenReturn("/parent/" + anotherPath);

		entity = new FileEntity(realm, storage, file);
	}

	@AfterEach
	void end() {
		verify(observedFolder, atLeast(0)).getLabel();
		verify(file, atLeastOnce()).getPath();
		verify(file, atLeastOnce()).isDirectory();
		verify(file, atLeastOnce()).length();
		verify(file, atLeastOnce()).lastModified();
		verify(file, atLeastOnce()).getParentPath();
	}

	private int prepareAnotherEntityOutsideTest() {
		final var faker = net.datafaker.Faker.instance();
		final var file2 = Mockito.mock(CachedFileAttributes.class);

		when(file2.isDirectory()).thenReturn(false);
		when(observedFolder.getLabel()).thenReturn(faker.numerify("label###"));
		when(file2.getPath()).thenReturn(faker.numerify("/path###"));
		when(file2.getParentPath()).thenReturn(faker.numerify("/parent/path###"));
		when(file2.isDirectory()).thenReturn(faker.random().nextBoolean());
		when(file2.length()).thenReturn(Math.abs(faker.random().nextLong()));
		when(file2.lastModified()).thenReturn(System.currentTimeMillis());

		final var anotherEntity = new FileEntity(faker.numerify("realm###"), faker.numerify("storage###"), file2);
		final var beforeAdd = (int) repository.count();
		repository.saveAndFlush(anotherEntity);
		assertEquals(beforeAdd + 1, repository.count());
		return beforeAdd + 1;
	}

	@Test
	void testAddDelete() { // NOSONAR S5961
		final var repositorySize = prepareUpdate();

		/**
		 * First, add
		 */
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.updated().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());
		assertEquals(repositorySize + 1, repository.count());

		/**
		 * Second, update
		 */
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertEquals(1, watchedFilesResult.founded().size());
		assertEquals(file, watchedFilesResult.founded().stream().findFirst().get());
		assertTrue(watchedFilesResult.updated().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());
		assertEquals(repositorySize + 1, repository.count());

		/**
		 * Third, nothing new
		 */
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertTrue(watchedFilesResult.updated().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());
		assertEquals(repositorySize + 1, repository.count());

		/**
		 * Fourth, remove deleted
		 */
		when(watchedFileScanner.scan(fileSystem)).thenReturn(List.of());
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertThat(watchedFilesResult.losted()).size().isEqualTo(1);
		assertTrue(watchedFilesResult.updated().isEmpty());
		assertEquals(path, watchedFilesResult.losted().iterator().next().getPath());

		assertEquals(0, watchedFilesResult.totalFiles());
		assertEquals(repositorySize, repository.count());

		verify(observedFolder, atLeastOnce()).getMinFixedStateTime();
		verify(watchedFileScanner, times(4)).scan(fileSystem);
	}

	@Test
	void testUpdate_size() {
		prepareUpdate();
		when(file.isDirectory()).thenReturn(false);

		/**
		 * First + Second, add and "confirm"
		 */
		pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);

		/**
		 * Third
		 */
		when(file.length()).thenReturn(length + 1l);
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertTrue(watchedFilesResult.updated().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());

		/**
		 * Fourth, confirm
		 */
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());

		assertThat(watchedFilesResult.updated()).size().isEqualTo(1);
		assertEquals(path, watchedFilesResult.updated().iterator().next().getPath());

		verify(observedFolder, atLeastOnce()).getMinFixedStateTime();
		verify(watchedFileScanner, times(4)).scan(fileSystem);
	}

	@Test
	void testUpdate_date() {
		prepareUpdate();
		when(file.isDirectory()).thenReturn(false);

		/**
		 * First + Second, add and "confirm"
		 */
		pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);

		/**
		 * Third
		 */
		when(file.lastModified()).thenReturn(lastModified + 2000l);
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertTrue(watchedFilesResult.updated().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());

		/**
		 * Fourth, confirm
		 */
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());

		assertThat(watchedFilesResult.updated()).size().isEqualTo(1);
		assertEquals(path, watchedFilesResult.updated().iterator().next().getPath());

		verify(observedFolder, atLeastOnce()).getMinFixedStateTime();
		verify(watchedFileScanner, times(4)).scan(fileSystem);
	}

	@Test
	void testAdd_growing() {
		prepareUpdate();
		when(file.isDirectory()).thenReturn(false);

		/**
		 * First, add
		 */
		pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);

		/**
		 * Second, growing
		 */
		when(file.length()).thenReturn(length + 1l);
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertTrue(watchedFilesResult.updated().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());

		/**
		 * Third, growing
		 */
		when(file.length()).thenReturn(length + 2l);
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertTrue(watchedFilesResult.updated().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());

		/**
		 * Fourth, confirm
		 */
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.updated().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());

		assertThat(watchedFilesResult.founded()).size().isEqualTo(1);
		assertEquals(path, watchedFilesResult.founded().iterator().next().getPath());

		verify(observedFolder, atLeastOnce()).getMinFixedStateTime();
		verify(watchedFileScanner, times(4)).scan(fileSystem);
	}

	@Test
	void testUpdate_growing() {
		prepareUpdate();
		when(file.isDirectory()).thenReturn(false);

		/**
		 * First + second, add + confirm
		 */
		pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);

		/**
		 * Second, growing
		 */
		when(file.length()).thenReturn(length + 1l);
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertTrue(watchedFilesResult.updated().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());

		/**
		 * Third, growing again
		 */
		when(file.length()).thenReturn(length + 2l);
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertTrue(watchedFilesResult.updated().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());

		/**
		 * Fourth, confirm
		 */
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertTrue(watchedFilesResult.founded().isEmpty());
		assertTrue(watchedFilesResult.losted().isEmpty());
		assertEquals(1, watchedFilesResult.totalFiles());

		assertThat(watchedFilesResult.updated()).size().isEqualTo(1);
		assertEquals(path, watchedFilesResult.updated().iterator().next().getPath());

		verify(observedFolder, atLeastOnce()).getMinFixedStateTime();
		verify(watchedFileScanner, times(5)).scan(fileSystem);
	}

	@Test
	void testLost() {
		prepareUpdate();

		/**
		 * First, add
		 */
		pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);

		/**
		 * Second, remove added
		 */
		when(watchedFileScanner.scan(fileSystem)).thenReturn(List.of());
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertThat(watchedFilesResult.losted()).isEmpty();
		assertThat(watchedFilesResult.founded()).isEmpty();
		assertThat(watchedFilesResult.updated()).isEmpty();
		assertEquals(0, watchedFilesResult.totalFiles());

		verify(observedFolder, atLeastOnce()).getMinFixedStateTime();
		verify(watchedFileScanner, times(2)).scan(fileSystem);
	}

	@Test
	void testLost_nonEmptyDir() {
		prepareUpdate();
		when(watchedFileScanner.scan(fileSystem)).thenReturn(List.of(file, anotherFile));

		/**
		 * First, add
		 */
		pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);

		/**
		 * Second, remove added
		 */
		when(watchedFileScanner.scan(fileSystem)).thenReturn(List.of(anotherFile));
		watchedFilesResult = pis.updateFoundedFiles(watchedFileScanner, realm, storage, observedFolder, fileSystem);
		assertThat(watchedFilesResult.losted()).isEmpty();
		assertThat(watchedFilesResult.updated()).isEmpty();
		assertThat(watchedFilesResult.founded()).size().isEqualTo(1);
		assertEquals(anotherFile, watchedFilesResult.founded().stream().findFirst().get());

		verify(observedFolder, atLeastOnce()).getMinFixedStateTime();
		verify(watchedFileScanner, times(2)).scan(fileSystem);
		verify(anotherFile, atLeastOnce()).getPath();
		verify(anotherFile, atLeastOnce()).isDirectory();
		verify(anotherFile, atLeastOnce()).length();
		verify(anotherFile, atLeastOnce()).lastModified();
		verify(anotherFile, atLeastOnce()).getParentPath();
	}

	@Test
	void testReset() {
		pis.resetFoundedFiles(realm, storage, observedFolder, Set.of(file));
		assertEquals(0, repository.count());

		repository.saveAndFlush(entity);
		pis.resetFoundedFiles(realm, storage, observedFolder, Set.of(file));
		assertEquals(0, repository.count());
	}

	@Test
	void testReset_noItems() {
		final Set<CachedFileAttributes> emptySet = Set.of();
		assertThrows(IllegalArgumentException.class,
				() -> pis.resetFoundedFiles(realm, storage, observedFolder, emptySet));
	}

	private int prepareUpdate() {
		final var repositorySize = prepareAnotherEntityOutsideTest();
		when(file.isDirectory()).thenReturn(directory);
		when(anotherFile.isDirectory()).thenReturn(directory);
		when(watchedFileScanner.scan(fileSystem)).thenReturn(List.of(file));
		when(observedFolder.getMinFixedStateTime()).thenReturn(Duration.ofMillis(-1000));
		return repositorySize;
	}
}
