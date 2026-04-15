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

import static java.lang.System.exit;
import static java.nio.file.Files.writeString;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.nio.file.Path;

public class SimpleApp {

    public static final String SYS_OUT = "It works!";
    public static final String OUT_FILE_NAME = SimpleApp.class.getSimpleName().toLowerCase() + ".txt";
    public static final String OUT_FILE_CONTENT = "File content";
    public static final int STATUS = 41;

    public static void main(final String[] args) throws IOException {// NOSONAR 1172
        System.out.println(SYS_OUT);
        writeString(Path.of(OUT_FILE_NAME), OUT_FILE_CONTENT, CREATE, WRITE);
        exit(STATUS);
    }

}
