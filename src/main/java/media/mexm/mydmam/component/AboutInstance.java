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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static org.apache.commons.io.FileUtils.readFileToString;
import static tv.hd3g.processlauncher.CapturedStreams.BOTH_STDOUT_STDERR;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Component
public class AboutInstance {

    private final ExecutableFinder executableFinder;

    @Getter
    private final String instanceName;
    @Getter
    private final long pid;
    @Getter
    private final String hostName;

    public AboutInstance(@Autowired final MyDMAMConfigurationProperties configuration,
                         @Autowired final ExecutableFinder executableFinder) {
        this.executableFinder = executableFinder;
        instanceName = configuration.instancename();
        pid = ProcessHandle.current().pid();
        hostName = Optional.ofNullable(System.getenv().get("COMPUTERNAME"))
                .or(this::getEtcHostname)
                .or(this::getCmdHostname)
                .or(this::getInetAddressHostname)
                .orElse("localhost");
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
