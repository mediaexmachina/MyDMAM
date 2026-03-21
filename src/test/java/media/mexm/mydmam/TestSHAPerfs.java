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
package media.mexm.mydmam;

import static media.mexm.mydmam.entity.FileEntity.hashPath;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import net.datafaker.Faker;

/**
 * Check how many times it take to compute at least 1M SHA hash.
 * We accept no more than 2 secs
 */
class TestSHAPerfs {

	static final Faker faker = Faker.instance();

	@Test
	@Timeout(2)
	void test() {
		final var paths = IntStream.range(0, 1_000_000)
				.mapToObj(_ -> faker.numerify("path#####"))
				.toList();
		final var realm = faker.numerify("realm#####");
		final var storage = faker.numerify("storage#####");

		paths.parallelStream()
				.map(path -> hashPath(realm, storage, path))
				.forEach(_ -> {
				});
	}

}
