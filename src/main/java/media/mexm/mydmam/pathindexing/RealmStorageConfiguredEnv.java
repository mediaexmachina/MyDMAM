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
package media.mexm.mydmam.pathindexing;

import static media.mexm.mydmam.activity.ActivityLimitPolicy.BASE_PREVIEW;
import static media.mexm.mydmam.dto.StorageCategory.DAS;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;

import media.mexm.mydmam.activity.ActivityLimitPolicy;
import media.mexm.mydmam.configuration.PathIndexingStorage;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.entity.FileEntity;
import tv.hd3g.transfertfiles.AbstractFileSystemURL;
import tv.hd3g.transfertfiles.local.LocalFile;

public record RealmStorageConfiguredEnv(String realmName,
                                        String storageName,
                                        RealmConf realm,
                                        PathIndexingStorage storage) {

    public ActivityLimitPolicy getActivityLimitPolicy() {
        return Optional.ofNullable(storage.activityLimit())
                .or(() -> Optional.ofNullable(realm.activityLimit()))
                .orElse(BASE_PREVIEW);
    }

    public boolean isDAS() {
        return DAS.equals(storage.getCategory());
    }

    public boolean haveWorkingDir() {
        return realm.workingDirectory() != null;
    }

    public boolean haveRenderedDir() {
        return realm.renderedMetadataDirectory() != null;
    }

    public File makeWorkingFile(final String fileName, final FileEntity file) {
        return realm.makeWorkingFile(file.getId() + "-" + fileName);
    }

    /**
     * Please do checks before call: StorageCategory must be DAS.
     * @return only local file, else throw UnsupportedOperationException
     */
    public File getLocalInternalFile(final FileEntity fileEntity) {
        try (final var fileSystem = new AbstractFileSystemURL(storage.path())) {
            final var aFile = fileSystem.getFromPath(fileEntity.getPath());

            if (aFile instanceof final LocalFile localFile) {
                return localFile.getInternalFile();
            } else {
                throw new UnsupportedOperationException("Can't manage non-local files from " + fileSystem.getClass());
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("Can't access to file system", e);
        }
    }
}
