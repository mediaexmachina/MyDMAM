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

import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryIOProvider;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusLogic;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusRegister;
import media.mexm.mydmam.service.MetadataThesaurusService;

@Service
@Primary
public class FlatMetadataThesaurusService implements MetadataThesaurusService {

    private final MetadataThesaurusLogic logic;
    private final Set<FileEntity> usedFileEntities;
    private final Set<ActivityHandler> usedActivityHandler;
    private final IOProvider inOutProvider;
    private final IOProvider outProvider;
    private final IOProvider testProvider;
    private final Set<ThesaurusDbEntry> savedForTest;
    private final Set<ThesaurusDbEntry> addedDuringTest;

    private String actualMimeType;

    public FlatMetadataThesaurusService() {
        logic = new MetadataThesaurusLogic();
        usedFileEntities = new HashSet<>();
        usedActivityHandler = new HashSet<>();
        savedForTest = new HashSet<>();
        addedDuringTest = new HashSet<>();
        actualMimeType = null;
        inOutProvider = new IOProvider(true, savedForTest, addedDuringTest);
        outProvider = new IOProvider(false, savedForTest, addedDuringTest);
        testProvider = new IOProvider(true, addedDuringTest, savedForTest);
    }

    public void reset() {
        usedFileEntities.clear();
        usedActivityHandler.clear();
        savedForTest.clear();
        addedDuringTest.clear();
        actualMimeType = null;
    }

    public FlatMetadataThesaurusService check(final FileEntity fileEntity) {
        assertThat(usedFileEntities.remove(fileEntity))
                .withFailMessage("Can't found to fileEntity: %s", fileEntity)
                .isTrue();
        return this;
    }

    public FlatMetadataThesaurusService check(final ActivityHandler activityHandler) {
        assertThat(usedActivityHandler.remove(activityHandler))
                .withFailMessage("Can't found to activityHandler: %s", activityHandler)
                .isTrue();
        return this;
    }

    public void check() {
        assertThat(usedFileEntities)
                .withFailMessage("Not empty usedFileEntities: %s", usedFileEntities)
                .isEmpty();
        assertThat(usedActivityHandler)
                .withFailMessage("Not empty usedActivityHandler: %s", usedActivityHandler)
                .isEmpty();
        assertThat(addedDuringTest)
                .withFailMessage("Not empty addedDuringTest (please check added): %s", addedDuringTest)
                .isEmpty();
    }

    private record ThesaurusDbEntry(String classifier, String key, int layer, String value) {
    }

    private static class IOProvider implements MetadataThesaurusEntryIOProvider {

        private final boolean canWrite;
        private final Set<ThesaurusDbEntry> readFrom;
        private final Set<ThesaurusDbEntry> writeTo;

        IOProvider(final boolean canWrite, final Set<ThesaurusDbEntry> readFrom, final Set<ThesaurusDbEntry> writeTo) {
            this.canWrite = canWrite;
            this.readFrom = readFrom;
            this.writeTo = writeTo;
        }

        @Override
        public Optional<String> getValueFromDatabase(final String classifier, final String key, final int layer) {
            final var oItem = readFrom.stream()
                    .filter(entry -> entry.classifier.equals(classifier))
                    .filter(entry -> entry.key.equals(key))
                    .filter(entry -> entry.layer == layer)
                    .findFirst();
            oItem.ifPresent(readFrom::remove);
            return oItem.map(ThesaurusDbEntry::value);
        }

        @Override
        public Map<Integer, String> getValueLayerFromDatabase(final String classifier, final String key) {
            final var items = readFrom.stream()
                    .filter(entry -> entry.classifier.equals(classifier))
                    .filter(entry -> entry.key.equals(key))
                    .toList();
            items.forEach(readFrom::remove);
            return items.stream().collect(toUnmodifiableMap(ThesaurusDbEntry::layer, ThesaurusDbEntry::value));
        }

        @Override
        public void setValueToDatabase(final String classifier, final String key, final int layer, final String value) {
            if (canWrite == false) {
                throw new UnsupportedOperationException("Can't write with this Provider/Logic");
            }
            writeTo.add(new ThesaurusDbEntry(classifier, key, layer, value));
        }
    }

    @Override
    public MetadataThesaurusRegister getThesaurus(final ActivityHandler handler, final FileEntity fileEntity) {
        usedFileEntities.add(fileEntity);
        usedActivityHandler.add(handler);
        return logic.makeRegister(inOutProvider);
    }

    @Override
    public MetadataThesaurusRegister getReadOnlyThesaurus(final FileEntity fileEntity) {
        usedFileEntities.add(fileEntity);
        return logic.makeRegister(outProvider);
    }

    public MetadataThesaurusRegister getTestThesaurus() {
        return logic.makeRegister(testProvider);
    }

    @Override
    public Optional<String> getMimeType(final FileEntity fileEntity) {
        usedFileEntities.add(fileEntity);
        return Optional.ofNullable(actualMimeType);
    }

    @Override
    public void setMimeType(final ActivityHandler handler, final FileEntity fileEntity, final String mimeType) {
        usedFileEntities.add(fileEntity);
        usedActivityHandler.add(handler);
        actualMimeType = mimeType;
    }

}
