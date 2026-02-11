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

import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.indexer.RealmIndexer;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.repository.PendingActivityDao;
import media.mexm.mydmam.service.PendingActivityService;
import tv.hd3g.jobkit.engine.Job;

@Slf4j
public record PendingActivityJob(RealmStorageConfiguredEnv configuredEnv,
								 MediaAsset asset,
								 ActivityHandler activityHandler,
								 ActivityEventType eventType,
								 Set<String> previousHandlers,
								 String previousHandlersJson,
								 PendingActivityDao pendingActivityDao,
								 PendingActivityService pendingActivityService,
								 Optional<RealmIndexer> oIndexer) implements Job {

	public PendingActivityJob evolve(final ActivityHandler activityHandler,
									 final Set<String> previousHandlers,
									 final String previousHandlersJson) {
		return new PendingActivityJob(
				configuredEnv,
				asset,
				activityHandler,
				eventType,
				previousHandlers,
				previousHandlersJson,
				pendingActivityDao,
				pendingActivityService,
				oIndexer);
	}

	@Override
	public void run() throws Exception {
		if (pendingActivityDao.haveDeclaredActivity(asset.getFile(), activityHandler) == false) {
			log.info("Cancel media asset activity {} on: {} as {}",
					activityHandler.getHandlerName(),
					asset,
					eventType);

			pendingActivityService.continueAssetActivity(this);
			return;
		}

		log.info("Start media asset activity {} on: {}, as {}",
				activityHandler.getHandlerName(),
				asset,
				eventType);

		/* FOR FUTURE NEEDS final var result = */

		activityHandler.handle(asset, eventType, configuredEnv);
		asset.commit(oIndexer);

		pendingActivityDao.endsActivity(asset.getFile(), activityHandler);

		log.debug("Ends media asset activity {} on: {}, as {}",
				activityHandler.getHandlerName(),
				asset,
				eventType);

		pendingActivityService.continueAssetActivity(this);
	}

	@Override
	public void onJobFail(final Exception e) {
		pendingActivityDao.endsActivity(asset.getFile(), activityHandler);

		log.error("Can't run media asset activity on: {}", asset, e);
	}

	@Override
	public String getJobName() {
		return "Run media asset activity " + activityHandler.getHandlerName() + " on file: " + asset.getName();
	}

	@Override
	public String getJobSpoolname() {
		return configuredEnv.realm().spoolProcessAsset();
	}

}
