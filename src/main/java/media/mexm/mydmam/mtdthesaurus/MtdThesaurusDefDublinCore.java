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

@MetadataThesaurusClassifier(value = "dc")
public interface MtdThesaurusDefDublinCore {

    MetadataThesaurusEntry contributor();

    MetadataThesaurusEntry coverage();

    MetadataThesaurusEntry creator();

    MetadataThesaurusEntry date();

    MetadataThesaurusEntry description();

    MetadataThesaurusEntry format();

    MetadataThesaurusEntry identifier();

    MetadataThesaurusEntry language();

    MetadataThesaurusEntry publisher();

    MetadataThesaurusEntry relation();

    MetadataThesaurusEntry rights();

    MetadataThesaurusEntry source();

    MetadataThesaurusEntry subject();

    MetadataThesaurusEntry title();

    MetadataThesaurusEntry type();

}
