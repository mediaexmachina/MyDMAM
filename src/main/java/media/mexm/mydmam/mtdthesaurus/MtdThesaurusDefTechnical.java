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

@MetadataThesaurusClassifier(value = "technical")
public interface MtdThesaurusDefTechnical {// TODO split with image/video

    // TODO declare all new Thresaurus

    MetadataThesaurusEntry width();

    MetadataThesaurusEntry height();

    MetadataThesaurusEntry pixelformat();

    MetadataThesaurusEntry colorspace();

    MetadataThesaurusEntry colorrange();

    MetadataThesaurusEntry colorprimaries();

    MetadataThesaurusEntry colortransfer();

    MetadataThesaurusEntry orientation();

    MetadataThesaurusEntry type();

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

    MetadataThesaurusEntry timecode();

    MetadataThesaurusEntry duration();

}
