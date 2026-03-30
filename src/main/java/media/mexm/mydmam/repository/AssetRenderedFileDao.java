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
package media.mexm.mydmam.repository;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import media.mexm.mydmam.entity.AssetRenderedFileEntity;
import media.mexm.mydmam.entity.RelativePathProvider;

public interface AssetRenderedFileDao {

    Map<String, Set<AssetRenderedFileEntity>> getRenderedFilesByFileId(final Collection<Integer> ids,
                                                                       final String realm);

    /**
     * @return Map of AssetRenderedFileEntity to delete by realm name
     */
    Map<String, Set<RelativePathProvider>> deleteRenderedFilesByFileId(Collection<Integer> ids);

}
