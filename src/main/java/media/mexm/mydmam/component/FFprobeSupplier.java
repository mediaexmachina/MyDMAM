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
package media.mexm.mydmam.component;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static tv.hd3g.processlauncher.cmdline.Parameters.bulk;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import tv.hd3g.fflauncher.FFprobe;
import tv.hd3g.fflauncher.processingtool.FFSourceDefinition;
import tv.hd3g.fflauncher.recipes.ProbeMedia;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.processingtool.KeepStdoutAndErrToLogWatcher;
import tv.hd3g.processlauncher.processingtool.ProcessingToolResult;

@Slf4j
@Component
public class FFprobeSupplier implements InternalService {

    public static final String FFPROBE = "ffprobe";
    private static final String START_FFPROBE_ON = "Start ffprobe on {}";

    private final ExternalExecCapabilities externalExecCapabilities;
    private final ExecutableFinder executableFinder;
    private final ScheduledExecutorService maxExecTimeScheduler;
    private final MyDMAMConfigurationProperties configuration;

    @Getter
    private boolean enabled;

    public FFprobeSupplier(@Autowired final ExternalExecCapabilities externalExecCapabilities,
                           @Autowired final ExecutableFinder executableFinder,
                           @Autowired final ScheduledExecutorService maxExecTimeScheduler,
                           @Autowired final MyDMAMConfigurationProperties configuration) {
        this.externalExecCapabilities = externalExecCapabilities;
        this.executableFinder = executableFinder;
        this.maxExecTimeScheduler = maxExecTimeScheduler;
        this.configuration = configuration;
        enabled = false;
    }

    @Override
    public String getInternalServiceName() {
        return FFPROBE;
    }

    @Override
    public void internalServiceStart() throws Exception {
        externalExecCapabilities.addPlaybook(
                FFPROBE,
                "run",
                bulk("-version"),
                evaluator -> {
                    if (evaluator.haveReturnCode(0) == false
                        || evaluator.haveStringInStdOutErr("ffprobe version") == false) {
                        log.error("Can't run ffprobe: {}", evaluator.captured().getStdouterr(false, "|"));
                        return false;
                    }
                    log.info("Detect ffprobe {}", evaluator.captured().getStdouterrLines(false).findFirst()
                            .orElseThrow());
                    return true;
                });

        externalExecCapabilities.tearDown(FFPROBE);
        enabled = externalExecCapabilities.getPassingPlaybookNames(FFPROBE).contains("run");
    }

    private void checkEnabled() {
        if (enabled == false) {
            throw new IllegalStateException(FFPROBE + " is disabled");
        }
    }

    protected ProbeMedia makeProbeMedia() {
        return new ProbeMedia(FFPROBE, maxExecTimeScheduler);
    }

    public final ProcessingToolResult<FFSourceDefinition, FFprobe, FFprobeJAXB, KeepStdoutAndErrToLogWatcher> processSimpleContainerAnalysis(final File source) {
        checkEnabled();
        final var probeMedia = makeProbeMedia();
        probeMedia.setExecutableFinder(executableFinder);

        final var maxExecTime = configuration.tools().ffprobeSimpleContainerAnalysisMaxExecTime();
        probeMedia.setMaxExecutionTime(maxExecTime, maxExecTimeScheduler);
        try {
            probeMedia.setWorkingDirectory(source.getParentFile());
        } catch (final IOException e) {
            throw new UncheckedIOException("Can't set workingDirectory", e);
        }
        log.debug(START_FFPROBE_ON, source.getAbsolutePath());
        return probeMedia.process(source.getName());
    }

    public static final Map<String, String> WELL_KNOWN_CODECS_NAMES = Map.ofEntries(// XXX to FFprobeJAXB
            Map.entry("dvvideo", "DV"),
            Map.entry("dvcp", "DV/DVCPro"),
            Map.entry("dv5p", "DVCPro 50"),
            Map.entry("avc1", "h264"),
            Map.entry("mpeg2video", "MPEG2"),
            Map.entry("mx5p", "MPEG2/4:2:2"),
            Map.entry("wmv3", "WMV9"),
            Map.entry("wmav2", "WMA9"),
            Map.entry("apch", "Apple ProRes 422 HQ"),
            Map.entry("apcn", "Apple ProRes 422"),
            Map.entry("apcs", "Apple ProRes 422 LT"),
            Map.entry("apco", "Apple ProRes 422 Proxy"),
            Map.entry("ap4h", "Apple ProRes 4444"),
            Map.entry("mp2", "MPEG/L2"));

    public static final Set<String> VIDEO_MIME_TYPES = Set.of(
            "application/gxf",
            "application/lxf",
            "application/mxf",
            "video/mp2t",
            "video/mp4",
            "video/mpeg",
            "video/quicktime",
            "video/x-dv",
            "video/vc1",
            "video/ogg",
            "video/webm",
            "video/x-matroska",
            "video/mp2p",
            "video/h264",
            "video/x-flv",
            "video/3gpp",
            "video/x-ms-wmv",
            "video/msvideo");

    public static final Set<String> AUDIO_MIME_TYPES = Set.of(
            "audio/x-wav",
            "audio/ac3",
            "audio/mp4",
            "audio/mpeg",
            "audio/ogg",
            "audio/vorbis",
            "audio/webm",
            "audio/quicktime",
            "application/mxf",
            "audio/x-ms-wmv",
            "audio/x-ms-wma",
            "audio/x-hx-aac-adts",
            "audio/3gpp",
            "audio/amr",
            "audio/amr-wb",
            "audio/amr-wb+",
            "audio/eac3",
            "audio/speex",
            "audio/g719",
            "audio/g722",
            "audio/g7221",
            "audio/g723",
            "audio/g726-16",
            "audio/g726-24",
            "audio/g726-32",
            "audio/g726-40",
            "audio/g728",
            "audio/g729",
            "audio/g7291",
            "audio/g729d",
            "audio/g729e",
            "audio/gsm",
            "audio/vnd.dolby.heaac.1",
            "audio/vnd.dolby.heaac.2",
            "audio/vnd.dolby.mlp",
            "audio/vnd.dolby.mps",
            "audio/vnd.dolby.pl2",
            "audio/vnd.dolby.pl2x",
            "audio/vnd.dolby.pl2z",
            "audio/vnd.dolby.pulse.1",
            "audio/vnd.dra",
            "audio/vnd.dts",
            "audio/vnd.dts.hd");

    public static final Set<String> ALL_MIME_TYPES = Stream.concat(
            VIDEO_MIME_TYPES.stream(),
            AUDIO_MIME_TYPES.stream())
            .collect(toUnmodifiableSet());

}
