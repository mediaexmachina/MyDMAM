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
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryNumericalUnit.PIXELS;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryType.IDENTIFIER_OR_SERIAL_ID;

@MetadataThesaurusClassifier(value = "technical:image", longname = "Image (technical)", kind = TECHNICAL_ATTRIBUTE)
public interface MtdThesaurusDefTechnicalImage {

    @MetadataThesaurusSortIndexOrder(1)
    @MetadataThesaurusEntryAttribute(type = IDENTIFIER_OR_SERIAL_ID)
    MetadataThesaurusEntry referenceId();

    @MetadataThesaurusSortIndexOrder(2)
    @MetadataThesaurusEntryAttribute(unit = PIXELS)
    MetadataThesaurusEntry width();

    @MetadataThesaurusSortIndexOrder(3)
    @MetadataThesaurusEntryAttribute(unit = PIXELS)
    MetadataThesaurusEntry height();

    MetadataThesaurusEntry pixelformat();

    MetadataThesaurusEntry colorspace();

    MetadataThesaurusEntry colorrange();

    MetadataThesaurusEntry colorprimaries();

    MetadataThesaurusEntry colortransfer();

    MetadataThesaurusEntry orientation();

    /**
     * 1.777
     */
    MetadataThesaurusEntry aspectRatio();

    /**
     * 1:1
     */
    MetadataThesaurusEntry sampleAspectRatio();

    /**
     * 16:9
     */
    MetadataThesaurusEntry displayAspectRatio();

    /**
     * @see ImageAspectRatioDetectionActivity.PageOrientation
     */
    MetadataThesaurusEntry imageAspectFormat();

}
