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
package media.mexm.mydmam.component;

import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.PathIndexingConf;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import media.mexm.mydmam.configuration.TechnicalName;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.repository.FileDao;
import media.mexm.mydmam.tools.FileEntityConsumer;
import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;
import tv.hd3g.jobkit.watchfolder.WatchedFiles;
import tv.hd3g.transfertfiles.CachedFileAttributes;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles({ "Default" })
@ExtendWith(MockToolsExtendsJunit.class)
class IndexerTest {
	static final Faker faker = net.datafaker.Faker.instance();

	@MockitoBean
	MyDMAMConfigurationProperties conf;
	@MockitoBean
	FileDao fileDao;
	@MockitoBean
	PathIndexer pathIndexer;

	@Autowired
	FlatJobKitEngine flatJobKitEngine;

	@Mock
	PathIndexingConf pathindexing;
	@Mock
	PathIndexingRealm pathIndexingRealm;

	@Fake
	String realmName;
	@Fake
	String badRealmName;
	@Fake
	String spoolName;

	@Autowired
	Indexer indexer;
	File realmWorkingDirectory;

	@BeforeEach
	void init() throws IOException {
		realmWorkingDirectory = new File(getTempDirectory(),
				"mydmam-" + getClass().getSimpleName() + "-indexer");
		if (realmWorkingDirectory.exists()) {
			forceDelete(realmWorkingDirectory);
		}
	}

	@AfterEach
	void ends() {
		assertTrue(flatJobKitEngine.isEmptyActiveServicesList());
		assertEquals(0, flatJobKitEngine.getEndEventsList().size());
	}

	@Test
	void testInit_nothing() throws IOException {
		indexer.init();
		assertThat(indexer.getIndexerByRealm(realmName)).isEmpty();

		verify(conf, times(1)).pathindexing();
	}

	@Test
	void testInit_noWorkingDir() throws IOException {
		when(conf.pathindexing()).thenReturn(pathindexing);
		when(pathindexing.realms()).thenReturn(Map.of(new TechnicalName(realmName), pathIndexingRealm));
		when(pathIndexingRealm.workingDirectory()).thenReturn(null);

		indexer.init();
		assertThat(indexer.getIndexerByRealm(realmName)).isEmpty();

		verify(conf, times(1)).pathindexing();
		verify(pathindexing, times(1)).realms();
		verify(pathIndexingRealm, times(1)).workingDirectory();
	}

	@Test
	void testInitGetIndexerByRealm() throws IOException {
		when(conf.pathindexing()).thenReturn(pathindexing);
		when(pathindexing.realms()).thenReturn(Map.of(new TechnicalName(realmName), pathIndexingRealm));
		when(pathIndexingRealm.workingDirectory()).thenReturn(realmWorkingDirectory);
		indexer.init();
		assertThat(indexer.getIndexerByRealm(realmName)).isNotEmpty();
		assertThat(indexer.getIndexerByRealm(badRealmName)).isEmpty();

		verify(conf, times(1)).pathindexing();
		verify(pathindexing, times(1)).realms();
		verify(pathIndexingRealm, times(1)).workingDirectory();
	}

	@Test
	void testDestroy() throws IOException {
		when(conf.pathindexing()).thenReturn(pathindexing);
		when(pathindexing.realms()).thenReturn(Map.of(new TechnicalName(realmName), pathIndexingRealm));
		when(pathIndexingRealm.workingDirectory()).thenReturn(realmWorkingDirectory);

		indexer.init();
		indexer.destroy();
		assertThat(indexer.getIndexerByRealm(realmName)).isEmpty();

		verify(conf, times(1)).pathindexing();
		verify(pathindexing, times(1)).realms();
		verify(pathIndexingRealm, times(1)).workingDirectory();
	}

	@Nested
	class Reset {

		@Mock
		CachedFileAttributes file;
		@Captor
		ArgumentCaptor<FileEntityConsumer> onFileCaptor;

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
		long lastModified;

		@BeforeEach
		void init() {
			fileName = faker.numerify("baseName#####") + "." + faker.numerify("extention#####");
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
		}

		@Test
		void testReset() throws IOException {
			when(conf.pathindexing()).thenReturn(pathindexing);
			when(pathindexing.realms()).thenReturn(Map.of(new TechnicalName(realmName), pathIndexingRealm));
			when(pathIndexingRealm.workingDirectory()).thenReturn(realmWorkingDirectory);

			indexer.init();
			final var realmIndexer = indexer.getIndexerByRealm(realmName).get();
			realmIndexer.update(new WatchedFiles(Set.of(file), Set.of(), Set.of(), 0), storageName);
			var searchResult = realmIndexer.openSearch("*", Optional.empty(), 10).foundedFiles();
			assertThat(searchResult).size().isEqualTo(1);
			assertThat(searchResult.get(0).name()).isEqualTo(fileName);

			fileName = faker.numerify("baseName#####") + "." + faker.numerify("extention#####");
			path = parentPath + "/" + fileName;
			when(file.getName()).thenReturn(fileName);
			when(file.getPath()).thenReturn(path);

			doAnswer(invocation -> {
				final var args = invocation.getArguments();
				final var onFile = (FileEntityConsumer) args[1];
				onFile.accept(new FileEntity(realmName, storageName, file));
				return null;
			}).when(fileDao).getAllFromRealm(eq(realmName), any());

			indexer.reset(spoolName);

			searchResult = realmIndexer.openSearch("*", Optional.empty(), 10).foundedFiles();
			assertThat(searchResult).size().isEqualTo(1);
			assertThat(searchResult.get(0).name()).isEqualTo(fileName);

			verify(conf, times(1)).pathindexing();
			verify(pathindexing, times(1)).realms();
			verify(pathIndexingRealm, times(1)).workingDirectory();
			verify(fileDao, times(1)).getAllFromRealm(eq(realmName), any());
			verify(file, atLeastOnce()).getPath();
			clearInvocations(file);
		}
	}
}
