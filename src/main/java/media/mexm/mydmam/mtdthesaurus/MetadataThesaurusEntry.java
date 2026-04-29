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

/**
 * @see MetadataThesaurusLogic#onEntryCall for usage
 */
public interface MetadataThesaurusEntry {

    String classifier();

    String key();

    void set(Object value);

    void set(int layer, Object value);

    /**
     * such as '2011-12-03T10:15:30Z'
     */
    void setDateISO8601(Optional<String> oValue);

    /**
     * such as '2011-12-03T10:15:30Z'
     */
    void setDateISO8601(int layer, Optional<String> oValue);

    Optional<String> get();

    int getAsInt(int defaultValue);

    int getAsInt(int layer, int defaultValue);

    Optional<String> get(int layer);

    Map<Integer, String> getAll();

    Map<Integer, Integer> getAllInt();

}
