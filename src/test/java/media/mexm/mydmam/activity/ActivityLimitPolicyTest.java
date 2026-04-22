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

import static media.mexm.mydmam.activity.ActivityLimitPolicy.BASE_PREVIEW;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.DEEP_ANALYSIS;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.DISABLED;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FULL_PREVIEW;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.TYPE_EXTRACTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ActivityLimitPolicyTest {

    @Test
    void testIsLevelEqualsOrLowerThan() {// NOSONAR 5961
        var level = DISABLED;
        assertFalse(DISABLED.isLevelLowerThan(level));
        assertFalse(TYPE_EXTRACTION.isLevelLowerThan(level));
        assertFalse(FILE_INFORMATION.isLevelLowerThan(level));
        assertFalse(BASE_PREVIEW.isLevelLowerThan(level));
        assertFalse(FULL_PREVIEW.isLevelLowerThan(level));
        assertFalse(DEEP_ANALYSIS.isLevelLowerThan(level));

        level = TYPE_EXTRACTION;
        assertFalse(DISABLED.isLevelLowerThan(level));
        assertFalse(TYPE_EXTRACTION.isLevelLowerThan(level));
        assertFalse(FILE_INFORMATION.isLevelLowerThan(level));
        assertFalse(BASE_PREVIEW.isLevelLowerThan(level));
        assertFalse(FULL_PREVIEW.isLevelLowerThan(level));
        assertFalse(DEEP_ANALYSIS.isLevelLowerThan(level));

        level = FILE_INFORMATION;
        assertFalse(DISABLED.isLevelLowerThan(level));
        assertTrue(TYPE_EXTRACTION.isLevelLowerThan(level));
        assertFalse(FILE_INFORMATION.isLevelLowerThan(level));
        assertFalse(BASE_PREVIEW.isLevelLowerThan(level));
        assertFalse(FULL_PREVIEW.isLevelLowerThan(level));
        assertFalse(DEEP_ANALYSIS.isLevelLowerThan(level));

        level = BASE_PREVIEW;
        assertFalse(DISABLED.isLevelLowerThan(level));
        assertTrue(TYPE_EXTRACTION.isLevelLowerThan(level));
        assertTrue(FILE_INFORMATION.isLevelLowerThan(level));
        assertFalse(BASE_PREVIEW.isLevelLowerThan(level));
        assertFalse(FULL_PREVIEW.isLevelLowerThan(level));
        assertFalse(DEEP_ANALYSIS.isLevelLowerThan(level));

        level = FULL_PREVIEW;
        assertFalse(DISABLED.isLevelLowerThan(level));
        assertTrue(TYPE_EXTRACTION.isLevelLowerThan(level));
        assertTrue(FILE_INFORMATION.isLevelLowerThan(level));
        assertTrue(BASE_PREVIEW.isLevelLowerThan(level));
        assertFalse(FULL_PREVIEW.isLevelLowerThan(level));
        assertFalse(DEEP_ANALYSIS.isLevelLowerThan(level));

        level = DEEP_ANALYSIS;
        assertFalse(DISABLED.isLevelLowerThan(level));
        assertTrue(TYPE_EXTRACTION.isLevelLowerThan(level));
        assertTrue(FILE_INFORMATION.isLevelLowerThan(level));
        assertTrue(BASE_PREVIEW.isLevelLowerThan(level));
        assertTrue(FULL_PREVIEW.isLevelLowerThan(level));
        assertFalse(DEEP_ANALYSIS.isLevelLowerThan(level));
    }

    @Test
    void testGetJobPriority() {
        assertThat(DISABLED.getJobPriority()).isEqualTo(-1);
        assertThat(TYPE_EXTRACTION.getJobPriority()).isEqualTo(4);
        assertThat(FILE_INFORMATION.getJobPriority()).isEqualTo(3);
        assertThat(BASE_PREVIEW.getJobPriority()).isEqualTo(2);
        assertThat(FULL_PREVIEW.getJobPriority()).isEqualTo(1);
        assertThat(DEEP_ANALYSIS.getJobPriority()).isZero();
    }

}
