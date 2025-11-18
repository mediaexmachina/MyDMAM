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
 * Copyright (C) Media ex Machina 2025
 *
 */
package media.mexm.mydmam.configuration;

import static java.util.Optional.empty;

import java.util.Optional;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@ConfigurationProperties(prefix = "mydmam")
@Validated
public record MyDMAMConfigurationProperties(@Valid PathIndexingConf pathindexing) {

	@ConstructorBinding
	public MyDMAMConfigurationProperties { // NOSONAR S1186
	}

	public Set<String> getRealmNames() {
		try {
			return pathindexing().realms().keySet();
		} catch (final NullPointerException e) {
			return Set.of();
		}
	}

	public Optional<PathIndexingRealm> getRealmByName(final String realmName) {
		try {
			return Optional.ofNullable(pathindexing().realms().get(realmName));
		} catch (final NullPointerException e) {
			return empty();
		}
	}

}
