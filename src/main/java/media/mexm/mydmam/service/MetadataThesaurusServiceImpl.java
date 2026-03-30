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

import static media.mexm.mydmam.audittrail.AuditTrailObjectType.FILE_METADATA_ENTRY;
import static media.mexm.mydmam.service.MediaAssetService.MEDIA_ASSET_AUDIT_ISSUER;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.asset.MetadataExtractorHandler;
import media.mexm.mydmam.audittrail.AuditTrailBatchInsertObject;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusDefinitionWriter;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntry;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefFileFormat;
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

    @Override
    public <T> T getReader(final Class<T> fromClass, final FileEntity fileEntity, final int layer) {
        return logic.injectInstanceReadEntities(
                metadataThesaurusEntry -> getValue(fileEntity, layer, metadataThesaurusEntry),
                fromClass);
    }

    @Override
    public Optional<String> getValue(final FileEntity fileEntity,
                                     final int layer,
                                     final MetadataThesaurusEntry metadataThesaurusEntry) {
        return fileMetadataDao.getMetadataValue(
                fileEntity,
                layer,
                metadataThesaurusEntry.classifier(),
                metadataThesaurusEntry.key());
    }

    @Override
    public <T> MetadataThesaurusDefinitionWriter<T> getWriter(final ActivityHandler handler,
                                                              final FileEntity fileEntity,
                                                              final Class<T> fromClass) {
        final String origin;
        if (handler instanceof final MetadataExtractorHandler mtdHander) {
            origin = mtdHander.getMetadataOriginName();
        } else {
            origin = handler.getHandlerName();
        }

        final var def = new MetadataThesaurusDefinitionWriter<T>();
        final var instance = logic.injectInstanceWriteEntities(entry -> {
            final var writed = def.get();
            if (writed.isEmpty()) {
                return;
            }
            final var dbEntry = new FileMetadataEntity(
                    fileEntity,
                    origin,
                    entry,
                    writed.get().layer(),
                    writed.get().value());

            log.debug("Save FileMetadata {}", dbEntry);
            fileMetadataDao.addUpdateEntry(fileEntity, dbEntry);
            updateAuditTrail(fileEntity, dbEntry);
        }, fromClass);
        def.setInstance(instance);
        return def;
    }

    private void updateAuditTrail(final FileEntity fileEntity, final FileMetadataEntity insert) {
        auditTrail.getAuditTrailByRealm(fileEntity.getRealm())
                .ifPresent(realmAuditTrail -> realmAuditTrail.asyncPersist(MEDIA_ASSET_AUDIT_ISSUER,
                        "extracted-file-metadatas",
                        new AuditTrailBatchInsertObject(FILE_METADATA_ENTRY, fileEntity.getHashPath(),
                                List.of(insert.getAuditTrailPayload()))));
    }

    @Override
    public Optional<String> getMimeType(final FileEntity fileEntity) {
        return getReader(MtdThesaurusDefFileFormat.class, fileEntity, 0).mimeType().value();
    }

}
