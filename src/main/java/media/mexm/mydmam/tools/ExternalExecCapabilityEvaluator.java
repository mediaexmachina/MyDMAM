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

import java.io.File;
import java.util.stream.IntStream;

import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;

public record ExternalExecCapabilityEvaluator(String name,
                                              String playbookName,
                                              File workingDir,
                                              int returnCode,
                                              CapturedStdOutErrTextRetention captured) {

    public boolean haveFile(final String fileName) {
        final var file = new File(workingDir, fileName);
        return file.exists() && file.isFile() && file.length() > 0;
    }

    public boolean haveReturnCode(final int... codes) {
        return IntStream.of(codes).anyMatch(i -> i == returnCode);
    }

    public boolean haveStringInStdOutErr(final String value) {
        return captured.getStdouterrLines(false)
                .anyMatch(l -> l.toLowerCase().contains(value.toLowerCase()));
    }

}
