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

@MetadataThesaurusClassifier(value = "pdf")
public interface MtdThesaurusDefPDF {

    MetadataThesaurusEntry pageCount();

    MetadataThesaurusEntry encrypted();

    MetadataThesaurusEntry pdfVersion();

    MetadataThesaurusEntry form();

    MetadataThesaurusEntry optimized();

    MetadataThesaurusEntry javascript();

    MetadataThesaurusEntry tagged();

    MetadataThesaurusEntry producer();

    MetadataThesaurusEntry keywords();

    MetadataThesaurusEntry permissionPrint();

    MetadataThesaurusEntry permissionCopy();

    MetadataThesaurusEntry permissionChange();

    MetadataThesaurusEntry permissionAddNotes();

    MetadataThesaurusEntry pageWidthMm();

    MetadataThesaurusEntry pageHeightMm();

    MetadataThesaurusEntry pageRotated();

    MetadataThesaurusEntry samePagesFormat();

}
