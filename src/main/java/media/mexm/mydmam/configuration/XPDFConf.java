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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

@Validated
public record XPDFConf(@DefaultValue("1h") Duration maxExecTime,
                       @DefaultValue(".") @NotEmpty String tempDir,
                       @DefaultValue("100000") @Min(1) @Max(100_000) int maxPageCount,
                       @DefaultValue("200") @Min(10) @Max(10_000) int resolution) {
}
