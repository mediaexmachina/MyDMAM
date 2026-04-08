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
package media.mexm.mydmam.service;

import static jakarta.transaction.Transactional.TxType.REQUIRES_NEW;
import static java.lang.Integer.toHexString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static java.util.stream.Stream.concat;
import static media.mexm.mydmam.audittrail.AuditTrailObjectType.RENDERED_FILE_ENTRY;
import static media.mexm.mydmam.entity.FileEntity.hashPath;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.moveFile;
import static org.apache.commons.lang3.StringUtils.leftPad;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.asset.DeclaredRenderedFile;
import media.mexm.mydmam.audittrail.AuditTrailBatchInsertObject;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.component.Indexer;
import media.mexm.mydmam.component.MimeTypeDetector;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.AssetTextExtractedFileEntity;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.RelativePathProvider;
import media.mexm.mydmam.repository.AssetRenderedFileDao;
import media.mexm.mydmam.repository.AssetRenderedFileRepository;
import media.mexm.mydmam.repository.AssetTextExtractedFileDao;
import media.mexm.mydmam.repository.AssetTextExtractedFileRepository;
import media.mexm.mydmam.repository.FileMetadataDao;
import media.mexm.mydmam.repository.FileMetadataRepository;
import media.mexm.mydmam.repository.FileRepository;
import tv.hd3g.transfertfiles.FileAttributesReference;

@Slf4j
@Service
public class MediaAssetServiceImpl implements MediaAssetService {

    @Autowired
    MyDMAMConfigurationProperties configuration;
    @Autowired
    FileRepository fileRepository;
    @Autowired
    AssetRenderedFileDao assetRenderedFileDao;
    @Autowired
    AssetRenderedFileRepository assetRenderedFileRepository;
    @Autowired
    FileMetadataRepository fileMetadataRepository;
    @Autowired
    FileMetadataDao fileMetadataDao;
    @Autowired
    AssetTextExtractedFileRepository assetTextExtractedFileRepository;
    @Autowired
    AssetTextExtractedFileDao assetTextExtractedFileDao;
    @Autowired
    Indexer indexer;
    @Autowired
    AuditTrail auditTrail;
    @Autowired
    MimeTypeDetector mimeTypeDetector;

    @Override
    public FileEntity getFromWatchfolder(final String realmName,
                                         final String storageName,
                                         final FileAttributesReference file) {
        return fileRepository.getByHashPath(hashPath(realmName, storageName, file.getPath()), realmName);
    }

    @Override
    public void purgeAssetArtefacts(final String realmName,
                                    final String storageName,
                                    final FileAttributesReference file) {
        /**
         * TO BE IMPLEMENTED
         * with auditTrail
         */
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public void declareRenderedStaticFile(final FileEntity fileEntity,
                                          final DeclaredRenderedFile declaredRenderedFile) throws IOException {
        final var realmName = fileEntity.getRealm();
        final var toCreate = new AssetRenderedFileEntity(fileEntity, declaredRenderedFile);
        assetRenderedFileRepository.saveAndFlush(toCreate);

        final var created = assetRenderedFileRepository.getRenderedForFileByEtag(
                fileEntity.getId(), toCreate.getEtag());

        final var renderedFile = getAbsolutePath(fileEntity, created);
        if (renderedFile.exists()) {
            throw new IOException("Can't move move rendered file " + created + " to " + renderedFile
                                  + ", file exists");
        }

        log.debug("Start to move rendered file from \"{}\" to \"{}\"",
                declaredRenderedFile.workingFile(), renderedFile);
        moveFile(declaredRenderedFile.workingFile(), renderedFile);

        auditTrail.getAuditTrailByRealm(realmName)
                .ifPresent(realmAuditTrail -> realmAuditTrail.asyncPersist(MEDIA_ASSET_AUDIT_ISSUER,
                        "save-rendered-file",
                        new AuditTrailBatchInsertObject(RENDERED_FILE_ENTRY, fileEntity.getHashPath(),
                                List.of(created.getAuditTrailPayload(renderedFile)))));
    }

    @Override
    @Transactional
    public void updateIndexer(final FileEntity fileEntity) {
        final var realm = fileEntity.getRealm();
        indexer.getIndexerByRealm(realm)
                .ifPresent(idx -> {
                    log.info("Update indexer for {}", fileEntity);
                    final var fileMetadataEntites = fileMetadataRepository.getByFile(fileEntity);
                    final var texts = new ArrayList<String>();
                    forEachTextExtractedFile(fileEntity, (_, text) -> texts.add(text));
                    idx.updateAsset(fileEntity, fileMetadataEntites, texts);
                });
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public void declareTextExtractedFile(final FileEntity fileEntity,
                                         final File workingTextFile,
                                         final String name) throws IOException {
        final var realmName = fileEntity.getRealm();
        final var validatedWorkingFile = new File(workingTextFile.getParentFile(), workingTextFile.getName() + ".gz");
        try (final var fso = new GZIPOutputStream(new FileOutputStream(validatedWorkingFile))) {
            log.info("Gzip text extracted file from \"{}\" to \"{}\"", workingTextFile, validatedWorkingFile);
            copyFile(workingTextFile, fso);
        }
        forceDelete(workingTextFile);

        final var toCreate = new AssetTextExtractedFileEntity(fileEntity, name, validatedWorkingFile.length());
        assetTextExtractedFileRepository.saveAndFlush(toCreate);
        final var created = assetTextExtractedFileRepository.getTextExtractedByName(fileEntity.getId(), name);

        final var renderedFile = getAbsolutePath(fileEntity, created);
        if (renderedFile.exists()) {
            throw new IOException("Can't move move rendered file " + created + " to " + renderedFile
                                  + ", file exists");
        }

        log.debug("Start to move text extracted file from \"{}\" to \"{}\"", validatedWorkingFile, renderedFile);
        moveFile(validatedWorkingFile, renderedFile);

        auditTrail.getAuditTrailByRealm(realmName)
                .ifPresent(realmAuditTrail -> realmAuditTrail.asyncPersist(MEDIA_ASSET_AUDIT_ISSUER,
                        "save-text-extracted-file",
                        new AuditTrailBatchInsertObject(RENDERED_FILE_ENTRY, fileEntity.getHashPath(),
                                List.of(created.getAuditTrailPayload(renderedFile)))));
    }

    @Override
    @Transactional
    public void forEachTextExtractedFile(final FileEntity fileEntity,
                                         final BiConsumer<AssetTextExtractedFileEntity, String> onTextExtracted) {
        assetTextExtractedFileRepository.getAllTextExtracted(fileEntity)
                .forEach(entity -> {
                    try {
                        final var renderedFile = getAbsolutePath(fileEntity, entity);
                        log.info("Start to read gzip text file \"{}\" to import text ({} bytes)",
                                renderedFile, renderedFile.length());
                        try (final var isr = new InputStreamReader(
                                new GZIPInputStream(new FileInputStream(renderedFile)), UTF_8)) {
                            onTextExtracted.accept(entity, isr.readAllAsString());
                        }
                    } catch (final IOException e) {
                        throw new UncheckedIOException("Can't extract text extracted file from " + entity, e);
                    }
                });
    }

    @Override
    @Transactional
    public String getRelativePath(final FileEntity fileEntity, final RelativePathProvider relativePathProvider) {
        final var hex = leftPad(toHexString(fileEntity.getId()).toUpperCase(), 8, "00000000");

        final var sb = new StringBuilder();
        sb.append("/");
        sb.append(hex.substring(0, 4));
        sb.append("/");
        sb.append(hex.substring(4));
        sb.append("/");
        sb.append(relativePathProvider.getId());
        sb.append(".");
        sb.append(relativePathProvider.getIndexref());
        sb.append(".");
        sb.append(relativePathProvider.getName());
        if (relativePathProvider.isGzipEncoded()) {
            sb.append(".gz");
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public File getAbsolutePath(final FileEntity fileEntity, final RelativePathProvider relativePathProvider) {
        final var realmName = fileEntity.getRealm();
        final var renderedMetadataDirectory = Objects.requireNonNull(configuration.getRealmByName(realmName)
                .orElseThrow()
                .renderedMetadataDirectory());
        try {
            return new File(renderedMetadataDirectory, getRelativePath(fileEntity, relativePathProvider))
                    .getAbsoluteFile()
                    .getCanonicalFile();
        } catch (final IOException e) {
            throw new UncheckedIOException("Can't access to " + renderedMetadataDirectory, e);
        }
    }

    @Override
    public void declareRenderedStaticFile(final FileEntity fileEntity,
                                          final File workingFile,
                                          final String name,
                                          final boolean toGzip,
                                          final int index,
                                          final String previewType) throws IOException {
        declareRenderedStaticFile(fileEntity,
                new DeclaredRenderedFile(workingFile, name, toGzip, mimeTypeDetector, index, previewType));
    }

    @Override
    public File getPhysicalRenderedFile(final FileEntity fileEntity,
                                        final AssetRenderedFileEntity assetRenderedFileEntity,
                                        final String realm) {
        try {
            final var renderedFile = getAbsolutePath(fileEntity, assetRenderedFileEntity);

            if (renderedFile.exists() == false) {
                throw new FileNotFoundException(renderedFile.getPath());
            } else if (renderedFile.length() != assetRenderedFileEntity.getLength()) {
                throw new IOException("Invalid real file size: "
                                      + renderedFile.length() + " bytes on \"" + renderedFile + "\"");
            }
            return renderedFile;
        } catch (final IOException e) {
            throw new UncheckedIOException("Can't access to a valid rendered file: " + assetRenderedFileEntity, e);
        }
    }

    @Override
    @Transactional(REQUIRES_NEW)
    public Collection<FileEntity> resetDetectedMetadatas(final Collection<FileEntity> assetsToReset) {
        if (assetsToReset.isEmpty()) {
            return List.of();
        }
        log.debug("Reset detected metadatas on {}", assetsToReset);

        final var fileIdsToReset = assetsToReset.stream()
                .map(FileEntity::getId)
                .distinct()
                .collect(toUnmodifiableSet());

        fileMetadataRepository.deleteByFileId(fileIdsToReset);

        final var renderedFilesToDelete = assetRenderedFileDao.deleteRenderedFilesByFileId(fileIdsToReset);
        final var tExtractedFilesToDelete = assetTextExtractedFileDao.deleteTextExtractedFilesByFileId(fileIdsToReset);

        final var deleteFilesList = concat(
                tExtractedFilesToDelete.keySet().stream(),
                renderedFilesToDelete.keySet().stream())
                        .distinct()
                        .filter(realmName -> configuration.getRealmByName(realmName).isPresent())
                        .flatMap(realmName -> {
                            final var renderedFilesPaths = Optional.ofNullable(renderedFilesToDelete
                                    .get(realmName))
                                    .stream()
                                    .flatMap(Set::stream)
                                    .map(rf -> getAbsolutePath(rf.getFile(), rf));

                            final var tExtractedFilesPaths = Optional.ofNullable(tExtractedFilesToDelete
                                    .get(realmName))
                                    .stream()
                                    .flatMap(Set::stream)
                                    .map(rf -> getAbsolutePath(rf.getFile(), rf));

                            return concat(renderedFilesPaths, tExtractedFilesPaths);
                        })
                        .toList();

        if (deleteFilesList.isEmpty() == false && log.isInfoEnabled()) {
            deleteFilesList.forEach(file -> log.info("Delete rendered file on reset detected metadatas: {}", file));
        }

        final var canDeleteFilesList = deleteFilesList.stream()
                .filter(not(File::delete))
                .toList();
        if (canDeleteFilesList.isEmpty() == false) {
            log.warn("Can't delete rendered file(s) on reset detected metadatas: {}", canDeleteFilesList);
        }

        final var updatedFiles = fileRepository.getByIds(fileIdsToReset);
        updatedFiles.stream()
                .forEach(updated -> indexer.getIndexerByRealm(updated.getRealm())
                        .ifPresent(idx -> idx.updateAsset(updated, List.of(), List.of())));
        return updatedFiles;
    }

}
