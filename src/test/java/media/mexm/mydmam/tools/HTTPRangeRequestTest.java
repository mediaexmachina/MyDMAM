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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import net.datafaker.Faker;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class HTTPRangeRequestTest {
	static Faker faker = net.datafaker.Faker.instance();

	@Fake(min = 100, max = 100_000)
	long fileLength;

	HTTPRangeRequest r;
	long start;
	long end;

	@BeforeEach
	void init() {
		start = faker.random().getRandomInternal().nextLong(10, fileLength);
		end = faker.random().getRandomInternal().nextLong(start, fileLength);
	}

	@Test
	void testEmpty() {
		r = new HTTPRangeRequest("bytes=", fileLength);
		check(0, fileLength - 1, fileLength, false);

		r = new HTTPRangeRequest("", fileLength);
		check(0, fileLength - 1, fileLength, false);

		r = new HTTPRangeRequest(null, fileLength);
		check(0, fileLength - 1, fileLength, false);
	}

	@Test
	void testPartial() {
		r = new HTTPRangeRequest("bytes=" + start + "-" + end, fileLength);
		check(start, end, end - start + 1, true);
	}

	@Test
	void testSkip() {
		r = new HTTPRangeRequest("bytes=" + start, fileLength);
		check(start, fileLength - 1, fileLength - start, true);
	}

	@Test
	void testFull() {
		r = new HTTPRangeRequest("bytes=" + 0, fileLength);
		check(0, fileLength - 1, fileLength, false);

		r = new HTTPRangeRequest("bytes=" + 0 + "-" + (fileLength - 1), fileLength);
		check(0, fileLength - 1, fileLength, false);
	}

	@Test
	void testTooBig_start() {
		r = new HTTPRangeRequest("bytes=" + (fileLength + 1) + "-" + (fileLength - 1), fileLength);
		check(0, fileLength - 1, fileLength, false);
	}

	@Test
	void testTooBig_end() {
		r = new HTTPRangeRequest("bytes=" + 0 + "-" + (fileLength + 1), fileLength);
		check(0, fileLength - 1, fileLength, false);
	}

	@Test
	void testPartial_endBeforeStart() {
		r = new HTTPRangeRequest("bytes=" + end + "-" + start, fileLength);
		check(end, end, 1, true);
	}

	void check(final long start, final long end, final long contentLength, final boolean partial) {
		assertThat(r.start()).describedAs("start").isEqualTo(start);
		assertThat(r.end()).describedAs("end").isEqualTo(end);
		assertThat(r.contentLength()).describedAs("contentLength").isEqualTo(contentLength);
		assertThat(r.partial()).describedAs("partial").isEqualTo(partial);
	}

}
