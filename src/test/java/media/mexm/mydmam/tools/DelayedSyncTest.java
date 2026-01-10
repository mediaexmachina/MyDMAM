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
 * Copyright (C) Media ex Machina 2026
 *
 */
package media.mexm.mydmam.tools;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class DelayedSyncTest {

	@Mock
	DelayedSyncConfiguration delayedSyncConfiguration;
	@Mock
	Consumer<List<Object>> onSync;
	@Captor
	ArgumentCaptor<List<Object>> onSyncCaptor;

	@Fake(min = 10, max = 100)
	int maxEntries;

	DelayedSync<Object> ds;

	@BeforeEach
	void init() {
		when(delayedSyncConfiguration.maxDelay()).thenReturn(Duration.ofHours(1000));
		when(delayedSyncConfiguration.maxEntries()).thenReturn(maxEntries);

		ds = new DelayedSync<>(delayedSyncConfiguration, onSync);
	}

	@AfterEach
	void ends() {
		verify(delayedSyncConfiguration, atLeastOnce()).maxEntries();
		verify(delayedSyncConfiguration, atLeastOnce()).maxDelay();
	}

	static Object makeObject() {
		return Mockito.mock(Object.class);
	}

	@Test
	void testAdd_maxCount() {
		IntStream.range(0, maxEntries - 1)
				.parallel()
				.forEach(_ -> {
					ds.add(makeObject());
					verifyNoInteractions(onSync);
				});

		ds.add(makeObject());
		verify(onSync, times(1)).accept(onSyncCaptor.capture());
		final var values = onSyncCaptor.getValue();
		assertThat(values).hasSize(maxEntries);

		verifyNoInteractions(values.toArray());

		ds.add(makeObject());
	}

	@Test
	void testAdd_timeout() {
		when(delayedSyncConfiguration.maxDelay()).thenReturn(Duration.ofMillis(1));
		ds = new DelayedSync<>(delayedSyncConfiguration, onSync);
		ds.add(makeObject());
		ds.waitToSync();

		verify(onSync, times(1)).accept(onSyncCaptor.capture());
		final var values = onSyncCaptor.getValue();
		assertThat(values).hasSize(1);
		verifyNoInteractions(values.toArray());
	}

	@Test
	void testMultipleAdd_timeout() {
		when(delayedSyncConfiguration.maxDelay()).thenReturn(Duration.ofMillis(1));
		ds = new DelayedSync<>(delayedSyncConfiguration, onSync);

		IntStream.range(0, maxEntries / 2)
				.parallel()
				.forEach(_ -> {
					ds.add(makeObject());
					try {
						sleep(1); // NOSONAR S2925
					} catch (final InterruptedException e) {
						throw new IllegalCallerException(e);
					}
				});

		ds.waitToSync();

		verify(onSync, atLeastOnce()).accept(onSyncCaptor.capture());
		final var values = onSyncCaptor.getAllValues().stream().flatMap(List::stream).toList();
		assertThat(values).hasSize(maxEntries / 2);
		verifyNoInteractions(values.toArray());
	}

	@Test
	void testMultipleAdd_max_timeout() {
		when(delayedSyncConfiguration.maxDelay()).thenReturn(Duration.ofMillis(1));
		ds = new DelayedSync<>(delayedSyncConfiguration, onSync);

		final var totalEntries = maxEntries * 2;

		IntStream.range(0, totalEntries)
				.parallel()
				.forEach(_ -> {
					ds.add(makeObject());
					try {
						sleep(0, 200); // NOSONAR S2925
					} catch (final InterruptedException e) {
						throw new IllegalCallerException(e);
					}
				});

		ds.waitToSync();

		verify(onSync, atLeastOnce()).accept(onSyncCaptor.capture());
		final var values = onSyncCaptor.getAllValues().stream().flatMap(List::stream).toList();
		assertThat(values).hasSize(totalEntries);
		verifyNoInteractions(values.toArray());
	}

}
