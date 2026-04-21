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
package media.mexm.mydmam.component;

import static java.lang.Math.abs;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMillis;
import static java.util.Optional.empty;
import static org.apache.commons.io.FileUtils.readFileToString;
import static tv.hd3g.processlauncher.CapturedStreams.BOTH_STDOUT_STDERR;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.repository.InstanceRepository;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Component
@Slf4j
public class AboutInstance {

    private final ExecutableFinder executableFinder;
    private final InstanceRepository instanceRepository;

    @Getter
    private final String instanceName;
    @Getter
    private final long pid;
    private final AtomicReference<String> hostNameRef;

    public AboutInstance(@Autowired final MyDMAMConfigurationProperties configuration,
                         @Autowired final ExecutableFinder executableFinder,
                         @Autowired final InstanceRepository instanceRepository) {
        this.executableFinder = executableFinder;
        this.instanceRepository = instanceRepository;
        instanceName = configuration.instancename();
        pid = ProcessHandle.current().pid();
        hostNameRef = new AtomicReference<>();

    }

    public String getHostName() {
        /**
         * https://stackoverflow.com/questions/20087173/how-to-do-a-lazy-create-and-set-with-atomicreference-in-a-safe-and-efficient-man
         */
        var hostName = hostNameRef.get();
        if (hostName == null) {
            hostName = Optional.ofNullable(System.getenv().get("COMPUTERNAME"))
                    .or(this::getEtcHostname)
                    .or(this::getCmdHostname)
                    .or(this::getInetAddressHostname)
                    .orElse("localhost");
            if (hostNameRef.compareAndSet(null, hostName) == false) {
                return hostNameRef.get();
            }
        }

        return hostName;
    }

    public long getQueryPingTime() {
        final var appNowBeforeQuery = System.currentTimeMillis();
        instanceRepository.currentTimestamp();
        final var appNowAfterQuery = System.currentTimeMillis();

        final var queryPingTime = appNowAfterQuery - appNowBeforeQuery;

        if (queryPingTime > 30_000l) {
            log.warn("It take {} ms to query database!", queryPingTime);
        } else {
            log.debug("Query database take {} ms", queryPingTime);
        }

        return queryPingTime;
    }

    public long getDatabaseDeltaTime() {
        final var appNowBeforeQuery = System.currentTimeMillis();
        final var dbNow = instanceRepository.currentTimestamp().getTime();

        final var databaseDeltaTime = dbNow - appNowBeforeQuery;
        final var relativeTime = databaseDeltaTime > 0 ? "in past" : "in future";
        if (abs(databaseDeltaTime) > Duration.ofMinutes(30l).toMillis()) {
            log.warn("Big delta time with database: {}, {}", ofMillis(databaseDeltaTime), relativeTime);
        } else {
            log.debug("Delta time with database: {}, {}", ofMillis(abs(databaseDeltaTime)), relativeTime);
        }
        return databaseDeltaTime;
    }

    final Optional<String> getEtcHostname() {
        try {
            final var file = new File("/etc/hostname");// NOSONAR 1075
            final var content = readFileToString(file, UTF_8).trim();
            if (content.isEmpty()) {
                return empty();
            }
            return Optional.ofNullable(content);
        } catch (final IOException e) {
            return empty();
        }
    }

    final Optional<String> getCmdHostname() {
        try {
            final var commandLine = new CommandLine("hostname", Parameters.of(), executableFinder);
            final var processlauncherBuilder = new ProcesslauncherBuilder(commandLine);
            final var capText = new CapturedStdOutErrTextRetention();
            processlauncherBuilder.getSetCaptureStandardOutputAsOutputText(BOTH_STDOUT_STDERR)
                    .addObserver(capText);
            processlauncherBuilder.setExecCodeMustBeZero(true).start().checkExecution();
            final var content = capText.getStdouterr(false, " ").trim();
            if (content.isEmpty()) {
                return empty();
            }
            return Optional.ofNullable(content);
        } catch (final IOException e) {
            return empty();
        }
    }

    final Optional<String> getInetAddressHostname() {
        try {
            return Optional.ofNullable(InetAddress.getLocalHost().getHostName());
        } catch (final UnknownHostException e) {
            return empty();
        }
    }

}
