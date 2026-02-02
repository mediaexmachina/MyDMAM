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

import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class TransfertFileRangeToOutputStreamTest {

	@Fake
	boolean partial;
	@Mock
	ByteArrayOutputStream outputStreamMock;

	File file;
	byte[] content;
	HTTPRangeRequest range;
	TransfertFileRangeToOutputStream t;
	ByteArrayOutputStream outputStream;

	@BeforeEach
	void init() {
		file = new File("LICENCE.TXT");
		assertThat(file).exists();
		outputStream = new ByteArrayOutputStream((int) file.length() / 10);
	}

	@Test
	void testSend_full() throws IOException {
		content = readFileToByteArray(file);

		final var start = 0;
		range = new HTTPRangeRequest(start, file.length(), file.length(), partial);
		t = new TransfertFileRangeToOutputStream(file, range);
		t.send(outputStream);

		assertThat(outputStream.toByteArray()).isEqualTo(content);
	}

	@Nested
	class Partial {

		long start;
		int len;
		FileInputStream fso;

		@Test
		void testSend_partial() {// NOSONAR S2699
			start = file.length() / 20;
			len = (int) file.length() - (int) file.length() / 20;
			content = new byte[len];

		}

		@Test
		void testSend_partial_small() {// NOSONAR S2699
			start = file.length() / 20;
			len = (int) start + 10;
			content = new byte[len];
		}

		@Test
		void testSend_partial_empty() {// NOSONAR S2699
			start = file.length() / 20;
			len = 0;
			content = new byte[len];
		}

		@AfterEach
		void checks() throws IOException {
			fso = new FileInputStream(file);
			fso.skip(start);
			assertThat(fso.read(content)).isEqualTo(len);
			closeQuietly(fso);

			range = new HTTPRangeRequest(start, start + len - 1, file.length(), partial);
			t = new TransfertFileRangeToOutputStream(file, range);
			t.send(outputStream);

			assertThat(outputStream.toByteArray()).isEqualTo(content);
		}

	}

	@Test
	void testSend_IOException() {// NOSONAR S2699
		range = new HTTPRangeRequest(0, file.length(), file.length(), partial);
		t = new TransfertFileRangeToOutputStream(new File("."), range);
		t.send(outputStreamMock);
	}

}
