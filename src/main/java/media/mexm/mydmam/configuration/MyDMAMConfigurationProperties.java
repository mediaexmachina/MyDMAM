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

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toUnmodifiableSet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

@ConfigurationProperties(prefix = "mydmam")
@Validated
public record MyDMAMConfigurationProperties(@Valid InfraConf infra,
											String instancename,
											@DefaultValue("audittrail") @NotEmpty String auditTrailSpoolName,
											@DefaultValue("false") boolean explainSearchResults,
											@DefaultValue("10000") @Min(0) int resetBatchSizeIndexer,
											@DefaultValue("100") @Min(1) int dirListMaxSize,
											@DefaultValue("100") @Min(1) int searchResultMaxSize,
											@DefaultValue("24h") Duration pendingActivityMaxAgeGraceRestart) {

	public MyDMAMConfigurationProperties {
		if (instancename == null || instancename.isEmpty()) {
			try {
				instancename = InetAddress.getLocalHost().getHostName();
			} catch (final UnknownHostException e) {
				throw new IllegalStateException("Can't get hostname", e);
			}
		}
		if (pendingActivityMaxAgeGraceRestart.isPositive() == false) {
			throw new IllegalStateException("Invalid pendingActivityMaxAgeGraceRestart: "
											+ pendingActivityMaxAgeGraceRestart);
		}
	}

	public Set<String> getRealmNames() {
		try {
			return infra().realms().keySet()
					.stream()
					.map(TechnicalName::name)
					.collect(toUnmodifiableSet());
		} catch (final NullPointerException e) {
			return Set.of();
		}
	}

	public Optional<PathIndexingRealm> getRealmByName(final String realmName) {
		requireNonNull(realmName);
		try {
			return Optional.ofNullable(infra().realms().get(new TechnicalName(realmName)));
		} catch (final NullPointerException e) {
			return empty();
		}
	}

}
