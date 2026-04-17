/*
 * This file is part of MyDMAM.
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
package media.mexm.mydmam;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.regex.Pattern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import media.mexm.mydmam.tools.DTORecordToAngularInterfaceConverter;
import media.mexm.mydmam.tools.MetadataThesaurusToAngularClasses;

@SpringBootApplication
public class App {

    public static final long START_DATE = System.currentTimeMillis();
    public static final String CONTROLLER_BASE_MAPPING_API_PATH = "/api/v1";
    public static final Pattern REPLACE_NORMALIZED = Pattern.compile("\\p{M}");

    public static void main(final String[] args) {
        if (args != null && args.length == 1 && args[0].equals("export-dto-angular")) {
            final var appDir = new File("src/front/app");
            new DTORecordToAngularInterfaceConverter("media.mexm.mydmam.dto", appDir).parseDTOs();

            try {
                new MetadataThesaurusToAngularClasses().make(appDir);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }

            return;
        }
        SpringApplication.run(App.class, args);
    }

}
