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
package media.mexm.mydmam.activity;

public enum ActivityLimitPolicy {

    DISABLED,
    /**
     * Only mime type extraction
     */
    TYPE_EXTRACTION,
    /**
     * Extract some file information. This shouldn't take any time.
     */
    FILE_INFORMATION,
    /**
     * Make rendered files. This shouldn't take too much time, like images extraction.
     */
    BASE_PREVIEW,
    /**
     * Make rendered files, but this will take time and resources, like video or MD5 extraction.
     */
    FULL_PREVIEW,
    /**
     * Applies time and resource intensive process.
     */
    DEEP_ANALYSIS;

    public boolean isLevelLowerThan(final ActivityLimitPolicy compareTo) {
        if (equals(DISABLED)) {
            return false;
        }
        return compareTo(compareTo) < 0;
    }

    public int getJobPriority() {
        if (equals(DISABLED)) {
            return -1;
        }
        return values().length - ordinal() - 1;
    }

}
