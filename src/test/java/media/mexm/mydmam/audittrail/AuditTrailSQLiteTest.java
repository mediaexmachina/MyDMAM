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
package media.mexm.mydmam.audittrail;

import static java.util.stream.Collectors.joining;
import static media.mexm.mydmam.audittrail.AuditTrailSQLite.VERSION;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.getTempDirectory;
import static org.apache.commons.io.FileUtils.touch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sqlite.SQLiteConfig;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class AuditTrailSQLiteTest {

	@Mock
	SQLiteConfig sqliteConfig;
	@Mock
	Connection connection;
	@Mock
	Statement statement;
	@Mock
	PreparedStatement preparedStatement;
	@Mock
	AuditTrailItem auditTrailItem;

	@Captor
	ArgumentCaptor<String> url0Captor;
	@Captor
	ArgumentCaptor<String> url1Captor;
	@Captor
	ArgumentCaptor<String> sqlCaptor;

	@Fake
	String realm;
	@Fake
	String tempDirName;
	@Fake
	long date;
	@Fake
	String issuer;
	@Fake
	String event;
	@Fake
	AuditTrailObjectType auditTrailObjectType;
	@Fake
	String objectReference;
	@Fake
	String objectPayload;

	File realmWorkingDirectory;
	AuditTrailSQLite ats;

	@BeforeEach
	void init() throws SQLException, IOException {
		realmWorkingDirectory = new File(getTempDirectory(),
				"mydmam-" + getClass().getSimpleName() + "-" + tempDirName);
		if (realmWorkingDirectory.exists()) {
			forceDelete(realmWorkingDirectory);
		}

		ats = new AuditTrailSQLite(realm, realmWorkingDirectory, sqliteConfig);
		assertThat(ats.getSqliteFile()).hasParent(realmWorkingDirectory);

		when(sqliteConfig.createConnection(any())).thenReturn(connection);
		when(connection.createStatement()).thenReturn(statement);
		when(connection.prepareStatement(any())).thenReturn(preparedStatement);

		when(auditTrailItem.objectType()).thenReturn(auditTrailObjectType);
		when(auditTrailItem.objectReference()).thenReturn(objectReference);
		when(auditTrailItem.objectPayload()).thenReturn(objectPayload);
	}

	@AfterEach
	void ends() {
		deleteQuietly(realmWorkingDirectory);
	}

	@Test
	void testInsert_empty() throws SQLException {// NOSONAR S2699
		ats.insert(date, issuer, event, List.of());
	}

	@Test
	void testInsert() throws SQLException { // NOSONAR S5961
		ats.insert(date, issuer, event, List.of(auditTrailItem));

		verify(sqliteConfig, times(1)).createConnection(url0Captor.capture());
		verify(connection, atLeast(1)).createStatement();
		verify(connection, atLeast(1)).prepareStatement(url1Captor.capture());
		verify(statement, atLeast(0)).setQueryTimeout(anyInt());
		verify(statement, atLeast(1)).executeUpdate(sqlCaptor.capture());
		verify(statement, atLeast(1)).close();
		verify(preparedStatement, atLeast(0)).setQueryTimeout(anyInt());
		verify(preparedStatement, times(1)).setString(1, "version");
		verify(preparedStatement, times(1)).setString(2, VERSION);
		verify(preparedStatement, times(1)).setString(1, "realm");
		verify(preparedStatement, times(1)).setString(2, realm);
		verify(preparedStatement, times(1)).setLong(anyInt(), eq(date));
		verify(preparedStatement, times(1)).setString(anyInt(), eq(issuer));
		verify(preparedStatement, times(1)).setString(anyInt(), eq(event));
		verify(preparedStatement, times(1)).setString(anyInt(), eq(auditTrailObjectType.toString().toLowerCase()));
		verify(preparedStatement, times(1)).setString(anyInt(), eq(objectReference));
		verify(preparedStatement, times(1)).setString(anyInt(), eq(objectPayload));
		verify(preparedStatement, times(3)).addBatch();
		verify(preparedStatement, times(2)).executeBatch();
		verify(preparedStatement, times(2)).close();
		verify(auditTrailItem, atLeast(1)).objectType();
		verify(auditTrailItem, atLeast(1)).objectReference();
		verify(auditTrailItem, atLeast(1)).objectPayload();

		assertThat(url0Captor.getValue()).containsSequence(
				"jdbc:sqlite:" + realmWorkingDirectory.getPath().replace('\\', '/'));

		assertThat(sqlCaptor.getAllValues()).isNotEmpty();
		final var prepareQueries = sqlCaptor.getAllValues().stream().collect(joining("\n"));
		assertThat(prepareQueries).contains("CREATE TABLE", "CREATE INDEX");

		assertThat(url1Captor.getAllValues()).size().isEqualTo(2);
		assertThat(url1Captor.getAllValues().get(0)).contains(
				"INSERT INTO setup",
				"key", "value",
				"?, ?");

		assertThat(url1Captor.getAllValues().get(1)).contains(
				"INSERT INTO events",
				"date", "issuer", "event", "object_type", "object_reference", "object_payload",
				"?, ?, ?, ?, ?, ?");
	}

	@Test
	void testInsert_twice() throws SQLException, IOException {
		when(connection.isClosed()).thenReturn(false);

		ats.insert(date, issuer, event, List.of(auditTrailItem));
		touch(ats.getSqliteFile());
		ats.insert(date, issuer, event, List.of(auditTrailItem));

		verify(sqliteConfig, times(1)).createConnection(url0Captor.capture());
		verify(connection, atLeast(1)).createStatement();
		verify(connection, times(3)).prepareStatement(url1Captor.capture());
		verify(connection, atLeast(1)).isClosed();
		verify(statement, atLeast(0)).setQueryTimeout(anyInt());
		verify(statement, atLeast(1)).executeUpdate(sqlCaptor.capture());
		verify(statement, atLeast(1)).close();

		Mockito.reset(preparedStatement, auditTrailItem);
	}

	@Test
	void testInsert_twice_disconnected() throws SQLException, IOException {
		when(connection.isClosed()).thenReturn(true);

		ats.insert(date, issuer, event, List.of(auditTrailItem));
		touch(ats.getSqliteFile());
		ats.insert(date, issuer, event, List.of(auditTrailItem));

		verify(sqliteConfig, times(2)).createConnection(url0Captor.capture());
		verify(connection, atLeast(1)).createStatement();
		verify(connection, times(3)).prepareStatement(url1Captor.capture());
		verify(connection, atLeast(1)).isClosed();
		verify(statement, atLeast(0)).setQueryTimeout(anyInt());
		verify(statement, atLeast(1)).executeUpdate(sqlCaptor.capture());
		verify(statement, atLeast(1)).close();

		Mockito.reset(preparedStatement, auditTrailItem);
	}

}
