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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.PendingActivityJob;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.InstanceEntity;
import media.mexm.mydmam.entity.PendingActivityEntity;

public interface PendingActivityDao {

    void declateActivities(List<PendingActivityJob> allActivitiesJobs, Optional<InstanceEntity> oInstance);

    void endsActivity(FileEntity file, ActivityHandler activityHandler);

    boolean haveDeclaredActivity(FileEntity file, ActivityHandler activityHandler);

    boolean havePendingActivities(FileEntity file);

    Map<FileEntity, Set<PendingActivityEntity>> getFilesAndPendingActivityByFileId(Collection<Integer> ids);

    List<Integer> getFilesAndWithResetPendingActivities(Set<String> realms);

}
