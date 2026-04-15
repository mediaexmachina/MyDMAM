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
package media.mexm.mydmam.tools;

import static media.mexm.mydmam.tools.CompareTimestamps.equalsTimestamps;
import static media.mexm.mydmam.tools.CompareTimestamps.roundToTimestamp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

class CompareTimestampsTest {

    @Test
    void testEqualsTimestamp() {
        assertTrue(equalsTimestamps(new Timestamp(60_000l), new Timestamp(60_000l)));
        assertTrue(equalsTimestamps(new Timestamp(60_000l), new Timestamp(60_400l)));
        assertFalse(equalsTimestamps(new Timestamp(60_000l), new Timestamp(60_500l)));
        assertTrue(equalsTimestamps(new Timestamp(61_000l), new Timestamp(60_500l)));
    }

    @Test
    void testEqualsTimestampLong() {
        assertTrue(equalsTimestamps(new Timestamp(60_000l), 60_000l));
        assertTrue(equalsTimestamps(new Timestamp(60_000l), 60_400l));
        assertFalse(equalsTimestamps(new Timestamp(60_000l), 60_500l));
        assertTrue(equalsTimestamps(new Timestamp(61_000l), 60_500l));
    }

    @Test
    void testRoundToTimestamp() {
        assertThat(roundToTimestamp(60_000l).getTime()).isEqualTo(60_000l);
        assertThat(roundToTimestamp(60_500l).getTime()).isEqualTo(61_000l);
    }

}
