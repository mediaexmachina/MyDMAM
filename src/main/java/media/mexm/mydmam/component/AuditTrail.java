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

import static java.util.Collections.unmodifiableMap;
import static media.mexm.mydmam.configuration.PathIndexingConf.correctName;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.sqlite.SQLiteConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.audittrail.RealmAuditTrail;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import tv.hd3g.jobkit.engine.JobKitEngine;

@Component
@Slf4j
public class AuditTrail implements InitializingBean { // TODO test

	@Autowired
	private JobKitEngine jobkitEngine;
	@Autowired
	private MyDMAMConfigurationProperties conf;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private SQLiteConfig sqliteConfig;

	@Value("${mydmamConsts.auditTrailSpoolName:audittrail}")
	private String auditTrailSpoolName;

	private Map<String, RealmAuditTrail> auditTrailByRealmName;

	@Override
	public void afterPropertiesSet() throws Exception {
		final var pathIndexing = conf.pathindexing();
		if (pathIndexing == null) {
			return;
		}

		final var tempAuditTrailByRealmName = new HashMap<String, RealmAuditTrail>();

		for (final var entry : Optional.ofNullable(pathIndexing.realms())
				.orElse(Map.of())
				.entrySet()) {
			final var realmName = correctName(entry.getKey(), "realm name");

			final var oWorkingDirectory = entry.getValue().getValidWorkingDirectory(realmName);
			if (oWorkingDirectory.isEmpty()) {
				continue;
			}

			log.info("Prepare audit trail for realm={}, on {}",
					realmName, oWorkingDirectory.get().getAbsolutePath());

			final var realmAuditTrail = new RealmAuditTrail(
					jobkitEngine,
					sqliteConfig,
					objectMapper,
					auditTrailSpoolName,
					realmName,
					oWorkingDirectory.get());
			tempAuditTrailByRealmName.put(realmName, realmAuditTrail);
		}
		auditTrailByRealmName = unmodifiableMap(tempAuditTrailByRealmName);
	}

	public Optional<RealmAuditTrail> getAuditTrailByRealm(final String realm) {
		if (auditTrailByRealmName.containsKey(realm) == false) {
			return Optional.empty();
		}
		return Optional.ofNullable(auditTrailByRealmName.get(realm));
	}

}
