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
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryNumericalUnit.DEGREES;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryNumericalUnit.MILLIMETERS;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryType.BOOLEAN;

@MetadataThesaurusClassifier(value = "pdf", longname = "PDF", kind = TECHNICAL_ATTRIBUTE)
public interface MtdThesaurusDefPDF {

    @MetadataThesaurusSortIndexOrder(1)
    MetadataThesaurusEntry pageCount();

    @MetadataThesaurusSortIndexOrder(2)
    MetadataThesaurusEntry encrypted();

    @MetadataThesaurusSortIndexOrder(3)
    MetadataThesaurusEntry pdfVersion();

    @MetadataThesaurusSortIndexOrder(4)
    @MetadataThesaurusEntryAttribute(type = BOOLEAN)
    MetadataThesaurusEntry javascript();

    @MetadataThesaurusSortIndexOrder(5)
    MetadataThesaurusEntry keywords();

    @MetadataThesaurusSortIndexOrder(6)
    @MetadataThesaurusEntryAttribute(unit = MILLIMETERS)
    MetadataThesaurusEntry pageWidth();

    @MetadataThesaurusSortIndexOrder(7)
    @MetadataThesaurusEntryAttribute(unit = MILLIMETERS)
    MetadataThesaurusEntry pageHeight();

    @MetadataThesaurusSortIndexOrder(8)
    @MetadataThesaurusEntryAttribute(unit = DEGREES)
    MetadataThesaurusEntry pageRotated();

    @MetadataThesaurusSortIndexOrder(9)
    @MetadataThesaurusEntryAttribute(type = BOOLEAN)
    MetadataThesaurusEntry samePagesFormat();

    @MetadataThesaurusSortIndexOrder(10)
    @MetadataThesaurusEntryAttribute(type = BOOLEAN)
    MetadataThesaurusEntry permissionPrint();

    @MetadataThesaurusSortIndexOrder(11)
    @MetadataThesaurusEntryAttribute(type = BOOLEAN)
    MetadataThesaurusEntry permissionCopy();

    @MetadataThesaurusSortIndexOrder(12)
    @MetadataThesaurusEntryAttribute(type = BOOLEAN)
    MetadataThesaurusEntry permissionChange();

    @MetadataThesaurusSortIndexOrder(13)
    @MetadataThesaurusEntryAttribute(type = BOOLEAN)
    MetadataThesaurusEntry permissionAddNotes();

    MetadataThesaurusEntry form();

    @MetadataThesaurusEntryAttribute(type = BOOLEAN)
    MetadataThesaurusEntry optimized();

    @MetadataThesaurusEntryAttribute(type = BOOLEAN)
    MetadataThesaurusEntry tagged();

    MetadataThesaurusEntry producer();

}
