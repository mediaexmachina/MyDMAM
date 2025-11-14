/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2025
 *
 */
package media.mexm.mydmam.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.sqlite.SQLiteConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.PathIndexingConf;
import media.mexm.mydmam.configuration.PathIndexingRealm;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.JobKitEngine;

@ExtendWith(MockToolsExtendsJunit.class)
class AuditTrailTest {

	@Mock
	JobKitEngine jobkitEngine;
	@Mock
	MyDMAMConfigurationProperties conf;
	@Mock
	ObjectMapper objectMapper;
	@Mock
	SQLiteConfig sqliteConfig;
	@Mock
	PathIndexingRealm pathIndexingRealm;

	@Fake
	String auditTrailSpoolName;
	@Fake
	String realmName;
	@Fake
	File workingDirectory;

	AuditTrail auditTrail;
	PathIndexingConf pathIndexingConf;

	@BeforeEach
	void init() {
		pathIndexingConf = new PathIndexingConf(
				Map.of(realmName, pathIndexingRealm),
				null,
				null,
				null);

		when(pathIndexingRealm.getValidWorkingDirectory(realmName))
				.thenReturn(Optional.ofNullable(workingDirectory));
		when(conf.pathindexing()).thenReturn(pathIndexingConf);
	}

	@Test
	void testGetAuditTrailByRealm_emptyConf() {
		when(conf.pathindexing()).thenReturn(null);
		auditTrail = new AuditTrail(
				jobkitEngine,
				conf,
				objectMapper,
				sqliteConfig,
				auditTrailSpoolName);

		verify(conf, times(1)).pathindexing();
		assertThat(auditTrail.getAuditTrailByRealm(realmName)).isEmpty();
	}

	@Test
	void testGetAuditTrailByRealm_noWorkingDir() {
		when(pathIndexingRealm.getValidWorkingDirectory(realmName))
				.thenReturn(Optional.empty());

		auditTrail = new AuditTrail(
				jobkitEngine,
				conf,
				objectMapper,
				sqliteConfig,
				auditTrailSpoolName);

		verify(conf, times(1)).pathindexing();
		verify(pathIndexingRealm, times(1)).getValidWorkingDirectory(realmName);
		assertThat(auditTrail.getAuditTrailByRealm(realmName)).isEmpty();
	}

	@Test
	void testGetAuditTrailByRealm() {
		auditTrail = new AuditTrail(
				jobkitEngine,
				conf,
				objectMapper,
				sqliteConfig,
				auditTrailSpoolName);

		verify(conf, times(1)).pathindexing();
		verify(pathIndexingRealm, times(1)).getValidWorkingDirectory(realmName);

		final var realmAuditTrail = auditTrail.getAuditTrailByRealm(realmName);
		assertThat(realmAuditTrail).isNotEmpty();

	}

}
