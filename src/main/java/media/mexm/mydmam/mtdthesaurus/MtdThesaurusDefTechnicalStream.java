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
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryType.BOOLEAN;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryType.IDENTIFIER_OR_SERIAL_ID;

@MetadataThesaurusClassifier(value = "technical:stream", longname = "Stream from media container (technical)",
                             kind = TECHNICAL_ATTRIBUTE)
public interface MtdThesaurusDefTechnicalStream {

    @MetadataThesaurusSortIndexOrder(1)
    @MetadataThesaurusEntryAttribute(type = IDENTIFIER_OR_SERIAL_ID)
    MetadataThesaurusEntry referenceId();

    @MetadataThesaurusSortIndexOrder(2)
    MetadataThesaurusEntry programId();

    @MetadataThesaurusSortIndexOrder(3)
    MetadataThesaurusEntry type();

    @MetadataThesaurusEntryAttribute(type = BOOLEAN)
    @MetadataThesaurusSortIndexOrder(4)
    MetadataThesaurusEntry isSecondary();

    @MetadataThesaurusSortIndexOrder(5)
    MetadataThesaurusEntry codec();

    @MetadataThesaurusSortIndexOrder(6)
    MetadataThesaurusEntry codecName();

    @MetadataThesaurusEntryAttribute(unit = BITS_PER_SECONDS)
    @MetadataThesaurusSortIndexOrder(7)
    MetadataThesaurusEntry bitrate();

    @MetadataThesaurusSortIndexOrder(8)
    MetadataThesaurusEntry level();

    @MetadataThesaurusSortIndexOrder(9)
    MetadataThesaurusEntry profile();

    @MetadataThesaurusSortIndexOrder(10)
    MetadataThesaurusEntry timeBase();

    @MetadataThesaurusSortIndexOrder(11)
    @MetadataThesaurusEntryAttribute(unit = MILLISECONDS)
    MetadataThesaurusEntry startTime();

    @MetadataThesaurusSortIndexOrder(12)
    MetadataThesaurusEntry disposition();

}
