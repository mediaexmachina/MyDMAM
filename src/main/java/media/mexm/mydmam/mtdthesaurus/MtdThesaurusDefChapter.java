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

import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusClassifierKind.BIBLIOGRAPHIC_RECORD;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryNumericalUnit.MILLISECONDS;

@MetadataThesaurusClassifier(value = "chapter", longname = "Chapter", kind = BIBLIOGRAPHIC_RECORD)
public interface MtdThesaurusDefChapter {

    @MetadataThesaurusSortIndexOrder(1)
    MetadataThesaurusEntry title();

    @MetadataThesaurusSortIndexOrder(2)
    @MetadataThesaurusEntryAttribute(unit = MILLISECONDS)
    MetadataThesaurusEntry startTime();

    @MetadataThesaurusSortIndexOrder(3)
    @MetadataThesaurusEntryAttribute(unit = MILLISECONDS)
    MetadataThesaurusEntry endTime();

}
