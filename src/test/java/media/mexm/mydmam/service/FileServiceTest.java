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
package media.mexm.mydmam.service;

import static java.util.Optional.empty;
import static media.mexm.mydmam.indexer.SearchConstraintCondition.MUST_NOT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.dto.StorageCategory;
import media.mexm.mydmam.dto.StorageStateClass;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.indexer.RealmIndexer;
import media.mexm.mydmam.repository.FileRepository;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class FileServiceTest {

	@MockitoBean
	MyDMAMConfigurationProperties configuration;
	@MockitoBean
	Indexer indexer;
	@MockitoBean
	FileRepository fileRepository;

	@Mock
	FileEntity fileEntity;
	@Mock
	FileEntity selectedFileDir;
	@Mock
	FileEntity selectedDir;
	@Mock
	RealmConf realm;

	@Fake
	String hashPath;
	@Fake
	StorageCategory limitCategoryItem;
	@Fake
	StorageStateClass limitStorageClassItem;
	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	String pathSelectedFileDir;
	@Fake
	String pathSelectedDir;

	@Autowired
	FileService fileService;

	Set<StorageCategory> limitCategory;
	Set<StorageStateClass> limitStorageClasses;
	Set<String> hashPaths;
	Set<String> availableStorageNames;
	Set<FileEntity> selectedFilesDirs;
	Set<FileEntity> selectedDirContent;

	@BeforeEach
	void init() {
		limitCategory = Set.of(limitCategoryItem);
		limitStorageClasses = Set.of(limitStorageClassItem);
		hashPaths = Set.of(hashPath);
		availableStorageNames = Set.of(storageName);
		selectedFilesDirs = Set.of(selectedFileDir);
		selectedDirContent = Set.of(selectedDir);

		when(selectedFileDir.getRealm()).thenReturn(realmName);
		when(selectedDir.getRealm()).thenReturn(realmName);
		when(selectedFileDir.getStorage()).thenReturn(storageName);
		when(selectedDir.getStorage()).thenReturn(storageName);
		when(selectedFileDir.getPath()).thenReturn(pathSelectedFileDir);
		when(selectedDir.getPath()).thenReturn(pathSelectedDir);
	}

	@Test
	void testResolveHashPaths_nothing() {
		assertThat(fileService.resolveHashPaths(
				Set.of(),
				limitCategory,
				limitStorageClasses,
				realmName,
				false)).isEmpty();
	}

	@Test
	void testResolveHashPaths_badRealm() {
		when(configuration.getRealmByName(realmName))
				.thenReturn(empty());

		assertThrows(IllegalArgumentException.class,
				() -> fileService.resolveHashPaths(
						hashPaths,
						limitCategory,
						limitStorageClasses,
						realmName,
						false));

		verify(configuration, times(1)).getRealmByName(realmName);
	}

	@Test
	void testResolveHashPaths_noStorages() {
		when(configuration.getRealmByName(realmName))
				.thenReturn(Optional.ofNullable(realm));
		when(realm.getStorageNames(limitCategory, limitStorageClasses))
				.thenReturn(Set.of());

		assertThrows(IllegalStateException.class,
				() -> fileService.resolveHashPaths(
						hashPaths,
						limitCategory,
						limitStorageClasses,
						realmName,
						false));

		verify(configuration, times(1)).getRealmByName(realmName);
		verify(realm, times(1)).getStorageNames(limitCategory, limitStorageClasses);
	}

	@Test
	void testResolveHashPaths_noFiles() {
		when(configuration.getRealmByName(realmName))
				.thenReturn(Optional.ofNullable(realm));
		when(realm.getStorageNames(limitCategory, limitStorageClasses))
				.thenReturn(availableStorageNames);
		when(fileRepository.getByHashPath(hashPaths, realmName, availableStorageNames))
				.thenReturn(Set.of());
		when(fileRepository.getByParentHashPath(hashPaths, realmName, availableStorageNames))
				.thenReturn(Set.of());

		final var result = fileService.resolveHashPaths(
				hashPaths,
				limitCategory,
				limitStorageClasses,
				realmName,
				false);

		assertThat(result).isEmpty();

		verify(configuration, times(1)).getRealmByName(realmName);
		verify(realm, times(1)).getStorageNames(limitCategory, limitStorageClasses);
		verify(fileRepository, times(1)).getByHashPath(hashPaths, realmName, availableStorageNames);
		verify(fileRepository, times(1)).getByParentHashPath(hashPaths, realmName, availableStorageNames);
	}

	@Test
	void testResolveHashPaths() {
		when(configuration.getRealmByName(realmName))
				.thenReturn(Optional.ofNullable(realm));
		when(realm.getStorageNames(limitCategory, limitStorageClasses))
				.thenReturn(availableStorageNames);
		when(fileRepository.getByHashPath(hashPaths, realmName, availableStorageNames))
				.thenReturn(selectedFilesDirs);
		when(fileRepository.getByParentHashPath(hashPaths, realmName, availableStorageNames))
				.thenReturn(selectedDirContent);

		final var result = fileService.resolveHashPaths(
				hashPaths,
				limitCategory,
				limitStorageClasses,
				realmName,
				false);

		assertThat(result).hasSize(2).contains(selectedFileDir, selectedDir);

		verify(configuration, times(1)).getRealmByName(realmName);
		verify(realm, times(1)).getStorageNames(limitCategory, limitStorageClasses);
		verify(fileRepository, times(1)).getByHashPath(hashPaths, realmName, availableStorageNames);
		verify(fileRepository, times(1)).getByParentHashPath(hashPaths, realmName, availableStorageNames);

		verify(selectedFileDir, atLeastOnce()).getRealm();
		verify(selectedDir, atLeastOnce()).getRealm();
		verify(selectedFileDir, atLeastOnce()).getStorage();
		verify(selectedDir, atLeastOnce()).getStorage();
		verify(selectedFileDir, atLeastOnce()).getPath();
		verify(selectedDir, atLeastOnce()).getPath();
	}

	@Nested
	class ResolveHashPathsRecursive {

		@Mock
		RealmIndexer realmIndexer;
		@Mock
		FileEntity fileEntitySubFile;

		@Fake
		String hashPathSelectedFileDir;
		@Fake
		String hashPathSelectedDir;
		@Fake
		String hashPathSubFile;
		@Fake
		String pathSubFile;

		@BeforeEach
		void init() {
			when(configuration.getRealmByName(realmName))
					.thenReturn(Optional.ofNullable(realm));
			when(realm.getStorageNames(limitCategory, limitStorageClasses))
					.thenReturn(availableStorageNames);
			when(fileRepository.getByHashPath(hashPaths, realmName, availableStorageNames))
					.thenReturn(selectedFilesDirs);
			when(fileRepository.getByParentHashPath(hashPaths, realmName, availableStorageNames))
					.thenReturn(selectedDirContent);
			when(indexer.getIndexerByRealm(realmName))
					.thenReturn(Optional.ofNullable(realmIndexer));
		}

		@AfterEach
		void ends() {
			verify(configuration, times(1)).getRealmByName(realmName);
			verify(realm, times(1)).getStorageNames(limitCategory, limitStorageClasses);
			verify(fileRepository, times(1)).getByHashPath(hashPaths, realmName, availableStorageNames);
			verify(fileRepository, times(1)).getByParentHashPath(hashPaths, realmName, availableStorageNames);
			verify(indexer, times(1)).getIndexerByRealm(realmName);
		}

		@Test
		void testNoIndexer() {
			when(indexer.getIndexerByRealm(realmName)).thenReturn(empty());

			assertThrows(IllegalStateException.class,
					() -> fileService.resolveHashPaths(
							hashPaths,
							limitCategory,
							limitStorageClasses,
							realmName,
							true));
		}

		@Test
		void testFullRecursive() {
			when(selectedFileDir.getHashPath()).thenReturn(hashPathSelectedFileDir);
			when(selectedDir.getHashPath()).thenReturn(hashPathSelectedDir);
			when(selectedDir.isDirectory()).thenReturn(true);
			when(realmIndexer.getHashPathsByRecursiveSearch(
					storageName,
					pathSelectedDir,
					MUST_NOT)).thenReturn(Set.of(hashPathSubFile));

			when(fileRepository.getByHashPath(Set.of(hashPathSubFile), realmName))
					.thenReturn(Set.of(fileEntitySubFile));

			when(fileEntitySubFile.getRealm()).thenReturn(realmName);
			when(fileEntitySubFile.getStorage()).thenReturn(storageName);
			when(fileEntitySubFile.getPath()).thenReturn(pathSubFile);

			final var result = fileService.resolveHashPaths(
					hashPaths,
					limitCategory,
					limitStorageClasses,
					realmName,
					true);

			assertThat(result).hasSize(3)
					.contains(selectedFileDir, selectedDir, fileEntitySubFile);

			verify(selectedFileDir, atLeastOnce()).getHashPath();
			verify(selectedDir, atLeastOnce()).getHashPath();
			verify(selectedDir, atLeastOnce()).isDirectory();
			verify(selectedDir, atLeastOnce()).getStorage();
			verify(selectedDir, atLeastOnce()).getPath();
			verify(realmIndexer, times(1))
					.getHashPathsByRecursiveSearch(
							storageName,
							pathSelectedDir,
							MUST_NOT);
			verify(fileRepository, times(1))
					.getByHashPath(Set.of(hashPathSubFile), realmName);

			verify(fileEntitySubFile, atLeastOnce()).getRealm();
			verify(fileEntitySubFile, atLeastOnce()).getStorage();
			verify(fileEntitySubFile, atLeastOnce()).getPath();

			verify(selectedFileDir, atLeastOnce()).getRealm();
			verify(selectedDir, atLeastOnce()).getRealm();
			verify(selectedFileDir, atLeastOnce()).getStorage();
			verify(selectedDir, atLeastOnce()).getStorage();
			verify(selectedFileDir, atLeastOnce()).getPath();
			verify(selectedDir, atLeastOnce()).getPath();
		}

	}
}
