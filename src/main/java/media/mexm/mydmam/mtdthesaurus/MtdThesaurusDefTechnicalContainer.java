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
package media.mexm.mydmam.mtdthesaurus;

import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusClassifierKind.TECHNICAL_ATTRIBUTE;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryNumericalUnit.BITS_PER_SECONDS;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryNumericalUnit.MILLISECONDS;

@MetadataThesaurusClassifier(value = "technical:container", longname = "Media container (technical)",
                             kind = TECHNICAL_ATTRIBUTE)
public interface MtdThesaurusDefTechnicalContainer {

    @MetadataThesaurusSortIndexOrder(1)
    @MetadataThesaurusEntryAttribute(unit = MILLISECONDS)
    MetadataThesaurusEntry duration();

    @MetadataThesaurusSortIndexOrder(2)
    MetadataThesaurusEntry timecode();

    @MetadataThesaurusSortIndexOrder(3)
    MetadataThesaurusEntry format();

    @MetadataThesaurusSortIndexOrder(4)
    MetadataThesaurusEntry formatName();

    @MetadataThesaurusSortIndexOrder(5)
    @MetadataThesaurusEntryAttribute(unit = BITS_PER_SECONDS)
    MetadataThesaurusEntry bitrate();

    @MetadataThesaurusSortIndexOrder(6)
    @MetadataThesaurusEntryAttribute(unit = MILLISECONDS)
    MetadataThesaurusEntry startTime();

}
