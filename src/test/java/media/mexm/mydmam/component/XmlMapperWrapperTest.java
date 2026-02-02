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
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.core.JsonProcessingException;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class XmlMapperWrapperTest {

	@Autowired
	XmlMapperWrapper xmlMapperWrapper;

	@Fake
	String rootName;
	@Fake
	String value;

	record Item(String value) {
	}

	@Test
	void testGetXmlMapper() throws JsonProcessingException {
		final var result = xmlMapperWrapper.getXmlMapper().writer()
				.withRootName(rootName)
				.writeValueAsString(new Item(value));

		assertThat(result).isEqualTo(
				"<?xml version='1.0' encoding='UTF-8'?><" + rootName + "><value>"
									 + value + "</value></" + rootName + ">");
	}

}
