/*
 * This file is part of mydmam.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2025
 *
 */
package media.mexm.mydmam.component;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.service.PendingActivityService;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "MockInternalService" })
class StartupTest {

    @MockitoBean
    AuditTrail auditTrail;
    @MockitoBean
    ImageMagick imageMagick;
    @MockitoBean
    Indexer indexer;
    @MockitoBean
    PathIndexer pathIndexer;
    @MockitoBean
    PendingActivityService pendingActivityService;
    @MockitoBean
    XPDF xpdf;

    @Autowired
    InternalService service;

    @Autowired
    Startup startup;

    @Test
    void test() throws Exception {
        verify(service, atLeastOnce()).getInternalServiceName();
        verify(service, times(1)).internalServiceStart();

        startup.destroy();

        verify(service, atLeastOnce()).getInternalServiceName();
        verify(service, times(1)).internalServiceStop();
    }

}
