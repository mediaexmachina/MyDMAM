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

import java.util.Optional;

import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusDefinitionWriter;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntry;

public interface MetadataThesaurusService {

    <T> T getReader(Class<T> fromClass, FileEntity fileEntity, int layer);

    /**
     * With layer = 0
     */
    default <T> T getReader(final Class<T> fromClass, final FileEntity fileEntity) {
        return getReader(fromClass, fileEntity, 0);
    }

    Optional<String> getValue(FileEntity fileEntity,
                              int layer,
                              MetadataThesaurusEntry metadataThesaurusEntry);

    /**
     * With layer = 0
     */
    default Optional<String> getValue(final FileEntity fileEntity,
                                      final MetadataThesaurusEntry metadataThesaurusEntry) {
        return getValue(fileEntity, 0, metadataThesaurusEntry);
    }

    <T> MetadataThesaurusDefinitionWriter<T> getWriter(ActivityHandler handler,
                                                       FileEntity fileEntity,
                                                       Class<T> fromClass);

    Optional<String> getMimeType(FileEntity fileEntity);

}
