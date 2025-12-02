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
package media.mexm.mydmam.indexer;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static media.mexm.mydmam.indexer.RealmIndexer.normalizeSearchString;
import static media.mexm.mydmam.indexer.SearchConstraintCondition.IGNORE;
import static media.mexm.mydmam.indexer.SearchConstraintRange.NO_RANGE;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.CachedFileAttributes;

@ExtendWith(MockToolsExtendsJunit.class)
class RealmIndexerTest {
	static final Faker faker = net.datafaker.Faker.instance();

	@Fake
	String realmName;
	@Fake
	String storageName;
	@Fake
	boolean computeExplainOnResults;
	@Fake
	boolean directory;
	@Fake
	boolean hidden;
	@Fake
	boolean link;
	@Fake
	boolean special;
	@Fake
	long lastModified;
	@Fake
	long length;
	@Fake
	boolean exists;

	String fileName;
	String path;
	String parentPath;
	String fileHashPath;

	@Mock
	CachedFileAttributes file;

	File workingDir;
	RealmIndexer ri;

	@BeforeEach
	void init() throws Exception {
		fileName = faker.numerify("baseName#####ok42") + "." + faker.numerify("extention#####");
		parentPath = "/" + faker.numerify("root#####") + "/" + faker.numerify("parent#####");
		path = parentPath + "/" + fileName;

		when(file.getName()).thenReturn(fileName);
		when(file.getParentPath()).thenReturn(parentPath);
		when(file.getPath()).thenReturn(path);
		when(file.isDirectory()).thenReturn(directory);
		when(file.isHidden()).thenReturn(hidden);
		when(file.isLink()).thenReturn(link);
		when(file.isSpecial()).thenReturn(special);
		when(file.lastModified()).thenReturn(lastModified);
		when(file.length()).thenReturn(length);
		when(file.exists()).thenReturn(exists);

		fileHashPath = hashPath(realmName, storageName, path);

		workingDir = new File(getTempDirectory(), "mydmam_" + realmName + "_test-indexer");
		forceMkdir(workingDir);

		ri = new RealmIndexer(realmName, workingDir, true); // TODO use computeExplainOnResults
	}

	@AfterEach
	void ends() {
		ri.close();
		deleteQuietly(workingDir);
	}

	@Test
	void testNormalizeSearchString() {
		final var result = normalizeSearchString(" THIS IS_A_$tést\\WITH * NUM8ERS. ");
		assertEquals(8, result.size());
		assertEquals("this is a test with num 8 ers", result.stream().collect(Collectors.joining(" ")));
	}

	CachedFileAttributes makeFalseFile() {
		final var fFile = Mockito.mock(CachedFileAttributes.class);

		final var fFileName = faker.numerify("cantusethis#") + "." + faker.numerify("cantusethis#");
		final var fParentPath = "/" + faker.numerify("cantusethis#") + "/" + faker.numerify("cantusethis#");
		final var fPath = fParentPath + "/" + fFileName;

		when(fFile.getName()).thenReturn(fFileName);
		when(fFile.getParentPath()).thenReturn(fParentPath);
		when(fFile.getPath()).thenReturn(fPath);

		when(fFile.isDirectory()).thenReturn(directory);
		when(fFile.isHidden()).thenReturn(hidden);
		when(fFile.isLink()).thenReturn(link);
		when(fFile.isSpecial()).thenReturn(special);
		when(fFile.lastModified()).thenReturn(lastModified);
		when(fFile.length()).thenReturn(length);
		when(fFile.exists()).thenReturn(exists);

		return fFile;
	}

	@Test
	void testOpenSearch() {
		final var scanResult = new WatchedFiles(Set.of(makeFalseFile(), file, makeFalseFile()), Set.of(), Set.of(), 0);
		ri.update(scanResult, storageName);

		var results = ri.openSearch(getBaseName(fileName), empty(), 10);
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		final var result0 = results.get(0);
		assertThat(result0.hashPath()).isEqualTo(fileHashPath);
		assertThat(result0.name()).isEqualTo(fileName);
		assertThat(result0.score()).isGreaterThan(0f);
		assertThat(result0.storage()).isEqualTo(storageName);
		assertThat(result0.explain()).isNotEmpty();

		results = ri.openSearch(fileName, empty(), 10);
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		results = ri.openSearch("impossible to found this!", empty(), 10);
		assertThat(results).isEmpty();

		results = ri.openSearch("basename", empty(), 10);
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		results = ri.openSearch("basenamf", empty(), 10);
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		results = ri.openSearch("basenamé", empty(), 10);
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		results = ri.openSearch("42 basename ok", empty(), 10);
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		verify(file, atLeastOnce()).getPath();
		clearInvocations(file);
	}

	FileSearchConstraints constraint;

	@Test
	void testOpenSearch_constraint_storage() {
		final var watchedFiles = new WatchedFiles(Set.of(file, makeFalseFile()), Set.of(), Set.of(), 0);

		when(file.getName()).thenReturn("fromstorage1");
		ri.update(watchedFiles, "storage1");

		when(file.getName()).thenReturn("fromstorage2");
		ri.update(watchedFiles, "storage2");

		var results = ri.openSearch("fromsto", empty(), 10);

		assertThat(results).size().isEqualTo(2);
		assertThat(results.stream().map(FileSearchResult::name).collect(toUnmodifiableSet()))
				.contains("fromstorage1", "fromstorage2");
		assertThat(results.stream().map(FileSearchResult::storage).collect(toUnmodifiableSet()))
				.contains("storage1", "storage2");

		for (var pos = 1; pos < 3; pos++) {
			constraint = new FileSearchConstraints(
					IGNORE, IGNORE, IGNORE, IGNORE, NO_RANGE, NO_RANGE, List.of("storage" + pos), null, null);

			results = ri.openSearch("fromsto", Optional.ofNullable(constraint), 10);
			assertThat(results).size().isEqualTo(1);
			assertThat(results.get(0).name()).isEqualTo("fromstorage" + pos);
		}

		verify(file, atLeastOnce()).getPath();
		clearInvocations(file);
	}

	@Test
	void testFile_add_delete_update() {
		final var scanResultAdd = new WatchedFiles(Set.of(file, makeFalseFile()), Set.of(), Set.of(), 0);
		ri.update(scanResultAdd, storageName);

		var results = ri.openSearch(getBaseName(fileName), empty(), 10);
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath()).isEqualTo(fileHashPath);

		final var scanResultLosted = new WatchedFiles(Set.of(), Set.of(file), Set.of(), 0);
		ri.update(scanResultLosted, storageName);
		results = ri.openSearch(getBaseName(fileName), empty(), 10);
		assertThat(results).isEmpty();

		ri.update(scanResultAdd, storageName);
		results = ri.openSearch(getBaseName(fileName), empty(), 10);
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath()).isEqualTo(fileHashPath);

		final var scanResultUpdated = new WatchedFiles(Set.of(), Set.of(), Set.of(file), 0);
		ri.update(scanResultUpdated, storageName);
		results = ri.openSearch(getBaseName(fileName), empty(), 10);
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath()).isEqualTo(fileHashPath);

		verify(file, atLeastOnce()).getPath();
		clearInvocations(file);
	}

	// TODO test constrains

}
