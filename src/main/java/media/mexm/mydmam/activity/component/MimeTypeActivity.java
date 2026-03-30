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
package media.mexm.mydmam.activity.component;

import static media.mexm.mydmam.audittrail.AuditTrailObjectType.FILE_MIME_TYPE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefFileFormat;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.service.MetadataThesaurusService;

@Component
@Slf4j
public class MimeTypeActivity implements ActivityHandler {

    @Autowired
    MimeTypeDetector mimeTypeDetector;
    @Autowired
    AuditTrail auditTrail;
    @Autowired
    MetadataThesaurusService metadataThesaurusService;

    @Override
    public boolean canHandle(final FileEntity file,
                             final ActivityEventType eventType,
                             final RealmStorageConfiguredEnv storedOn) {
        return storedOn.isDAS();
    }

    @Override
    public void handle(final FileEntity file,
                       final ActivityEventType eventType,
                       final RealmStorageConfiguredEnv storedOn) throws Exception {
        final var internalFile = storedOn.getLocalInternalFile(file);
        log.debug("Get mime type from {}", internalFile);
        final var mimeType = mimeTypeDetector.getMimeType(internalFile);
        log.debug("Founded mime type: {}", mimeType);

        metadataThesaurusService.getWriter(this, file, MtdThesaurusDefFileFormat.class)
                .set(mimeType)
                .mimeType();

        auditTrail.asyncPersistForRealm(
                file.getRealm(),
                "mime-type",
                "direct-extracted-from-file",
                FILE_MIME_TYPE,
                file.getHashPath(),
                mimeType);
    }

}
