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
package media.mexm.mydmam.configuration;

import java.time.Duration;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Validated
public record EnvConf(@DefaultValue("1h") @NotNull Duration timeBetweenScans,
                      @DefaultValue("pathindexing") @NotEmpty String spoolEvents,
                      @DefaultValue("audittrail") @NotEmpty String auditTrailSpoolName,
                      @DefaultValue("async-api") @NotEmpty String asyncAPISpoolName,
                      @DefaultValue("false") boolean explainSearchResults,
                      @DefaultValue("100") @Min(1) int dirListMaxSize,
                      @DefaultValue("100") @Min(1) int searchResultMaxSize,
                      @DefaultValue("24h") Duration pendingActivityMaxAgeGraceRestart) {

    public EnvConf {
        if (pendingActivityMaxAgeGraceRestart.isPositive() == false) {
            throw new IllegalArgumentException("Invalid pendingActivityMaxAgeGraceRestart: "
                                               + pendingActivityMaxAgeGraceRestart);
        }
        if (timeBetweenScans == Duration.ZERO || timeBetweenScans.isNegative()) {
            throw new IllegalArgumentException("Invalid mockTimeBetweenScans=" + timeBetweenScans);
        }
    }

}
