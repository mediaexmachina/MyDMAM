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
package media.mexm.mydmam.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.repository.PendingActivityDao;
import media.mexm.mydmam.service.PendingActivityService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class PendingActivityJobTest {

	@Mock
	MediaAsset asset;
	@Mock
	ActivityHandler activityHandler;
	@Mock
	ActivityHandler newActivityHandler;
	@Mock
	PendingActivityDao pendingActivityDao;
	@Mock
	PendingActivityService pendingActivityService;

	@Fake
	ActivityEventType eventType;
	@Fake
	String spoolName;
	@Fake
	String previousHandler;
	@Fake
	String handlerName;
	@Fake
	String assetName;

	Set<String> previousHandlers;
	PendingActivityJob job;
	Exception e;

	@BeforeEach
	void init() {
		when(asset.getHashPath()).thenReturn(assetHashPath);
		when(activityHandler.getHandlerName()).thenReturn(handlerName);
		when(asset.getName()).thenReturn(assetName);

		previousHandlers = new HashSet<>(Set.of(previousHandler));
		job = new PendingActivityJob(
				spoolName,
				asset,
				activityHandler,
				eventType,
				previousHandlers,
				pendingActivityDao,
				pendingActivityService);
		e = new Exception("For tests purpose");
	}

	@Test
	void testEvolve() {
		final var evolved = job.evolve(newActivityHandler);
		assertEquals(spoolName, evolved.spoolName());
		assertEquals(asset, evolved.asset());
		assertEquals(newActivityHandler, evolved.activityHandler());
		assertEquals(eventType, evolved.eventType());
		assertEquals(previousHandlers, evolved.previousHandlers());
		assertEquals(pendingActivityDao, evolved.pendingActivityDao());
		assertEquals(pendingActivityService, evolved.pendingActivityService());
	}

	@Fake
	String assetHashPath;

	@Test
	void testRun() throws Exception {
		job.run();

		verify(activityHandler, times(1)).handle(asset, eventType);
		verify(asset, times(1)).getHashPath();
		verify(pendingActivityDao, times(1)).endsActivity(assetHashPath, activityHandler);
		verify(pendingActivityService, times(1)).continueAssetActivity(job);
	}

	@Test
	void testOnJobFail() {
		job.onJobFail(e);
		verify(pendingActivityDao, times(1)).endsActivity(assetHashPath, activityHandler);
		verify(asset, times(1)).getHashPath();
	}

	@Test
	void testGetJobName() {
		assertThat(job.getJobName()).contains(handlerName, assetName);

		verify(activityHandler, times(1)).getHandlerName();
		verify(asset, times(1)).getName();
	}

	@Test
	void testGetJobSpoolname() {
		assertEquals(spoolName, job.getJobSpoolname());
	}

}
