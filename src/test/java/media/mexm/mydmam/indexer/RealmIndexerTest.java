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
import static media.mexm.mydmam.indexer.SearchConstraintCondition.MUST;
import static media.mexm.mydmam.indexer.SearchConstraintCondition.MUST_NOT;
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
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;

import media.mexm.mydmam.entity.FileEntity;
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
	long length;
	@Fake
	boolean exists;

	String fileName;
	String path;
	String parentPath;
	String fileHashPath;
	long lastModified;

	@Mock
	CachedFileAttributes file;

	File workingDir;
	RealmIndexer ri;

	FileSearchConstraints constraint;
	SearchConstraintCondition directoryConstraint;
	SearchConstraintCondition hiddenConstraint;
	SearchConstraintCondition linkConstraint;
	SearchConstraintCondition specialConstraint;
	SearchConstraintRange constraintDateRange;
	SearchConstraintRange constraintSizeRange;

	@BeforeEach
	void init() throws Exception {
		fileName = faker.numerify("baseName#####ok42") + "." + faker.numerify("extention#####");
		parentPath = "/" + faker.numerify("root#####") + "/" + faker.numerify("parent#####");
		path = parentPath + "/" + fileName;
		lastModified = System.currentTimeMillis();

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

		ri = new RealmIndexer(realmName, workingDir, false);
	}

	@AfterEach
	void ends() {
		ri.close();
		deleteQuietly(workingDir);
	}

	@Test
	void testComputeExplainOnResults() throws IOException {// NOSONAR S5961
		ri.close();
		ri = new RealmIndexer(realmName, workingDir, true);
		ri.update(new WatchedFiles(Set.of(file), Set.of(), Set.of(), 0), storageName);

		assertThat(ri.openSearch(getBaseName(fileName), empty(), 10)
				.foundedFiles().stream().findFirst().map(FileSearchResult::explain)
				.orElse(null)).isNotEmpty();

		verify(file, atLeastOnce()).getPath();
		clearInvocations(file);
	}

	@Test
	void testNormalizeSearchString() {
		final var result = normalizeSearchString(" THIS IS_A_$tést\\WITH * NUM8ERS. ");
		assertEquals(8, result.size());
		assertEquals("this is a test with num 8 ers", result.stream().collect(Collectors.joining(" ")));

		assertThat(normalizeSearchString(null)).isEmpty();
		assertThat(normalizeSearchString(" \t")).isEmpty();
		assertThat(normalizeSearchString("_")).isEmpty();
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
	void testOpenSearch() {// NOSONAR S5961
		final var scanResult = new WatchedFiles(Set.of(makeFalseFile(), file, makeFalseFile()), Set.of(), Set.of(), 0);
		ri.update(scanResult, storageName);

		var results = ri.openSearch(getBaseName(fileName), empty(), 10).foundedFiles();
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		final var result0 = results.get(0);
		assertThat(result0.hashPath()).isEqualTo(fileHashPath);
		assertThat(result0.name()).isEqualTo(fileName);
		assertThat(result0.score()).isGreaterThan(0f);
		assertThat(result0.storage()).isEqualTo(storageName);
		assertThat(result0.explain()).isNull();

		results = ri.openSearch(fileName, empty(), 10).foundedFiles();
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		results = ri.openSearch("impossible to found this!", empty(), 10).foundedFiles();
		assertThat(results).isEmpty();

		results = ri.openSearch("basename", empty(), 10).foundedFiles();
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		results = ri.openSearch("basenamf", empty(), 10).foundedFiles();
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		results = ri.openSearch("basenamé", empty(), 10).foundedFiles();
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		results = ri.openSearch("42 basename ok", empty(), 10).foundedFiles();
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		results = ri.openSearch("bas?nam*", empty(), 10).foundedFiles();
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		verify(file, atLeastOnce()).getPath();
		clearInvocations(file);
	}

	@Test
	void testOpenSearch_constraint_storage() {
		final var watchedFiles = new WatchedFiles(Set.of(file, makeFalseFile()), Set.of(), Set.of(), 0);

		when(file.getName()).thenReturn("fromstorage1");
		ri.update(watchedFiles, "storage1");

		when(file.getName()).thenReturn("fromstorage2");
		ri.update(watchedFiles, "storage2");

		var results = ri.openSearch("fromsto", empty(), 10).foundedFiles();

		assertThat(results).size().isEqualTo(2);
		assertThat(results.stream().map(FileSearchResult::name).collect(toUnmodifiableSet()))
				.contains("fromstorage1", "fromstorage2");
		assertThat(results.stream().map(FileSearchResult::storage).collect(toUnmodifiableSet()))
				.contains("storage1", "storage2");

		for (var pos = 1; pos < 3; pos++) {
			constraint = new FileSearchConstraints(
					IGNORE, IGNORE, IGNORE, IGNORE, NO_RANGE, NO_RANGE, List.of("storage" + pos), null, null);

			results = ri.openSearch("fromsto", Optional.ofNullable(constraint), 10).foundedFiles();
			assertThat(results).size().isEqualTo(1);
			assertThat(results.get(0).name()).isEqualTo("fromstorage" + pos);
		}

		verify(file, atLeastOnce()).getPath();
		clearInvocations(file);
	}

	private static Stream<Arguments> fourBooleansArgumentsParameters() {
		return IntStream.range(0, 16)
				.mapToObj(i -> Arguments.of(
						i % 2 == 0,
						(i >> 1) % 2 == 0,
						(i >> 2) % 2 == 0,
						(i >> 3) % 2 == 0));
	}

	@ParameterizedTest
	@MethodSource("fourBooleansArgumentsParameters")
	void testOpenSearch_boolean_constraints(final boolean directory,
											final boolean hidden,
											final boolean link,
											final boolean special) {
		final var watchedFiles = new WatchedFiles(Set.of(file, makeFalseFile()), Set.of(), Set.of(), 0);
		when(file.isDirectory()).thenReturn(directory);
		when(file.isHidden()).thenReturn(hidden);
		when(file.isLink()).thenReturn(link);
		when(file.isSpecial()).thenReturn(special);
		ri.update(watchedFiles, storageName);

		final var results = ri.openSearch("basename", empty(), 10).foundedFiles();
		assertThat(results.stream().findFirst().map(FileSearchResult::hashPath).orElse(null)).isEqualTo(fileHashPath);

		directoryConstraint = IGNORE;
		hiddenConstraint = IGNORE;
		linkConstraint = IGNORE;
		specialConstraint = IGNORE;
		assertThatFoundWithBooleanConstraints();

		directoryConstraint = directory ? MUST : MUST_NOT;
		assertThatFoundWithBooleanConstraints();
		directoryConstraint = directory ? MUST_NOT : MUST;
		assertThatNotFoundWithBooleanConstraints();
		directoryConstraint = IGNORE;

		hiddenConstraint = hidden ? MUST : MUST_NOT;
		assertThatFoundWithBooleanConstraints();
		hiddenConstraint = hidden ? MUST_NOT : MUST;
		assertThatNotFoundWithBooleanConstraints();
		hiddenConstraint = IGNORE;

		linkConstraint = link ? MUST : MUST_NOT;
		assertThatFoundWithBooleanConstraints();
		linkConstraint = link ? MUST_NOT : MUST;
		assertThatNotFoundWithBooleanConstraints();
		linkConstraint = IGNORE;

		specialConstraint = special ? MUST : MUST_NOT;
		assertThatFoundWithBooleanConstraints();
		specialConstraint = special ? MUST_NOT : MUST;
		assertThatNotFoundWithBooleanConstraints();
		specialConstraint = IGNORE;

		directoryConstraint = directory ? MUST : MUST_NOT;
		hiddenConstraint = hidden ? MUST : MUST_NOT;
		linkConstraint = link ? MUST : MUST_NOT;
		specialConstraint = special ? MUST : MUST_NOT;
		assertThatFoundWithBooleanConstraints();

		directoryConstraint = directory ? MUST_NOT : MUST;
		hiddenConstraint = hidden ? MUST_NOT : MUST;
		linkConstraint = link ? MUST_NOT : MUST;
		specialConstraint = special ? MUST_NOT : MUST;
		assertThatNotFoundWithBooleanConstraints();

		verify(file, atLeastOnce()).getPath();
		clearInvocations(file);
	}

	private void assertThatFoundWithBooleanConstraints() {
		assertThat(ri.openSearch("basename",
				Optional.ofNullable(new FileSearchConstraints(
						directoryConstraint, hiddenConstraint, linkConstraint, specialConstraint,
						NO_RANGE, NO_RANGE, List.of(), null, null)), 10).foundedFiles()
				.stream().findFirst().map(FileSearchResult::hashPath).orElse(null)).isEqualTo(fileHashPath);
	}

	private void assertThatNotFoundWithBooleanConstraints() {
		assertThat(ri.openSearch("basename",
				Optional.ofNullable(new FileSearchConstraints(
						directoryConstraint, hiddenConstraint, linkConstraint, specialConstraint,
						NO_RANGE, NO_RANGE, List.of(), null, null)), 10).foundedFiles()).isEmpty();
	}

	@Test
	void testOpenSearch_length_size_constraints() {
		ri.update(new WatchedFiles(Set.of(file, makeFalseFile()), Set.of(), Set.of(), 0), storageName);

		constraintDateRange = new SearchConstraintRange(true, lastModified - 1, lastModified + 1);
		constraintSizeRange = new SearchConstraintRange(false, length - 1, length + 1);
		assertThatFoundWithDateSizeConstraints();

		constraintDateRange = new SearchConstraintRange(true, lastModified + 1, lastModified + 2);
		assertThatNotFoundWithDateSizeConstraints();
		constraintDateRange = new SearchConstraintRange(true, lastModified - 2, lastModified - 1);
		assertThatNotFoundWithDateSizeConstraints();

		constraintDateRange = new SearchConstraintRange(false, lastModified - 1, lastModified + 1);
		constraintSizeRange = new SearchConstraintRange(true, length - 1, length + 1);
		assertThatFoundWithDateSizeConstraints();

		constraintSizeRange = new SearchConstraintRange(true, length + 1, length + 2);
		assertThatNotFoundWithDateSizeConstraints();
		constraintSizeRange = new SearchConstraintRange(true, length - 2, length - 1);
		assertThatNotFoundWithDateSizeConstraints();

		constraintDateRange = new SearchConstraintRange(true, lastModified - 1, lastModified + 1);
		constraintSizeRange = new SearchConstraintRange(true, length - 1, length + 1);
		assertThatFoundWithDateSizeConstraints();

		constraintDateRange = new SearchConstraintRange(true, lastModified - 2, lastModified - 1);
		constraintSizeRange = new SearchConstraintRange(true, length + 1, length + 2);
		assertThatNotFoundWithDateSizeConstraints();

		verify(file, atLeastOnce()).getPath();
		clearInvocations(file);
	}

	private void assertThatFoundWithDateSizeConstraints() {
		assertThat(ri.openSearch("basename",
				Optional.ofNullable(new FileSearchConstraints(
						IGNORE, IGNORE, IGNORE, IGNORE,
						constraintDateRange, constraintSizeRange, List.of(), null, null)), 10).foundedFiles()
				.stream().findFirst().map(FileSearchResult::hashPath).orElse(null)).isEqualTo(fileHashPath);
	}

	private void assertThatNotFoundWithDateSizeConstraints() {
		assertThat(ri.openSearch("basename",
				Optional.ofNullable(new FileSearchConstraints(
						IGNORE, IGNORE, IGNORE, IGNORE,
						constraintDateRange, constraintSizeRange, List.of(), null, null)), 10).foundedFiles())
								.isEmpty();
	}

	String parentPathConstraint;
	String parentHashPathConstraint;

	@Test
	void testOpenSearch_parentpath_constraints() {
		ri.update(new WatchedFiles(Set.of(file, makeFalseFile()), Set.of(), Set.of(), 0), storageName);

		parentPathConstraint = "/";
		assertThatFoundWithParentPathHashConstraints();
		parentPathConstraint = parentPath;
		assertThatFoundWithParentPathHashConstraints();
		parentPathConstraint = "IMPOSSIBLE";
		assertThatNotFoundWithParentPathHashConstraints();

		parentPathConstraint = null;
		parentHashPathConstraint = hashPath(realmName, storageName, "/");
		assertThatNotFoundWithParentPathHashConstraints();
		parentHashPathConstraint = hashPath(realmName, storageName, parentPath);
		assertThatFoundWithParentPathHashConstraints();
		parentHashPathConstraint = hashPath(realmName, storageName, "IMPOSSIBLE");
		assertThatNotFoundWithParentPathHashConstraints();

		parentPathConstraint = parentPath;
		parentHashPathConstraint = hashPath(realmName, storageName, parentPath);
		assertThatFoundWithParentPathHashConstraints();

		parentPathConstraint = parentPath;
		parentHashPathConstraint = hashPath(realmName, storageName, "IMPOSSIBLE");
		assertThatFoundWithParentPathHashConstraints();

		verify(file, atLeastOnce()).getPath();
		clearInvocations(file);
	}

	private void assertThatFoundWithParentPathHashConstraints() {
		assertThat(ri.openSearch("basename",
				Optional.ofNullable(new FileSearchConstraints(
						IGNORE, IGNORE, IGNORE, IGNORE,
						NO_RANGE, NO_RANGE, List.of(), parentPathConstraint, parentHashPathConstraint)), 10)
				.foundedFiles()
				.stream().findFirst().map(FileSearchResult::hashPath).orElse(null)).isEqualTo(fileHashPath);
	}

	private void assertThatNotFoundWithParentPathHashConstraints() {
		assertThat(ri.openSearch("basename",
				Optional.ofNullable(new FileSearchConstraints(
						IGNORE, IGNORE, IGNORE, IGNORE,
						NO_RANGE, NO_RANGE, List.of(), parentPathConstraint, parentHashPathConstraint)), 10)
				.foundedFiles()).isEmpty();
	}

	@Test
	void testFile_add_delete_update() {
		final var scanResultAdd = new WatchedFiles(Set.of(file, makeFalseFile()), Set.of(), Set.of(), 0);
		ri.update(scanResultAdd, storageName);

		assertThat(ri.openSearch(getBaseName(fileName), empty(), 10).foundedFiles().stream().findFirst().map(
				FileSearchResult::hashPath).orElse(null))
						.isEqualTo(fileHashPath);

		final var scanResultLosted = new WatchedFiles(Set.of(), Set.of(file), Set.of(), 0);
		ri.update(scanResultLosted, storageName);
		assertThat(ri.openSearch(getBaseName(fileName), empty(), 10).foundedFiles())
				.isEmpty();

		constraintSizeRange = new SearchConstraintRange(true, length - 100, length + 100);
		constraint = new FileSearchConstraints(
				IGNORE, IGNORE, IGNORE, IGNORE,
				NO_RANGE, constraintSizeRange, List.of(), null, null);

		ri.update(scanResultAdd, storageName);
		assertThat(ri.openSearch(getBaseName(fileName), Optional.ofNullable(constraint), 10).foundedFiles().stream()
				.findFirst()
				.map(FileSearchResult::hashPath).orElse(null))
						.isEqualTo(fileHashPath);

		final var scanResultUpdated = new WatchedFiles(Set.of(), Set.of(), Set.of(file), 0);
		when(file.length()).thenReturn(1l);

		ri.update(scanResultUpdated, storageName);

		assertThat(ri.openSearch(getBaseName(fileName), empty(), 10).foundedFiles().stream().findFirst().map(
				FileSearchResult::hashPath).orElse(null))
						.isEqualTo(fileHashPath);
		assertThat(ri.openSearch(getBaseName(fileName), Optional.ofNullable(constraint), 10).foundedFiles()).isEmpty();

		verify(file, atLeastOnce()).getPath();
		clearInvocations(file);
	}

	@Test
	void testReset() {
		final var scanResult = new WatchedFiles(Set.of(makeFalseFile(), makeFalseFile()), Set.of(), Set.of(), 0);
		ri.update(scanResult, storageName);
		assertThat(ri.openSearch("*", empty(), 10).foundedFiles()).size().isEqualTo(2);

		final var session = ri.reset(10);
		session.accept(new FileEntity(realmName, storageName, file));
		session.close();

		final var results = ri.openSearch("*", empty(), 10).foundedFiles();
		assertThat(results).size().isEqualTo(1);
		assertThat(results.get(0).hashPath())
				.isEqualTo(fileHashPath);

		final var result0 = results.get(0);
		assertThat(result0.hashPath()).isEqualTo(fileHashPath);
		assertThat(result0.name()).isEqualTo(fileName);
		assertThat(result0.score()).isGreaterThan(0f);
		assertThat(result0.storage()).isEqualTo(storageName);
		assertThat(result0.explain()).isNull();

		verify(file, atLeastOnce()).getPath();
		clearInvocations(file);
	}

}
