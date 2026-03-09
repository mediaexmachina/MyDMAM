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
package media.mexm.mydmam.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class AllowBlockListsTest {

	@Fake
	String candidate;
	AllowBlockLists alb;

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testPass_allowOnly(final boolean blockBeforeAllow) {
		alb = new AllowBlockLists(
				List.of("allow",
						"some*",
						"*thing",
						"som?thing",
						"*omethin*"),
				List.of(),
				blockBeforeAllow);

		assertFalse(alb.pass(candidate));
		assertTrue(alb.pass("allow"));
		assertTrue(alb.pass("someX"));
		assertTrue(alb.pass("Xthing"));
		assertTrue(alb.pass("som0thing"));
		assertTrue(alb.pass("XomethinX"));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testPass_blockOnly(final boolean blockBeforeAllow) {
		alb = new AllowBlockLists(
				List.of(),
				List.of("block",
						"some*",
						"*thing",
						"som?thing",
						"*omethin*"),
				blockBeforeAllow);

		assertTrue(alb.pass(candidate));
		assertFalse(alb.pass("block"));
		assertFalse(alb.pass("someX"));
		assertFalse(alb.pass("Xthing"));
		assertFalse(alb.pass("som0thing"));
		assertFalse(alb.pass("XomethinX"));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testPass_empty(final boolean blockBeforeAllow) {
		assertTrue(new AllowBlockLists(null, null, blockBeforeAllow)
				.pass(candidate));
		assertTrue(new AllowBlockLists(List.of(), null, blockBeforeAllow)
				.pass(candidate));
		assertTrue(new AllowBlockLists(null, List.of(), blockBeforeAllow)
				.pass(candidate));
		assertTrue(new AllowBlockLists(List.of(), List.of(), blockBeforeAllow)
				.pass(candidate));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void testPass_allowAndBlock(final boolean blockBeforeAllow) {
		alb = new AllowBlockLists(
				List.of("allow", "*yep*", "som?thing"),
				List.of("block", "*nope*", "som?thing"),
				blockBeforeAllow);

		assertTrue(alb.pass("allow"));
		assertTrue(alb.pass("allow".toUpperCase()));
		assertFalse(alb.pass("block"));
		assertFalse(alb.pass("block".toUpperCase()));
		assertTrue(alb.pass("this yep go"));
		assertFalse(alb.pass("this nope go"));
		assertFalse(alb.pass(candidate));

		assertEquals(blockBeforeAllow == false, alb.pass("something"));
	}

	@Test
	void testPass_allowBeforeBlock() {
		alb = new AllowBlockLists(
				List.of("som?thing"),
				List.of("som?thing"),
				false);
		assertTrue(alb.pass("something"));
	}

	@Test
	void testPass_blockBeforeAllow() {
		alb = new AllowBlockLists(
				List.of("som?thing"),
				List.of("som?thing"),
				true);
		assertFalse(alb.pass("something"));
	}

}
