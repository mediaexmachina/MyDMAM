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
package media.mexm.mydmam.mtdthesaurus;

import java.util.Map;
import java.util.Optional;

import media.mexm.mydmam.entity.FileEntity;

public interface MetadataThesaurusEntryIOProvider {

    Optional<String> getValueFromDatabase(FileEntity fileEntity, String classifier, String key, int layer);

    Map<Integer, String> getValueLayerFromDatabase(FileEntity fileEntity, String classifier, String key);

    void setValueToDatabase(FileEntity fileEntity, String classifier, String key, int layer, String value);

    static MetadataThesaurusEntryIOProvider emptyProvider() {// TODO test
        return new MetadataThesaurusEntryIOProvider() {

            @Override
            public void setValueToDatabase(final FileEntity fileEntity,
                                           final String classifier,
                                           final String key,
                                           final int layer,
                                           final String value) {
                throw new UnsupportedOperationException("Data I/O is disabled");
            }

            @Override
            public Map<Integer, String> getValueLayerFromDatabase(final FileEntity fileEntity,
                                                                  final String classifier,
                                                                  final String key) {
                throw new UnsupportedOperationException("Data I/O is disabled");
            }

            @Override
            public Optional<String> getValueFromDatabase(final FileEntity fileEntity,
                                                         final String classifier,
                                                         final String key,
                                                         final int layer) {
                throw new UnsupportedOperationException("Data I/O is disabled");
            }
        };
    }

}
