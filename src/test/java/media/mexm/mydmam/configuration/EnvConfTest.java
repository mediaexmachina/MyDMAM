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
package media.mexm.mydmam.configuration;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class EnvConfTest {

    @Fake
    long pendingActivityMaxAgeGraceRestartDuration;
    @Fake(min = 100, max = 1000)
    long timeBetweenScansValue;

    Duration timeBetweenScans;
    Duration pendingActivityMaxAgeGraceRestart;
    EnvConf ev;

    @BeforeEach
    void init() {
        timeBetweenScans = Duration.ofMillis(timeBetweenScansValue);
        pendingActivityMaxAgeGraceRestart = Duration.ofMillis(abs(pendingActivityMaxAgeGraceRestartDuration));
        ev = new EnvConf(timeBetweenScans, null, null, null, false, 0, 0, pendingActivityMaxAgeGraceRestart);
    }

    @Test
    void testBadPendingActivityMaxAgeGraceRestart() {
        pendingActivityMaxAgeGraceRestart = Duration.ofMillis(-abs(pendingActivityMaxAgeGraceRestartDuration));

        assertThrows(IllegalArgumentException.class,
                () -> new EnvConf(
                        timeBetweenScans, null, null, null, false, 0, 0, pendingActivityMaxAgeGraceRestart));

        pendingActivityMaxAgeGraceRestart = Duration.ZERO;
        assertThrows(IllegalArgumentException.class,
                () -> new EnvConf(
                        timeBetweenScans, null, null, null, false, 0, 0, pendingActivityMaxAgeGraceRestart));
    }

    @Test
    void testBadTimeBetweenScans() {
        timeBetweenScans = Duration.ofMillis(-timeBetweenScansValue);
        assertThrows(IllegalArgumentException.class,
                () -> new EnvConf(
                        timeBetweenScans, null, null, null, false, 0, 0, pendingActivityMaxAgeGraceRestart));

        timeBetweenScans = Duration.ZERO;
        assertThrows(IllegalArgumentException.class,
                () -> new EnvConf(
                        timeBetweenScans, null, null, null, false, 0, 0, pendingActivityMaxAgeGraceRestart));
    }

}
