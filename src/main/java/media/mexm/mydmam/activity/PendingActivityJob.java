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

import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.repository.PendingActivityDao;
import media.mexm.mydmam.service.PendingActivityService;
import tv.hd3g.jobkit.engine.Job;

@Slf4j
public record PendingActivityJob(String realmName,
								 String storageName,
								 String spoolName,
								 MediaAsset asset,
								 ActivityHander activityHandler,
								 ActivityEventType eventType,
								 Set<Class<?>> previousHandlers,
								 PendingActivityDao pendingActivityDao,
								 PendingActivityService pendingActivityService) implements Job {

	@Override
	public void run() throws Exception {
		log.debug("Start media asset activity on file: \"{}\" ({}:{})",
				asset.getName(), realmName, storageName);
		pendingActivityDao.declateActivities(
				Set.of(asset.getHashPath()), activityHandler.getTaskContextName(), "starts");
		activityHandler.handle(asset, eventType);
	}

	@Override
	public void onJobDone() {
		pendingActivityDao.endsActivity(asset.getHashPath(), activityHandler.getTaskContextName());

		log.trace("Ends media asset activity on file: \"{}\" ({}:{})",
				asset.getName(), realmName, storageName);
		pendingActivityService.dispatchAssetActivities(
				realmName, storageName, asset, spoolName, eventType, previousHandlers);
	}

	@Override
	public void onJobFail(final Exception e) {
		pendingActivityDao.endsActivity(asset.getHashPath(), activityHandler.getTaskContextName());

		log.error("Can't run media asset activity on file: \"{}\" ({}:{})",
				asset.getName(), realmName, storageName, e);
	}

	@Override
	public String getJobName() {
		return "Run media asset activity " + activityHandler.getTaskContextName() + " on file: " + asset.getName();
	}

	@Override
	public String getJobSpoolname() {
		return spoolName;
	}

}
