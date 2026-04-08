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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import media.mexm.mydmam.asset.RenderedFileSpecs;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.tools.AllowBlockLists;

@ConfigurationProperties(prefix = "mydmam")
@Validated
public record MyDMAMConfigurationProperties(@Valid Map<TechnicalName, RealmConf> realms,
                                            @DefaultValue @Valid @NotNull EnvConf env,
                                            @DefaultValue @Valid @NotNull SiteConf site,
                                            String instancename,
                                            @DefaultValue @Valid @NotNull ExternalToolsConf tools,
                                            @DefaultValue @Valid @NotNull AllowBlockLists activityHandlers,
                                            @DefaultValue @Valid @NotNull RenderedFileSpecs renderedSpecs) {

    public MyDMAMConfigurationProperties {
        if (instancename == null || instancename.isEmpty()) {
            try {
                instancename = InetAddress.getLocalHost().getHostName();
            } catch (final UnknownHostException e) {
                throw new IllegalStateException("Can't get hostname", e);
            }
        }
    }

    public Set<String> getRealmNames() {
        try {
            return realms().keySet()
                    .stream()
                    .map(TechnicalName::name)
                    .collect(toUnmodifiableSet());
        } catch (final NullPointerException e) {
            return Set.of();
        }
    }

    public Optional<RealmConf> getRealmByName(final String realmName) {
        requireNonNull(realmName);
        try {
            return Optional.ofNullable(realms().get(new TechnicalName(realmName)));
        } catch (final NullPointerException e) {
            return empty();
        }
    }

    /**
     * Must exists, else thrown an Exception
     */
    public RealmStorageConfiguredEnv getRealmAndStorage(final String realmName, final String storageName) {
        final var realm = getRealmByName(realmName)
                .orElseThrow(() -> new IllegalArgumentException("Can't found realm" + realmName));
        final var storage = realm.getStorageByName(storageName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Can't found storageName" + storageName + " for realm " + realmName));
        return new RealmStorageConfiguredEnv(realmName, storageName, realm, storage);
    }

    public boolean isActivatedActivityHandler(final String realmName, final String handlerName) {
        requireNonNull(realmName);
        requireNonNull(handlerName);

        final var globalActivated = activityHandlers.pass(handlerName);
        if (globalActivated == false) {
            return false;
        }

        return getRealmByName(realmName)
                .map(RealmConf::activityHandlers)
                .map(f -> f.pass(handlerName))
                .orElse(true);
    }

}
