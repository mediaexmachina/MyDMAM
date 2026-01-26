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

/**
 * @see policy.xml from ImageMagick
 */
@Validated
public record MagickConf(@DefaultValue("1h") Duration maxExecTime,
						 @DefaultValue(".config/ImageMagick") @NotEmpty String confDir,
						 @DefaultValue(".") @NotEmpty String tempDir,
						 @DefaultValue("0") @Min(0) int maxThreadCount,
						 /**
						  * In MiB
						  */
						 @DefaultValue("128") @Min(1) int maxMap,
						 /**
						  * In MiB
						  */
						 @DefaultValue("256") @Min(1) int maxMemory,
						 /**
						  * In MiB
						  */
						 @DefaultValue("256") @Min(1) int maxMemoryRequest,
						 /**
						  * In MiB
						  */
						 @DefaultValue("1024") @Min(1) int maxDisk,
						 /**
						  * In pixels
						  */
						 @DefaultValue("8000") @Min(1) int maxWidth,
						 /**
						  * In pixels
						  */
						 @DefaultValue("8000") @Min(1) int maxHeight) {

}
