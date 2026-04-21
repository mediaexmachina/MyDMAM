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

import static org.apache.commons.lang3.SystemUtils.IS_OS_MAC_OSX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class AboutInstanceTest {

    @Autowired
    AboutInstance ai;
    @Autowired
    MyDMAMConfigurationProperties conf;

    @Test
    void testGetInstanceName() {
        assertThat(ai.getInstanceName()).isEqualTo(conf.instancename());
    }

    @Test
    void testGetPid() {
        assertThat(ai.getPid()).isGreaterThan(0l);
    }

    @Test
    void testHostName() {
        assertThat(ai.getHostName()).isNotBlank();
    }

    @Test
    void testGetEtcHostname() {
        if (IS_OS_WINDOWS) {
            assertThat(ai.getEtcHostname()).isEmpty();
        } else if (IS_OS_MAC_OSX) {
            /**
             * Somebody want to test ?
             */
            return;
        } else {
            assertThat(ai.getEtcHostname()).asString().isNotBlank();
        }
    }

    @Test
    void testGetCmdHostname() {
        assertThat(ai.getCmdHostname()).asString().isNotBlank();
    }

    @Test
    void testGetInetAddressHostname() {
        assertThat(ai.getCmdHostname()).isNotNull();
    }

    @Test
    void testQueryPingTime() {
        assertThat(ai.getQueryPingTime()).isBetween(0l, 1_000l);
    }

    @Test
    void testDatabaseDeltaTime() {
        assertThat(ai.getDatabaseDeltaTime()).isBetween(0l, 1_000l);
    }
}
