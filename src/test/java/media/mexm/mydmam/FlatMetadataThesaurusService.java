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
package media.mexm.mydmam;

import static java.util.Collections.synchronizedSet;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.component.AuditTrail;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.entity.FileMetadataEntity;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusDefinitionWriter;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntry;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefDublinCore;
import media.mexm.mydmam.repository.FileMetadataDao;
import media.mexm.mydmam.service.MetadataThesaurusService;
import media.mexm.mydmam.service.MetadataThesaurusServiceImpl;

@Service
@Primary
public class FlatMetadataThesaurusService implements MetadataThesaurusService {

    private final MetadataThesaurusServiceImpl backend;
    private final FlatFileMetadataDao flatFileMetadataDao;
    private final ConcurrentHashMap<ResponseValueKey, String> responseMap;
    private final Set<FileEntity> relativeTofiles;
    private final Set<FileMetadataEntity> entitiesAdded;
    private final Set<ResponseValueKey> entitiesReaded;
    private final MetadataThesaurusLogic logic;

    private final ConcurrentHashMap<Class<?>, MetadataThesaurusDefinitionWriter<?>> writersByClasses;

    public FlatMetadataThesaurusService() {
        relativeTofiles = synchronizedSet(new HashSet<>());
        entitiesAdded = synchronizedSet(new HashSet<>());
        responseMap = new ConcurrentHashMap<>();
        entitiesReaded = synchronizedSet(new HashSet<>());

        flatFileMetadataDao = new FlatFileMetadataDao();

        final var auditTrail = mock(AuditTrail.class);
        when(auditTrail.getAuditTrailByRealm(anyString())).thenReturn(empty());
        backend = new MetadataThesaurusServiceImpl(flatFileMetadataDao, auditTrail);
        logic = new MetadataThesaurusLogic();
        writersByClasses = new ConcurrentHashMap<>();
    }

    record ResponseValueKey(int layer, String classifier, String key) {
    }

    private class FlatFileMetadataDao implements FileMetadataDao {

        @Override
        public void addUpdateEntry(final FileEntity file, final FileMetadataEntity item) {
            relativeTofiles.add(file);
            entitiesAdded.add(item);
        }

        @Override
        public Optional<String> getMetadataValue(final FileEntity fileEntity,
                                                 final int layer,
                                                 final String classifier,
                                                 final String key) {
            relativeTofiles.add(fileEntity);
            final var responseValueKey = new ResponseValueKey(layer, classifier, key);
            entitiesReaded.add(responseValueKey);
            return Optional.ofNullable(responseMap.get(responseValueKey));
        }

        @Override
        public Map<Integer, String> getMetadataLayersValues(final FileEntity fileEntity, final String classifier, final String key) {
            relativeTofiles.add(fileEntity);
            final var responseValueKey = new ResponseValueKey(-99, classifier, key);
            entitiesReaded.add(responseValueKey);

            final var response = responseMap.get(responseValueKey);
            if (response == null) {
                return Map.of();
            } else {
                return Map.of(-99, response);
            }
        }

        @Override
        public Map<String, Set<FileMetadataEntity>> getFileMetadatasByFileIds(final Collection<Integer> fileIds,
                                                                              final String realm) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addUpdateEntries(final FileEntity file, final Collection<FileMetadataEntity> items) {
            relativeTofiles.add(file);
            entitiesAdded.addAll(items);
        }

    }

    public void endChecks(final FileEntity... files) {
        if (entitiesAdded.isEmpty() == false) {
            assertionFailure()
                    .message("Some (" + entitiesAdded.size() + ") entrie(s) are not checks")
                    .reason("Not checks: " + entitiesAdded)
                    .buildAndThrow();
        }

        if (relativeTofiles.isEmpty() == false) {
            assertThat(relativeTofiles)
                    .withFailMessage("Not expected entities added to file(s): %s", relativeTofiles)
                    .containsExactly(files);
        }

        final var entriesExpectedToRead = responseMap.keySet();
        assertThat(entitiesReaded).containsAll(entriesExpectedToRead);

        relativeTofiles.clear();
        responseMap.clear();
        entitiesReaded.clear();
    }

    public <T> T checkIfAdded(final Class<T> fromClass,
                              final int layer,
                              final Object rawValue) {
        requireNonNull(rawValue);
        if (rawValue instanceof Optional<?>) {
            throw new IllegalArgumentException("Can't check optionals");
        }
        final var value = String.valueOf(rawValue);

        return logic.injectInstanceWriteEntities(
                entry -> {
                    final var classifier = entry.classifier();
                    final var key = entry.key();

                    final var allForThat = entitiesAdded.stream()
                            .filter(entity -> classifier.equals(entity.getClassifier()))
                            .filter(entity -> key.equals(entity.getKey()))
                            .filter(entity -> layer == entity.getLayer())
                            .toList();

                    if (allForThat.isEmpty()) {
                        assertionFailure()
                                .message("Can't found value.")
                                .reason("For: classifier=" + classifier + ", key=" + key + ", layer=" + layer)
                                .buildAndThrow();
                    } else if (allForThat.size() > 1) {
                        assertionFailure()
                                .message("Too more than one value added.")
                                .reason("Found: " + allForThat)
                                .buildAndThrow();
                    } else if (value.equals(allForThat.get(0).getValue()) == false) {
                        assertionFailure()
                                .message("Not expected value, want=" + value + ", have=" + allForThat.get(0).getValue())
                                .reason("For: classifier=" + classifier + ", key=" + key + ", layer=" + layer)
                                .buildAndThrow();
                    }

                    entitiesAdded.remove(allForThat.get(0));
                }, fromClass);
    }

    public <T> T checkIfAdded(final Class<T> fromClass,
                              final Object rawValue) {
        return checkIfAdded(fromClass, 0, rawValue);
    }

    public <T> T addResponse(final Class<T> fromClass,
                             final int layer,
                             final Object rawValue) {
        requireNonNull(rawValue);

        return logic.injectInstanceWriteEntities(
                entry -> {
                    final var rvk = new ResponseValueKey(layer, entry.classifier(), entry.key());
                    if (rawValue instanceof final Optional<?> o) {
                        if (o.isEmpty()) {
                            responseMap.remove(rvk);
                        } else {
                            responseMap.put(rvk, String.valueOf(o.get()));
                        }
                    } else {
                        responseMap.put(rvk, String.valueOf(rawValue));
                    }
                }, fromClass);
    }

    /**
     * With layer = 0
     */
    public <T> T addResponse(final Class<T> fromClass,
                             final Object rawValue) {
        return addResponse(fromClass, 0, rawValue);
    }

    public FlatMetadataThesaurusService setMimeType(final String value) {
        addResponse(MtdThesaurusDefDublinCore.class, value).format();
        return this;
    }

    public void reset() {
        relativeTofiles.clear();
        entitiesAdded.clear();
        responseMap.clear();
        entitiesReaded.clear();
        writersByClasses.clear();
    }

    @Override
    public <T> T getReader(final Class<T> fromClass, final FileEntity fileEntity, final int layer) {
        relativeTofiles.add(fileEntity);
        return backend.getReader(fromClass, fileEntity, layer);
    }

    @Override
    public Optional<String> getValue(final FileEntity fileEntity,
                                     final int layer,
                                     final MetadataThesaurusEntry metadataThesaurusEntry) {
        relativeTofiles.add(fileEntity);
        return backend.getValue(fileEntity, layer, metadataThesaurusEntry);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> MetadataThesaurusDefinitionWriter<T> getWriter(final ActivityHandler handler,
                                                              final FileEntity fileEntity,
                                                              final Class<T> fromClass) {
        relativeTofiles.add(fileEntity);
        return (MetadataThesaurusDefinitionWriter<T>) writersByClasses.computeIfAbsent(fromClass,
                c -> backend.getWriter(handler, fileEntity, c));
    }

    @Override
    public Optional<String> getMimeType(final FileEntity fileEntity) {
        relativeTofiles.add(fileEntity);
        return backend.getMimeType(fileEntity);
    }

    @Override
    public <T> T makeInstance(final Class<T> fromClass) {
        return backend.makeInstance(fromClass);
    }

}
