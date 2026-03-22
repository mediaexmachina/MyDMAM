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
package media.mexm.mydmam.asset;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Validated
public record RenderedFileSpecs(
                                /**
                                 * Imagemagick command lines
                                 */
                                @DefaultValue @Valid @NotNull ThumbnailIMCmd thumbnail) {

    @Validated
    public record ThumbnailIMCmd(
                                 @DefaultValue("""
                                         <%INPUTFILE%>[0] -thumbnail 1024x1024
                                         -profile <%ICCPROFILE%>
                                         -strip -density 72x72 -units PixelsPerInch -interlace plane -sampling-factor 2:1:1 -quality 100
                                         <%OUTPUTFILE%>
                                         """) @NotEmpty String heroCmd,
                                 @DefaultValue("""
                                         <%INPUTFILE%>[0] -thumbnail 256x256
                                         -profile <%ICCPROFILE%>
                                         -strip -density 72x72 -units PixelsPerInch -interlace plane -sampling-factor 2:1:1 -quality 95
                                         <%OUTPUTFILE%>
                                         """) @NotEmpty String cartridgeCmd,
                                 @DefaultValue("""
                                         <%INPUTFILE%>[0] -thumbnail 64x64
                                         -profile <%ICCPROFILE%>
                                         -strip -density 72x72 -units PixelsPerInch -interlace plane -sampling-factor 4:1:1 -quality 95
                                         <%OUTPUTFILE%>
                                         """) @NotEmpty String iconCmd,
                                 @DefaultValue("""
                                         <%INPUTFILE%>[0] -thumbnail 1024x1024
                                         -profile <%ICCPROFILE%>
                                         null: ( -size 2048x2048 tile:pattern:checkerboard -brightness-contrast 40x10 ) -compose Dst_Over -layers composite
                                         -strip -density 72x72 -units PixelsPerInch -interlace plane -sampling-factor 2:1:1 -quality 100
                                         <%OUTPUTFILE%>
                                         """) @NotEmpty String heroAlphaCmd,
                                 @DefaultValue("""
                                         <%INPUTFILE%>[0] -thumbnail 256x256
                                         -profile <%ICCPROFILE%>
                                         null: ( -size 512x512 tile:pattern:checkerboard -brightness-contrast 40x10 ) -compose Dst_Over -layers composite
                                         -strip -density 72x72 -units PixelsPerInch -interlace plane -sampling-factor 2:1:1 -quality 95
                                         <%OUTPUTFILE%>
                                         """) @NotEmpty String cartridgeAlphaCmd,
                                 @DefaultValue("""
                                         <%INPUTFILE%>[0] -thumbnail 64x64
                                         -profile <%ICCPROFILE%>
                                         null: ( -size 128x128 tile:pattern:checkerboard -brightness-contrast 40x10 ) -compose Dst_Over -layers composite
                                         -strip -density 72x72 -units PixelsPerInch -interlace plane -sampling-factor 4:1:1 -quality 95
                                         <%OUTPUTFILE%>
                                         """) @NotEmpty String iconAlphaCmd) {

        public ThumbnailIMCmd {
            heroCmd = fixNL(heroCmd);
            cartridgeCmd = fixNL(cartridgeCmd);
            iconCmd = fixNL(iconCmd);
            heroAlphaCmd = fixNL(heroAlphaCmd);
            cartridgeAlphaCmd = fixNL(cartridgeAlphaCmd);
            iconAlphaCmd = fixNL(iconAlphaCmd);
        }

    }

    static String fixNL(final String lines) {
        return lines.lines()
                .map(String::trim)
                .filter(not(String::isEmpty))
                .collect(joining(" "));
    }
}
