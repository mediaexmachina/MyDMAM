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
package media.mexm.mydmam.configuration;

import static org.apache.commons.io.FilenameUtils.getFullPathNoEndSeparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.watchfolder.ObservedFolder;

@ExtendWith(MockToolsExtendsJunit.class)
class PathIndexingStorageTest {

	@Mock
	Set<String> allowedExtentions;
	@Mock
	Set<String> blockedExtentions;
	@Mock
	Set<String> ignoreRelativePaths;
	@Mock
	Set<String> ignoreFiles;
	@Mock
	Set<String> allowedFileNames;
	@Mock
	Set<String> allowedDirNames;
	@Mock
	Set<String> blockedFileNames;
	@Mock
	Set<String> blockedDirNames;

	@Fake
	String spoolScans;
	@Fake
	int maxDeep;
	@Fake
	int retryAfterTimeFactor;
	@Fake
	boolean allowedHidden;
	@Fake
	boolean allowedLinks;
	@Fake
	boolean noScans;
	@Fake(min = 1, max = 10000)
	long timeBetweenScansDuration;
	@Fake
	String realmName;
	@Fake
	String storageName;

	String path;
	PathIndexingStorage pis;
	Duration timeBetweenScans;
	Duration minFixedStateTime;
	ObservedFolder observedFolder;

	@BeforeEach
	void init() {
		path = new File(".").getAbsolutePath();
		timeBetweenScans = Duration.ofMillis(timeBetweenScansDuration);
		minFixedStateTime = Duration.ofMillis(timeBetweenScansDuration);

		makePis();
	}

	void makePis() {
		pis = new PathIndexingStorage(path, maxDeep, timeBetweenScans, retryAfterTimeFactor, allowedExtentions,
				blockedExtentions, ignoreRelativePaths, ignoreFiles, allowedFileNames, allowedDirNames,
				blockedFileNames, blockedDirNames, allowedHidden, allowedLinks, minFixedStateTime, noScans,
				spoolScans);
	}

	@Test
	void testInvalid_TimeBetweenScans() {
		timeBetweenScans = null;
		makePis();
		assertThat(pis.timeBetweenScans()).isNull();

		timeBetweenScans = Duration.ZERO;
		assertThrows(IllegalArgumentException.class, this::makePis);
		timeBetweenScans = Duration.ofMillis(-timeBetweenScansDuration);
		assertThrows(IllegalArgumentException.class, this::makePis);

	}

	@Test
	void testInvalid_path() {
		path = "/this-not-exists-" + spoolScans;
		assertThrows(IllegalArgumentException.class, this::makePis);
	}

	@Test
	void testInvalid_networkPath() {
		path = "ftp://0.0.0.0/";
		assertThrows(IllegalArgumentException.class, this::makePis);
	}

	@Test
	void testMakeObservedFolder() {
		observedFolder = pis.makeObservedFolder(realmName, storageName);
		assertNotNull(observedFolder);

		assertThat(observedFolder.getLabel()).contains(realmName, storageName);

		assertThat(observedFolder.getAllowedDirNames()).isEqualTo(allowedDirNames);
		assertThat(observedFolder.getAllowedExtentions()).isEqualTo(allowedExtentions);
		assertThat(observedFolder.getAllowedFileNames()).isEqualTo(allowedFileNames);
		assertThat(observedFolder.isAllowedHidden()).isEqualTo(allowedHidden);
		assertThat(observedFolder.isAllowedLinks()).isEqualTo(allowedLinks);
		assertThat(observedFolder.getBlockedDirNames()).isEqualTo(blockedDirNames);
		assertThat(observedFolder.getBlockedExtentions()).isEqualTo(blockedExtentions);
		assertThat(observedFolder.getBlockedFileNames()).isEqualTo(blockedFileNames);
		assertThat(observedFolder.isDisabled()).isEqualTo(noScans);
		assertThat(observedFolder.getIgnoreFiles()).isEqualTo(ignoreFiles);
		assertThat(observedFolder.getIgnoreRelativePaths()).isEqualTo(ignoreRelativePaths);
		assertThat(observedFolder.getMinFixedStateTime()).isEqualTo(minFixedStateTime);
		assertThat(observedFolder.getRetryAfterTimeFactor()).isEqualTo(retryAfterTimeFactor);
		assertThat(observedFolder.isRecursive()).isTrue();
		assertThat(observedFolder.getRetryAfterTimeFactor()).isEqualTo(retryAfterTimeFactor);
		assertThat(observedFolder.getSpoolScans()).isEqualTo(spoolScans);
		assertThat(observedFolder.getTargetFolder()).contains(getFullPathNoEndSeparator(path));
		assertThat(observedFolder.getTimeBetweenScans()).isEqualTo(timeBetweenScans);
	}

}
