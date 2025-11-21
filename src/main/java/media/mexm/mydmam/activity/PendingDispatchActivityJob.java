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

import static java.util.Collections.synchronizedSet;

import java.util.HashSet;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.asset.MediaAsset;
import media.mexm.mydmam.repository.PendingActivityDao;
import media.mexm.mydmam.service.PendingActivityService;
import tv.hd3g.jobkit.engine.Job;

@Slf4j
public record PendingDispatchActivityJob(String realmName,
										 String storageName,
										 String spoolName,
										 MediaAsset asset,
										 ActivityEventType eventType,
										 PendingActivityDao pendingActivityDao,
										 PendingActivityService pendingActivityService) implements Job {

	@Override
	public void run() throws Exception {
		log.debug("Start to dispatch founded file: \"{}\" ({}:{})", asset.getName(), realmName, storageName);
		pendingActivityService.dispatchAssetActivities(
				realmName,
				storageName,
				asset, spoolName,
				eventType,
				synchronizedSet(new HashSet<>()));
	}

	@Override
	public void onJobDone() {
		log.trace("End dispatch founded file: \"{}\" ({}:{})",
				asset.getName(), realmName, storageName);
		pendingActivityDao.endsActivity(asset.getHashPath(), eventType.toString());
	}

	@Override
	public void onJobFail(final Exception e) {
		log.error("Can't dispatch founded file: \"{}\" ({}:{})",
				asset.getName(), realmName, storageName, e);
		pendingActivityDao.endsActivity(asset.getHashPath(), eventType.toString());
	}

	@Override
	public String getJobName() {
		return "Dispatch founded file: " + asset.getName();
	}

	@Override
	public String getJobSpoolname() {
		return spoolName;
	}

}
