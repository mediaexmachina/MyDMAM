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
package media.mexm.mydmam.audittrail;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.jobkit.engine.JobKitEngine;

@Slf4j
public class RealmAuditTrail {
	private final JobKitEngine jobkitEngine;
	private final ObjectMapper objectMapper;

	private final String auditTrailSpoolName;
	private final String realmName;
	private final AuditTrailSQLite sqlite;

	public RealmAuditTrail(final JobKitEngine jobkitEngine,
						   final ObjectMapper objectMapper,
						   final String auditTrailSpoolName,
						   final String realmName,
						   final AuditTrailSQLite sqlite) {
		this.jobkitEngine = requireNonNull(jobkitEngine);
		this.objectMapper = requireNonNull(objectMapper);
		this.auditTrailSpoolName = requireNonNull(auditTrailSpoolName);
		this.realmName = requireNonNull(realmName);
		this.sqlite = requireNonNull(sqlite);
	}

	public void asyncPersist(final String issuer,
							 final String event,
							 final AuditTrailBatchInsertObject insert) {
		requireNonNull(issuer);
		requireNonNull(insert);
		requireNonNull(event);

		log.trace("Async persist audit trail realm={} issuer={} objectReference={} event={}",
				realmName, issuer, insert.objectReference(), event);
		final var now = System.currentTimeMillis();

		jobkitEngine.runOneShot(
				"Save audit trail for " + realmName + ", event=" + event,
				auditTrailSpoolName, 0, () -> sqlite.insert(
						now,
						issuer,
						event,
						List.of(insert.makeAuditTrailItem(objectMapper))),
				e -> {
					if (e != null) {
						log.error("Can't write to SQlite audit trail for " + realmName, e);
					}
				});
	}

	public void asyncPersist(final String issuer,
							 final String event,
							 final Collection<AuditTrailBatchInsertObject> inserts) {
		requireNonNull(issuer);
		requireNonNull(inserts);
		requireNonNull(event);
		if (inserts.isEmpty()) {
			return;
		}

		log.trace("Async persist audit trail realm={} issuer={} objects={} event={}",
				realmName, issuer, inserts.size(), event);
		final var now = System.currentTimeMillis();

		jobkitEngine.runOneShot(
				"Save audit trail for " + realmName + ", event=" + event,
				auditTrailSpoolName, 0, () -> sqlite.insert(
						now,
						issuer,
						event,
						inserts.stream()
								.map(f -> f.makeAuditTrailItem(objectMapper))
								.toList()),
				e -> {
					if (e != null) {
						log.error("Can't write to SQlite audit trail for " + realmName, e);
					}
				});
	}

}
