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
package media.mexm.mydmam.service;

import static java.util.Optional.empty;
import static media.mexm.mydmam.audittrail.AuditTrailObjectType.FILE_METADATA_ENTRY;
import static media.mexm.mydmam.service.MediaAssetService.MEDIA_ASSET_AUDIT_ISSUER;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.audittrail.AuditTrailBatchInsertObject;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryIOProvider;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusRegister;
import media.mexm.mydmam.repository.FileMetadataDao;

@Service
@Slf4j
public class MetadataThesaurusServiceImpl implements MetadataThesaurusService {

    private final FileMetadataDao fileMetadataDao;
    private final MetadataThesaurusLogic logic;
    private final AuditTrail auditTrail;

    public MetadataThesaurusServiceImpl(@Autowired final FileMetadataDao fileMetadataDao,
                                        @Autowired final AuditTrail auditTrail) {
        this.fileMetadataDao = fileMetadataDao;
        this.auditTrail = auditTrail;
        logic = new MetadataThesaurusLogic();
    }

    record IOProvider(FileMetadataDao fileMetadataDao,
                      Optional<String> origin,
                      FileEntity fileEntity,
                      AuditTrail auditTrail) implements MetadataThesaurusEntryIOProvider {

        @Override
        public Optional<String> getValueFromDatabase(final String classifier, final String key, final int layer) {
            return fileMetadataDao.getMetadataValue(fileEntity, layer, classifier, key);
        }

        @Override
        public Map<Integer, String> getValueLayerFromDatabase(final String classifier, final String key) {
            return fileMetadataDao.getMetadataLayersValues(fileEntity, classifier, key);
        }

        @Override
        public void setValueToDatabase(final String classifier, final String key, final int layer, final String value) {
            final var insert = new FileMetadataEntity(
                    fileEntity,
                    origin.orElseThrow(() -> new IllegalAccessError("Can't use this provider to write datas")),
                    classifier,
                    key,
                    layer,
                    value);

            fileMetadataDao.addUpdateEntry(fileEntity, insert);
            auditTrail.getAuditTrailByRealm(fileEntity.getRealm())
                    .ifPresent(realmAuditTrail -> realmAuditTrail.asyncPersist(MEDIA_ASSET_AUDIT_ISSUER,
                            "extracted-file-metadatas",
                            new AuditTrailBatchInsertObject(FILE_METADATA_ENTRY, fileEntity.getHashPath(),
                                    List.of(insert.getAuditTrailPayload()))));
        }

    }

    @Override
    public MetadataThesaurusRegister getThesaurus(final ActivityHandler handler, final FileEntity fileEntity) {
        return logic.makeRegister(new IOProvider(
                fileMetadataDao,
                Optional.ofNullable(handler.getMetadataOriginName()),
                fileEntity,
                auditTrail));
    }

    @Override
    public MetadataThesaurusRegister getReadOnlyThesaurus(final FileEntity fileEntity) {
        return logic.makeRegister(new IOProvider(fileMetadataDao, empty(), fileEntity, auditTrail));
    }

    @Override
    public Optional<String> getMimeType(final FileEntity fileEntity) {
        return getReadOnlyThesaurus(fileEntity).dublinCore().format().get();
    }

    @Override
    public void setMimeType(final ActivityHandler handler, final FileEntity fileEntity, final String mimeType) {
        getThesaurus(handler, fileEntity).dublinCore().format().set(mimeType);
    }
}
