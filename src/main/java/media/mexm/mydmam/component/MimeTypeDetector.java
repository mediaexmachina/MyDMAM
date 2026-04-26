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
package media.mexm.mydmam.component;

import static eu.medsea.mimeutil.MimeUtil2.UNKNOWN_MIME_TYPE;
import static eu.medsea.mimeutil.MimeUtil2.getMostSpecificMimeType;
import static org.apache.commons.io.FilenameUtils.getExtension;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil2;
import lombok.extern.slf4j.Slf4j;

/**
 * @see https://github.com/mimeutil/MimeUtil
 */
@Slf4j
@Component
public class MimeTypeDetector {

    public static final String DEFAULT_MIME_TYPE = UNKNOWN_MIME_TYPE.toString();

    @Autowired
    MimeUtil2 magicMimeUtil;
    @Autowired
    MimeUtil2 extensionMimeUtil;

    public String getMimeType(final String filename) {
        final var file = new File("TEMP_DETECTION_MYDMAM." + getExtension(filename).toLowerCase());
        return getMostSpecificMimeType(extensionMimeUtil.getMimeTypes(file)).toString().toLowerCase();
    }

    public String getMimeType(final File source) {
        final var detectedTypes = castList(magicMimeUtil.getMimeTypes(source));
        final var extensionTypes = castList(extensionMimeUtil.getMimeTypes(source));

        log.debug("File \"{}\" detected as mime type {}, extension type {}",
                source.getAbsolutePath(), detectedTypes, extensionTypes);

        for (final var dT : detectedTypes) {
            for (final var eT : extensionTypes) {
                if (dT.equals(eT)) {
                    return dT.toString();
                }
            }
        }

        final var allTypes = new LinkedHashSet<MimeType>();
        allTypes.addAll(detectedTypes);
        if (detectedTypes.isEmpty()) {
            allTypes.addAll(extensionTypes);
        }

        return allTypes.stream()
                .sorted((l, r) -> r.compareTo(l))
                .findFirst()
                .map(Object::toString)
                .map(String::toLowerCase)
                .orElse(DEFAULT_MIME_TYPE);
    }

    static List<MimeType> castList(final Collection<?> rawCollection) {
        final List<MimeType> result = new ArrayList<>(rawCollection.size());
        for (final Object o : rawCollection) {
            if (o.toString().equals(DEFAULT_MIME_TYPE)) {
                continue;
            }
            result.add(MimeType.class.cast(o));
        }
        return result;
    }

}
