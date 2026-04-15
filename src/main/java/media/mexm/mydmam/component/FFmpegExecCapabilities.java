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

import static java.util.stream.Collectors.toUnmodifiableMap;
import static tv.hd3g.fflauncher.SimpleSourceTraits.addSineAudioGeneratorAsInputSource;
import static tv.hd3g.fflauncher.SimpleSourceTraits.addSmptehdbarsGeneratorAsInputSource;
import static tv.hd3g.processlauncher.cmdline.Parameters.bulk;

import java.awt.Point;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tv.hd3g.fflauncher.FFmpeg;

@Slf4j
@Component
public class FFmpegExecCapabilities implements InternalService {

    static final String FFMPEG = "ffmpeg";
    static final String BARS1K_MKV = "bars1k.mkv";
    private final ExternalExecCapabilities externalExecCapabilities;

    @Getter
    private Map<String, String> avaliableCodecs;

    public FFmpegExecCapabilities(@Autowired final ExternalExecCapabilities externalExecCapabilities) {
        this.externalExecCapabilities = externalExecCapabilities;
        avaliableCodecs = Map.of();
    }

    @Override
    public void internalServiceStart() throws Exception {
        final var ffmpeg = new FFmpeg(FFMPEG);
        addSmptehdbarsGeneratorAsInputSource(ffmpeg, new Point(640, 360), 1, "25");
        addSineAudioGeneratorAsInputSource(ffmpeg, 1000, 1, 48000);
        ffmpeg.addVideoCodecName("ffv1", -1);
        ffmpeg.addAudioCodecName("pcm_s16le", -1);
        ffmpeg.setOverwriteOutputFiles();
        ffmpeg.addSimpleOutputDestination(BARS1K_MKV);

        externalExecCapabilities.setup(FFMPEG, List.of(ffmpeg.getReadyToRunParameters()));

        final var videoCodecsFamilies = Map.of(
                "h264", List.of(
                        "h264_videotoolbox",
                        "h264_nvenc",
                        "h264_qsv",
                        "h264_v4l2m2m",
                        "h264_vaapi",
                        "h264_amf",
                        "h264_mf",
                        "h264_vulkan",
                        "libx264"),
                "hevc", List.of(
                        "hevc_videotoolbox",
                        "hevc_amf",
                        "hevc_d3d12va",
                        "hevc_mf",
                        "hevc_nvenc",
                        "hevc_qsv",
                        "hevc_vaapi",
                        "hevc_vulkan",
                        "hevc_v4l2m2m",
                        "libx265"),
                "av1", List.of(
                        "av1_nvenc",
                        "av1_qsv",
                        "av1_amf",
                        "av1_mf",
                        "av1_vaapi",
                        "av1_vulkan",
                        "libaom-av1",
                        "librav1e",
                        "libsvtav1"),
                "vp9", List.of(
                        "vp9_vaapi",
                        "vp9_qsv",
                        "libvpx-vp9"));

        final var audioCodecsFamilies = Map.of(
                "opus", List.of(
                        "libopus",
                        "opus"),
                "aac", List.of(
                        "libfdk_aac",
                        "aac"));

        videoCodecsFamilies.values().stream().flatMap(List::stream).forEach(this::makePlaybookVCodec);
        audioCodecsFamilies.values().stream().flatMap(List::stream).forEach(this::makePlaybookACodec);

        externalExecCapabilities.tearDown(FFMPEG);

        final var passing = externalExecCapabilities.getPassingPlaybookNames(FFMPEG);
        log.debug("Activated ffmpeg codecs and options: {}", passing);

        avaliableCodecs = Stream.concat(
                videoCodecsFamilies.entrySet().stream(),
                audioCodecsFamilies.entrySet().stream())
                .filter(entry -> entry.getValue().stream().anyMatch(passing::contains))
                .collect(toUnmodifiableMap(
                        Entry::getKey,
                        entry -> entry.getValue().stream().filter(passing::contains).findFirst().orElseThrow()));

        log.info("Use ffmpeg codecs: {}", avaliableCodecs);
    }

    void makePlaybookVCodec(final String codecName) {
        makePlaybookCodec(codecName, true);
    }

    void makePlaybookACodec(final String codecName) {
        makePlaybookCodec(codecName, false);
    }

    void makePlaybookCodec(final String codecName, final boolean video) {
        final var outFileName = codecName + ".mp4";
        final var codecsOpts = video ? "-codec:v" : "-codec:a";
        final var disableCodec = video ? "-an" : "-vn";

        externalExecCapabilities.addPlaybook(
                FFMPEG,
                codecName,
                bulk("-loglevel error -y -i", BARS1K_MKV,
                        codecsOpts, codecName, disableCodec, outFileName),
                evaluator -> {
                    final var stdErr = evaluator.captured().getStderr(false, "; ");
                    log.trace("Evaluate ffmpeg with {}: {}", codecName, stdErr);
                    return evaluator.haveReturnCode(0) && evaluator.haveFile(outFileName);
                });
    }

    @Override
    public String getInternalServiceName() {
        return getClass().getSimpleName();
    }

}
