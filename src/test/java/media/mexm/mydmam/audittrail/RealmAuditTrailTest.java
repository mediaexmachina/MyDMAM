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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import com.fasterxml.jackson.databind.ObjectMapper;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;

@ExtendWith(MockToolsExtendsJunit.class)
class RealmAuditTrailTest {

	FlatJobKitEngine jobkitEngine;
	@Mock
	AuditTrailSQLite sqlite;
	@Mock
	ObjectMapper objectMapper;
	@Mock
	AuditTrailBatchInsertObject insert;
	@Mock
	AuditTrailItem auditTrailItem;

	@Fake
	String auditTrailSpoolName;
	@Fake
	String realmName;
	@Fake
	String issuer;
	@Fake
	String event;

	@Captor
	ArgumentCaptor<Collection<AuditTrailItem>> auditTrailItemsCaptor;
	@Captor
	ArgumentCaptor<Long> dateCaptor;

	RealmAuditTrail rat;

	@BeforeEach
	void init() {
		jobkitEngine = new FlatJobKitEngine();

		rat = new RealmAuditTrail(
				jobkitEngine,
				objectMapper,
				auditTrailSpoolName,
				realmName,
				sqlite);
	}

	@AfterEach
	void ends() {
		assertThat(jobkitEngine.getEndEventsList()).isEmpty();
		assertTrue(jobkitEngine.isEmptyActiveServicesList());
	}

	@Nested
	class WithItems {

		@BeforeEach
		void init() {
			when(insert.makeAuditTrailItem(objectMapper)).thenReturn(auditTrailItem);
		}

		@Test
		void testAsyncPersist_one() { // NOSONAR S2699
			rat.asyncPersist(issuer, event, insert);
		}

		@Test
		void testAsyncPersist_many() {// NOSONAR S2699
			rat.asyncPersist(issuer, event, List.of(insert));
		}

		@AfterEach
		void ends() throws SQLException {
			verify(insert, atLeast(0)).objectReference();
			verify(insert, times(1)).makeAuditTrailItem(objectMapper);

			verify(sqlite, times(1)).insert(
					dateCaptor.capture(),
					eq(issuer),
					eq(event),
					auditTrailItemsCaptor.capture());

			assertThat(dateCaptor.getValue()).isLessThanOrEqualTo(System.currentTimeMillis());
			assertThat(auditTrailItemsCaptor.getValue()).size().isEqualTo(1);
			assertThat(auditTrailItemsCaptor.getValue().iterator().next()).isEqualTo(auditTrailItem);
		}
	}

	@Test
	void testAsyncPersist_many_empty() {// NOSONAR S2699
		rat.asyncPersist(issuer, event, List.of());
	}

}
