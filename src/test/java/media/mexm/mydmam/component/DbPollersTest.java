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
package media.mexm.mydmam.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.entity.InstanceEntity;
import media.mexm.mydmam.repository.InstanceDao;
import media.mexm.mydmam.service.PendingActivityService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.jobkit.engine.FlatJobKitEngine;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class DbPollersTest {

    @MockitoBean
    InstanceDao instanceDao;
    @MockitoBean
    PendingActivityService pendingActivityService;

    @Mock
    InstanceEntity instanceEntity;
    @Fake
    int instanceId;

    @Autowired
    FlatJobKitEngine flatJobKitEngine;
    @Autowired
    DbPollers dbPollers;

    @BeforeEach
    void init() {
        when(instanceDao.getSelfInstance()).thenReturn(instanceEntity);
        when(instanceEntity.getId()).thenReturn(instanceId);
    }

    @AfterEach
    void ends() {
        verifyNoMoreInteractions(instanceDao, pendingActivityService);
    }

    @Test
    void testUpdateDbPresenceInstance() {
        assertThat(flatJobKitEngine.getEndEventsList()).isEmpty();
        assertTrue(flatJobKitEngine.isEmptyActiveServicesList());
    }

    @Test
    void testInternalServiceStart() throws Exception {
        dbPollers.internalServiceStart();

        assertThat(flatJobKitEngine.getEndEventsList()).isEmpty();
        assertFalse(flatJobKitEngine.isEmptyActiveServicesList());

        verify(instanceDao, times(1)).getSelfInstance();
        verify(instanceEntity, times(1)).getId();

        flatJobKitEngine.runAllServicesOnce();
        verify(instanceDao, times(1)).updatePresenceInstance(instanceId);
        verify(pendingActivityService, times(1)).restartPendingActivities(true);

        flatJobKitEngine.runAllServicesOnce();
        verify(instanceDao, times(2)).updatePresenceInstance(instanceId);
        verify(pendingActivityService, times(1)).restartPendingActivities(false);

        flatJobKitEngine.runAllServicesOnce();
        verify(instanceDao, times(3)).updatePresenceInstance(instanceId);
        verify(pendingActivityService, times(2)).restartPendingActivities(false);

        assertThat(flatJobKitEngine.getEndEventsList()).isEmpty();
        assertFalse(flatJobKitEngine.isEmptyActiveServicesList());
    }

    @Test
    void testInternalServiceStop() throws Exception {
        dbPollers.internalServiceStop();

        assertThat(flatJobKitEngine.getEndEventsList()).isEmpty();
        assertTrue(flatJobKitEngine.isEmptyActiveServicesList());

        dbPollers.internalServiceStart();
        verify(instanceDao, times(1)).getSelfInstance();
        verify(instanceEntity, times(1)).getId();

        dbPollers.internalServiceStop();

        assertThat(flatJobKitEngine.getEndEventsList()).isEmpty();
        assertTrue(flatJobKitEngine.isEmptyActiveServicesList());
    }

    @Test
    void testGetInternalServiceName() {
        assertThat(dbPollers.getInternalServiceName()).isNotBlank();
    }
}
