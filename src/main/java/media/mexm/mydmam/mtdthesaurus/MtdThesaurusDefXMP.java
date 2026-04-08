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

/**
 * @see https://github.com/adobe/XMP-Toolkit-SDK/blob/main/docs/XMPSpecificationPart1.pdf
 */
@MetadataThesaurusClassifier(value = "xmp")
public interface MtdThesaurusDefXMP {

    MetadataThesaurusEntry createDate();

    MetadataThesaurusEntry creatorTool();

    MetadataThesaurusEntry identifier();

    MetadataThesaurusEntry label();

    MetadataThesaurusEntry metadataDate();

    MetadataThesaurusEntry modifyDate();

    MetadataThesaurusEntry rating();
}
