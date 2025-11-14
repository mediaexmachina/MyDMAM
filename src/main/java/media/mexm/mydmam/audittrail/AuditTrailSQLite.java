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

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import org.sqlite.SQLiteConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuditTrailSQLite {

	public static final String VERSION = "1";
	private static final String EXECUTE_SQL = "Execute SQL: {}";

	private final String realm;
	private final File sqliteFile;
	private final SQLiteConfig sqliteConfig;
	private final String url;
	private Connection connection;

	public AuditTrailSQLite(final String realm, final File realmWorkingDirectory, final SQLiteConfig sqliteConfig) {
		this.realm = realm;
		this.sqliteConfig = sqliteConfig;
		sqliteFile = new File(realmWorkingDirectory, "audittrail-" + realm + ".sqlite");
		url = "jdbc:sqlite:" + sqliteFile.getPath().replace('\\', '/');
	}

	/**
	 * For tests purpose
	 */
	File getSqliteFile() {
		return sqliteFile;
	}

	private void prepareSQLite() throws SQLException {
		connection = sqliteConfig.createConnection(url);
		try (final var statement = connection.createStatement()) {
			statement.setQueryTimeout(5);

			final var createTableEvents = """
					CREATE TABLE "events" (
					    "id"      INTEGER NOT NULL,
					    "date"    INTEGER NOT NULL,
					    "issuer"  TEXT NOT NULL,
					    "event"   TEXT NOT NULL,
					    "object_type"  TEXT NOT NULL,
					    "object_reference"  TEXT NOT NULL,
					    "object_payload" TEXT NOT NULL,
					PRIMARY KEY("id" AUTOINCREMENT)
					);
					""";
			log.debug(EXECUTE_SQL, createTableEvents);
			statement.executeUpdate(createTableEvents);

			final var createIndex = """
					CREATE INDEX "idx-events-object" ON "events" ("object_type", "object_reference");
					""";
			log.debug(EXECUTE_SQL, createIndex);
			statement.executeUpdate(createIndex);

			final var createTableSetup = """
					CREATE TABLE "setup" (
					    "key"   TEXT NOT NULL UNIQUE,
					    "value" TEXT NOT NULL,
					PRIMARY KEY("key")
					);
					""";
			log.debug(EXECUTE_SQL, createTableSetup);
			statement.executeUpdate(createTableSetup);
		}

		final var insertTableSetup = "INSERT INTO setup (key, value) VALUES (?, ?)";
		try (final var pStatement = connection.prepareStatement(insertTableSetup)) {
			pStatement.setString(1, "version");
			pStatement.setString(2, VERSION);
			pStatement.addBatch();

			pStatement.setString(1, "realm");
			pStatement.setString(2, realm);
			pStatement.addBatch();

			pStatement.executeBatch();
		}

	}

	private void checkConnection() throws SQLException {
		if (connection == null || connection.isClosed()) {
			if (sqliteFile.exists() == false) {
				prepareSQLite();
			} else {
				connection = sqliteConfig.createConnection(url);
			}
		}
	}

	public synchronized void insert(final long date,
									final String issuer,
									final String event,
									final Collection<AuditTrailItem> auditTrailItems) throws SQLException {
		if (auditTrailItems.isEmpty()) {
			return;
		}
		checkConnection();
		final var sql = """
				INSERT INTO events (date, issuer, event, object_type, object_reference, object_payload)
				VALUES (?, ?, ?, ?, ?, ?)
				""";
		try (final var statement = connection.prepareStatement(sql)) {
			statement.setQueryTimeout(10);
			statement.setLong(1, date);
			statement.setString(2, issuer);
			statement.setString(3, event);

			for (final var entry : auditTrailItems) {
				statement.setString(4, entry.objectType().toString().toLowerCase());
				statement.setString(5, entry.objectReference());
				statement.setString(6, entry.objectPayload());
				statement.addBatch();
			}

			statement.executeBatch();
		}
	}

}
