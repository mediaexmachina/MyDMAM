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
package media.mexm.mydmam.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.RealmAboutConf;
import media.mexm.mydmam.configuration.RealmConf;
import media.mexm.mydmam.configuration.SiteConf;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class InstanceControllerTest {

    private static final RequestMapping REQUEST_MAPPING = InstanceController.class.getAnnotation(
            RequestMapping.class);
    private static final String BASE_MAPPING = REQUEST_MAPPING.value()[0];
    private static final ResultMatcher CONTENT_TYPE = content().contentType(REQUEST_MAPPING.produces()[0]);
    private static final ResultMatcher STATUS_OK = status().isOk();
    private static final ResultMatcher STATUS_NO_CONTENT = status().isNoContent();

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    MyDMAMConfigurationProperties conf;

    @Mock
    RealmConf realmConf;
    @Fake
    String siteName;
    @Fake
    String realmName;
    @Fake
    String longName;

    HttpHeaders baseHeaders;
    SiteConf siteConf;
    RealmAboutConf about;

    @BeforeEach
    void init() {
        baseHeaders = new HttpHeaders();
        baseHeaders.setContentType(APPLICATION_JSON);

        siteConf = new SiteConf(siteName, "description", "location", "pageFooter");
        about = new RealmAboutConf(longName, "contact", "logo", "color");

        when(conf.site()).thenReturn(siteConf);
        when(conf.getRealmByName(realmName)).thenReturn(Optional.ofNullable(realmConf));
        when(realmConf.about()).thenReturn(about);
    }

    @Test
    void testGetSiteConf() throws Exception {
        final var content = mvc.perform(get(BASE_MAPPING + "/site")
                .headers(baseHeaders))
                .andExpect(STATUS_OK)
                .andExpect(CONTENT_TYPE)
                .andReturn()
                .getResponse()
                .getContentAsString();

        final var response = objectMapper.readValue(content, SiteConf.class);
        assertThat(response).isEqualTo(siteConf);

        verify(conf, times(1)).site();
    }

    @Test
    void testGetRealmAbout() throws Exception {
        final var content = mvc.perform(get(BASE_MAPPING + "/about/" + realmName)
                .headers(baseHeaders))
                .andExpect(STATUS_OK)
                .andExpect(CONTENT_TYPE)
                .andReturn()
                .getResponse()
                .getContentAsString();

        final var response = objectMapper.readValue(content, RealmAboutConf.class);
        assertThat(response).isEqualTo(about);

        verify(conf, times(1)).getRealmByName(realmName);
        verify(realmConf, times(1)).about();
    }

    @Test
    void testGetRealmAbout_noRealm() throws Exception {
        when(conf.getRealmByName(realmName)).thenReturn(Optional.empty());

        mvc.perform(get(BASE_MAPPING + "/about/" + realmName)
                .headers(baseHeaders))
                .andExpect(STATUS_NO_CONTENT)
                .andReturn();

        verify(conf, times(1)).getRealmByName(realmName);
    }

}
