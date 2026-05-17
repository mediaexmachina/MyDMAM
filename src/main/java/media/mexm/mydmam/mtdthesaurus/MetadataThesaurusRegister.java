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

public interface MetadataThesaurusRegister {

    @MetadataThesaurusSortIndexOrder(1)
    MtdThesaurusDefDublinCore dublinCore();

    @MetadataThesaurusSortIndexOrder(2)
    MtdThesaurusDefDCMI dcmi();

    @MetadataThesaurusSortIndexOrder(3)
    MtdThesaurusDefXMP xmp();

    @MetadataThesaurusSortIndexOrder(4)
    MtdThesaurusDefPDF pdf();

    @MetadataThesaurusSortIndexOrder(5)
    MtdThesaurusDefTechnical technical();

    @MetadataThesaurusSortIndexOrder(6)
    MtdThesaurusDefChapter chapter();

    @MetadataThesaurusSortIndexOrder(7)
    MtdThesaurusDefTechnicalContainer technicalContainer();

    @MetadataThesaurusSortIndexOrder(8)
    MtdThesaurusDefTechnicalStream technicalStream();

    @MetadataThesaurusSortIndexOrder(9)
    MtdThesaurusDefTechnicalImage technicalImage();

    @MetadataThesaurusSortIndexOrder(10)
    MtdThesaurusDefTechnicalVideo technicalVideo();

    @MetadataThesaurusSortIndexOrder(11)
    MtdThesaurusDefTechnicalAudio technicalAudio();

    @MetadataThesaurusSortIndexOrder(12)
    MtdThesaurusDefTechnicalMXF technicalMXF();

    @MetadataThesaurusSortIndexOrder(13)
    MtdThesaurusDefTechnicalTransportStream technicalTransportStream();

}
