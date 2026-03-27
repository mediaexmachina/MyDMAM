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

@Validated
public record EnvConf(@DefaultValue("audittrail") @NotEmpty String auditTrailSpoolName,
                      @DefaultValue("async-api") @NotEmpty String asyncAPISpoolName,
                      @DefaultValue("false") boolean explainSearchResults,
                      @DefaultValue("10000") @Min(0) int resetBatchSizeIndexer,
                      @DefaultValue("100") @Min(1) int dirListMaxSize,
                      @DefaultValue("100") @Min(1) int searchResultMaxSize,
                      @DefaultValue("24h") Duration pendingActivityMaxAgeGraceRestart) {

    public EnvConf {
        if (pendingActivityMaxAgeGraceRestart.isPositive() == false) {
            throw new IllegalStateException("Invalid pendingActivityMaxAgeGraceRestart: "
                                            + pendingActivityMaxAgeGraceRestart);
        }
    }

}
