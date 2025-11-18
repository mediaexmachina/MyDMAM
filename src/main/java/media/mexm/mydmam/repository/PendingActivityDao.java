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
package media.mexm.mydmam.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.entity.PendingActivityEntity;

public interface PendingActivityDao {

	void declateActivity(String hashPathItem,
						 ActivityHandler activityHandler,
						 ActivityEventType eventType,
						 String previousHandlers,
						 String hostName,
						 long pid);

	void endsActivity(String hashPath, ActivityHandler activityHandler);

	List<PendingActivityEntity> getPendingActivities(Set<String> realms, String hostName);

	void resetPendingActivity(PendingActivityEntity pendingActivity, String hostName, long pid);

	void deletePendingActivities(Collection<PendingActivityEntity> pendingActivities);

}
