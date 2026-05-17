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
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryType.IDENTIFIER_OR_SERIAL_ID;

@MetadataThesaurusClassifier(value = "technical:mxf", longname = "MXF (technical)", kind = TECHNICAL_ATTRIBUTE)
public interface MtdThesaurusDefTechnicalMXF {

    @MetadataThesaurusEntryAttribute(type = IDENTIFIER_OR_SERIAL_ID)
    MetadataThesaurusEntry operationalPatternUL();

    @MetadataThesaurusEntryAttribute(type = IDENTIFIER_OR_SERIAL_ID)
    MetadataThesaurusEntry uid();

    @MetadataThesaurusEntryAttribute(type = IDENTIFIER_OR_SERIAL_ID)
    MetadataThesaurusEntry generationUID();

    @MetadataThesaurusEntryAttribute(type = IDENTIFIER_OR_SERIAL_ID)
    MetadataThesaurusEntry materialPackageUMID();

    MetadataThesaurusEntry trackName();

    @MetadataThesaurusEntryAttribute(type = IDENTIFIER_OR_SERIAL_ID)
    MetadataThesaurusEntry filePackageUMID();

    MetadataThesaurusEntry filePackageName();

}
