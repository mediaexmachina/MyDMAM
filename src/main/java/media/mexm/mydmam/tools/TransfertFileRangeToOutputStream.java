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

import static java.lang.Math.min;
import static org.apache.commons.io.IOUtils.EOF;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
/**
 * See IOUtils.copyLarge(final InputStream input, final OutputStream output, final long inputOffset, final long length, final byte[] buffer)
 */
public record TransfertFileRangeToOutputStream(File file,
											   HTTPRangeRequest range) {

	public void send(final OutputStream output) {
		final var length = range.end() + 1 - range.start();
		if (length == 0) {
			return;
		}

		/**
		 * 4 kbytes
		 */
		final var buffer = new byte[0x1000];

		var bytesToRead = buffer.length;
		if (length > 0 && length < buffer.length) {
			bytesToRead = (int) length;
		}

		log.debug("Start to send={}, from={} to={} len={}", file, range.start(), range.end(), length);
		try (final var input = new RandomAccessFile(file, "r")) {
			final var pos = range.start();
			input.seek(pos);

			int read;
			var totalRead = 0L;
			while (bytesToRead > 0 && EOF != (read = input.read(buffer, 0, bytesToRead))) {
				output.write(buffer, 0, read);
				totalRead += read;
				if (length > 0) {
					bytesToRead = (int) min(length - totalRead, buffer.length);
				}
			}

			output.flush();
		} catch (final IOException e) {
			log.error("Can't read/send {}", file, e);
		}
	}

}
