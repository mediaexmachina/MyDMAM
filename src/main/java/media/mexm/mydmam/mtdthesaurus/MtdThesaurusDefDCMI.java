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

@MetadataThesaurusClassifier(value = "dc:mi")
public interface MtdThesaurusDefDCMI {

    MetadataThesaurusEntry _abstract();// NOSONAR

    MetadataThesaurusEntry accessRights();

    MetadataThesaurusEntry accrualMethod();

    MetadataThesaurusEntry accrualPeriodicity();

    MetadataThesaurusEntry accrualPolicy();

    MetadataThesaurusEntry alternative();

    MetadataThesaurusEntry audience();

    MetadataThesaurusEntry available();

    MetadataThesaurusEntry bibliographicCitation();

    MetadataThesaurusEntry conformsTo();

    MetadataThesaurusEntry created();

    MetadataThesaurusEntry dateAccepted();

    MetadataThesaurusEntry dateCopyrighted();

    MetadataThesaurusEntry dateSubmitted();

    MetadataThesaurusEntry educationLevel();

    MetadataThesaurusEntry extent();

    MetadataThesaurusEntry hasFormat();

    MetadataThesaurusEntry hasPart();

    MetadataThesaurusEntry hasVersion();

    MetadataThesaurusEntry instructionalMethod();

    MetadataThesaurusEntry isFormatOf();

    MetadataThesaurusEntry isPartOf();

    MetadataThesaurusEntry isReferencedBy();

    MetadataThesaurusEntry isReplacedBy();

    MetadataThesaurusEntry isRequiredBy();

    MetadataThesaurusEntry issued();

    MetadataThesaurusEntry isVersionOf();

    MetadataThesaurusEntry license();

    MetadataThesaurusEntry mediator();

    MetadataThesaurusEntry medium();

    MetadataThesaurusEntry modified();

    MetadataThesaurusEntry provenance();

    MetadataThesaurusEntry references();

    MetadataThesaurusEntry replaces();

    MetadataThesaurusEntry requires();

    MetadataThesaurusEntry rightsHolder();

    MetadataThesaurusEntry spatial();

    MetadataThesaurusEntry tableOfContents();

    MetadataThesaurusEntry temporal();

    MetadataThesaurusEntry valid();
}
