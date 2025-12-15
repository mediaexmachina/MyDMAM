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
 * Copyright (C) Media ex Machina 2025
 *
 */
package media.mexm.mydmam.configuration;

import static media.mexm.mydmam.entity.FileEntity.MAX_NAME_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class TechnicalNameTest {

	@Fake
	String name;

	TechnicalName tn;

	@Test
	void testTechnicalName() {
		assertThat(new TechnicalName(name + "\u00a0 \t_\r \n" + name + "a  ?  ééé€_A?a-a12+d" + name).name())
				.isEqualTo(name + "___" + name + "a_eee__A_a-a12_d" + name);

		final var longString = StringUtils.repeat(name, MAX_NAME_SIZE);
		assertThat(new TechnicalName(longString).name()).hasSize(MAX_NAME_SIZE);

		assertThrows(NullPointerException.class, () -> new TechnicalName(null));
		assertThrows(IllegalArgumentException.class, () -> new TechnicalName(""));
	}

	@Test
	void testToString() {
		tn = new TechnicalName(name);
		assertEquals(name, tn.name());
		assertEquals(name, tn.toString());
	}

}
