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

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import tv.hd3g.jobkit.engine.JobKitEngine;

@Slf4j
public class RealmAuditTrail { // TODO test
	private final JobKitEngine jobkitEngine;
	private final ObjectMapper objectMapper;

	private final String auditTrailSpoolName;
	private final String realmName;
	private final AuditTrailSQLite sqlite;

	public RealmAuditTrail(final JobKitEngine jobkitEngine,
						   final ObjectMapper objectMapper,
						   final String auditTrailSpoolName,
						   final String realmName,
						   final File realmWorkingDirectory) {
		this.jobkitEngine = requireNonNull(jobkitEngine);
		this.objectMapper = requireNonNull(objectMapper);
		this.auditTrailSpoolName = requireNonNull(auditTrailSpoolName);
		this.realmName = requireNonNull(realmName);
		sqlite = new AuditTrailSQLite(realmName, realmWorkingDirectory);
	}

	public void asyncPersist(final String issuer, final String object, final String event, final Object data) {
		requireNonNull(issuer);
		requireNonNull(object);
		requireNonNull(event);
		requireNonNull(data);

		log.trace("Async persist audit trail realm={} issuer={} object={} event={}", realmName, issuer, object, event);
		final var now = System.currentTimeMillis();

		jobkitEngine.runOneShot(
				"Save audit trail for " + realmName + ", event=" + event,
				auditTrailSpoolName, 0, () -> sqlite.insert(
						now,
						issuer,
						event,
						List.of(new BatchInsert(object, objectMapper.writeValueAsString(data)))),
				e -> {
					if (e != null) {
						log.error("Can't write to SQlite audit trail for " + realmName, e);
					}
				});
	}

	public void asyncPersist(final String issuer, final String event, final Map<String, Object> dataByObject) {
		requireNonNull(issuer);
		requireNonNull(dataByObject);
		requireNonNull(event);
		if (dataByObject.isEmpty()) {
			return;
		}

		log.trace("Async persist audit trail realm={} issuer={} objects={} event={}",
				realmName, issuer, dataByObject.size(), event);
		final var now = System.currentTimeMillis();

		jobkitEngine.runOneShot(
				"Save audit trail for " + realmName + ", event=" + event,
				auditTrailSpoolName, 0, () -> {

					final var batchInserts = new ArrayList<BatchInsert>(dataByObject.size());
					for (final var entry : dataByObject.entrySet()) {
						batchInserts.add(
								new BatchInsert(
										entry.getKey(),
										objectMapper.writeValueAsString(entry.getValue())));
					}

					sqlite.insert(
							now,
							issuer,
							event,
							unmodifiableList(batchInserts));
				},
				e -> {
					if (e != null) {
						log.error("Can't write to SQlite audit trail for " + realmName, e);
					}
				});
	}

}
