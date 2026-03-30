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
package media.mexm.mydmam.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class PendingActivityEntityTest {

    @Mock
    ActivityHandler activityHandler;
    @Mock
    FileEntity file;

    @Fake
    ActivityEventType eventType;
    @Fake
    String handlerName;
    @Fake
    String previousHandlers;
    @Fake
    String host;
    @Fake
    long pid;

    PendingActivityEntity pae;

    @BeforeEach
    void init() {
        when(activityHandler.getHandlerName()).thenReturn(handlerName);
        pae = new PendingActivityEntity(activityHandler, eventType, previousHandlers, file, host, pid);
        verify(activityHandler, times(1)).getHandlerName();
    }

    @Test
    void testGetFile() {
        assertEquals(file, pae.getFile());
    }

    @Test
    void testGetHandlerName() {
        assertEquals(handlerName, pae.getHandlerName());
    }

    @Test
    void testGetEventType() {
        assertEquals(eventType.name(), pae.getEventType());
    }

    @Test
    void testGetUpdated() {
        assertEquals(pae.getUpdated(), pae.getCreateDate());
    }

    @Test
    void testGetCreateDate() {
        assertThat(pae.getCreateDate()).isBeforeOrEqualTo(new Date());
    }

}
