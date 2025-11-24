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

import static java.util.Collections.synchronizedMap;
import static media.mexm.mydmam.configuration.PathIndexingConf.correctName;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.sqlite.SQLiteConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.audittrail.AuditTrailSQLite;
import media.mexm.mydmam.audittrail.RealmAuditTrail;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import tv.hd3g.jobkit.engine.JobKitEngine;

@Component
@Slf4j
public class AuditTrail {

	private final Map<String, RealmAuditTrail> auditTrailByRealmName = synchronizedMap(new HashMap<>());

	@Autowired
	JobKitEngine jobkitEngine;
	@Autowired
	MyDMAMConfigurationProperties conf;
	@Autowired
	ObjectMapper objectMapper;
	@Autowired
	SQLiteConfig sqliteConfig;
	@Value("${mydmamConsts.auditTrailSpoolName:audittrail}")
	String auditTrailSpoolName;

	public void init() {
		final var pathIndexing = conf.pathindexing();
		if (pathIndexing == null) {
			return;
		}

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

			final var sqlite = new AuditTrailSQLite(realmName, oWorkingDirectory.get(), sqliteConfig);
			final var realmAuditTrail = new RealmAuditTrail(
					jobkitEngine,
					objectMapper,
					auditTrailSpoolName,
					realmName,
					sqlite);

			auditTrailByRealmName.put(realmName, realmAuditTrail);
		}
	}

	public Optional<RealmAuditTrail> getAuditTrailByRealm(final String realm) {
		if (auditTrailByRealmName.containsKey(realm) == false) {
			return Optional.empty();
		}
		return Optional.ofNullable(auditTrailByRealmName.get(realm));
	}

}
