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

import static java.util.stream.Collectors.joining;
import static media.mexm.mydmam.entity.FileMetadataEntity.MAX_ORIGIN_LENGTH;
import static media.mexm.mydmam.entity.PendingActivityEntity.PREVIOUS_HANDLERS_SIZES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.internal.util.MockUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class ActivityHandlersNameSizesTest {

    @Autowired
    List<ActivityHandler> handlers;

    @Test
    void preChecks() {
        assertThat(handlers).size().isGreaterThan(1);
        handlers.stream().forEach(ah -> assertFalse(MockUtil.isMock(ah), ah.getClass().getName()));
    }

    @Test
    void testAllHandlerNames() {
        handlers.stream()
                .map(ActivityHandler::getHandlerName)
                .forEach(n -> {
                    assertThat(n).doesNotContain(" ");
                    assertThat(n).isNotEmpty();
                });
    }

    @Test
    void testAllMetadataOriginName() {
        handlers.stream()
                .map(ActivityHandler::getMetadataOriginName)
                .forEach(n -> {
                    assertThat(n).doesNotContain(" ");
                    assertThat(n).isNotEmpty();
                    assertThat(n).hasSizeLessThan(MAX_ORIGIN_LENGTH);
                });
    }

    @Test
    void testAllHandlerNamesSizes() {
        final var fullList = handlers.stream()
                .map(ActivityHandler::getHandlerName)
                .collect(joining(" "));

        assertThat(fullList.length()).isLessThan(PREVIOUS_HANDLERS_SIZES);
    }

}
