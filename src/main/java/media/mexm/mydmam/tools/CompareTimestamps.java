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

import static java.lang.Math.round;

import java.sql.Timestamp;

public class CompareTimestamps {

    private CompareTimestamps() {
        /** This utility class should not be instantiated */
    }

    public static boolean equalsTimestamps(final Timestamp l, final Timestamp r) {
        return equalsTimestamps(l, r.getTime());
    }

    public static boolean equalsTimestamps(final Timestamp l, final long r) {
        return round(l.getTime() / 1000d) == round(r / 1000d);
    }

    public static Timestamp roundToTimestamp(final long date) {
        return new Timestamp(round(date / 1000d) * 1000);
    }

}
