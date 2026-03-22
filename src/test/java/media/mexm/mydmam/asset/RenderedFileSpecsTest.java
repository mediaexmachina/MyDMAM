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
package media.mexm.mydmam.asset;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class RenderedFileSpecsTest {

    @Fake
    String line0;
    @Fake
    String line1;
    @Fake
    String line2;
    @Fake
    String line3;

    @Test
    void testFixNL() {
        assertEquals(line0 + " " + line1 + " " + line2 + " " + line3,
                RenderedFileSpecs.fixNL(line0 + " \n" + line1 + "\r " + " \n\n \r" + line2 + " \r\n " + line3));
    }

}
