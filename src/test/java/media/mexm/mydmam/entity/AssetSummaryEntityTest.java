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

import static java.lang.System.currentTimeMillis;
import static java.time.Instant.ofEpochMilli;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class AssetSummaryEntityTest {

	@Mock
	FileEntity file;

	AssetSummaryEntity ase;

	@Test
	void testAssetSummaryEntity() {
		ase = new AssetSummaryEntity();
		assertNull(ase.getFile());
		assertNull(ase.getCreateDate());
	}

	@Test
	void testAssetSummaryEntityFileEntity() {
		ase = new AssetSummaryEntity(file);
		assertThat(ase.getFile()).isEqualTo(file);
		assertThat(ase.getCreateDate())
				.isBetween(ofEpochMilli(currentTimeMillis() - 2000),
						ofEpochMilli(currentTimeMillis() + 1));
	}

}
