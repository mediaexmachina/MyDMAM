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

import static java.lang.Long.parseLong;

public record HTTPRangeRequest(long start, long end, long contentLength, boolean partial) { // TODO test

	private static final int BYTES_LENGTH = "bytes=".length();

	public HTTPRangeRequest(final String rangeHeader, final long fileLength) {
		final long start;
		final long end;
		if (rangeHeader == null) {
			start = 0;
			end = fileLength - 1;
		} else {
			final var ranges = rangeHeader.split("-");
			start = parseLong(ranges[0].substring(BYTES_LENGTH));
			if (ranges.length > 1) {
				end = parseLong(ranges[1]);
			} else {
				end = fileLength - 1;
			}
		}
		final var contentLength = end + 1 - start;
		this(start, end, contentLength, fileLength != contentLength);
	}

}
