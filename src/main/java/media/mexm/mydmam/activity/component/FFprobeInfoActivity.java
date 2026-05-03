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
package media.mexm.mydmam.activity.component;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.BASE_PREVIEW;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.aspectRatio;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.getPageOrientation;
import static org.apache.commons.io.FileUtils.write;
import static tv.hd3g.processlauncher.cmdline.Parameters.bulk;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.ActivityLimitPolicy;
import media.mexm.mydmam.component.ExternalExecCapabilities;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefChapter;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefDublinCore;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnical;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalTransportStream;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.service.MediaAssetService;
import media.mexm.mydmam.service.MetadataThesaurusService;
import tv.hd3g.fflauncher.recipes.ProbeMedia;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.ffprobejaxb.FFprobeReference;
import tv.hd3g.ffprobejaxb.data.FFProbeKeyValue;
import tv.hd3g.ffprobejaxb.data.FFProbeStream;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Slf4j
@Component
public class FFprobeInfoActivity implements ActivityHandler { // TODO test

    private static final String FFPROBE = "ffprobe";
    private static final String AUDIO_SLASH = "audio/";
    private static final String VIDEO_SLASH = "video/";
    @Autowired
    MediaAssetService mediaAssetService;
    @Autowired
    MetadataThesaurusService metadataThesaurusService;
    @Autowired
    ExternalExecCapabilities externalExecCapabilities;
    @Autowired
    ScheduledExecutorService maxExecTimeScheduler;
    @Autowired
    ExecutableFinder executableFinder;

    @Deprecated
    private Set<String> passingPlaybookNames;

    // TODO create an FFprobe component to separate logics

    // TODO display and download from front, like identify "ffprobe-base" >> "ffprobe.xml"

    @Override
    public boolean isEnabled() {
        if (passingPlaybookNames == null) {
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
            passingPlaybookNames = externalExecCapabilities.getPassingPlaybookNames(FFPROBE);
        }
        return passingPlaybookNames.contains("run");
    }

    @Override
    public String getHandlerName() {
        return "ffprobe-info";
    }

    @Override
    public String getMetadataOriginName() {
        return FFPROBE;
    }

    @Override
    public ActivityLimitPolicy getLimitPolicy() {
        return FILE_INFORMATION;
    }

    private static final Map<String, String> WELL_KNOWN_CODECS_NAMES = Map.ofEntries(
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

    private static final Set<String> VIDEO_MIME_TYPES = Set.of(
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

    private static final Set<String> AUDIO_MIME_TYPES = Set.of(
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

    private static final Set<String> ALL_MIME_TYPES = Stream.concat(
            VIDEO_MIME_TYPES.stream(),
            AUDIO_MIME_TYPES.stream())
            .collect(toUnmodifiableSet());

    @Override
    public boolean canHandle(final FileEntity fileEntity,
                             final ActivityEventType eventType,
                             final RealmStorageConfiguredEnv storedOn) {
        return storedOn.isDAS()
               && metadataThesaurusService.getMimeType(fileEntity)
                       .map(ALL_MIME_TYPES::contains)
                       .orElse(false);
    }

    @Override
    public void handle(final FileEntity fileEntity,
                       final ActivityEventType eventType,
                       final RealmStorageConfiguredEnv storedOn) throws Exception {
        final var assetFile = storedOn.getLocalInternalFile(fileEntity);
        final var probeMedia = new ProbeMedia(FFPROBE, maxExecTimeScheduler);
        probeMedia.setExecutableFinder(executableFinder);
        probeMedia.setWorkingDirectory(assetFile.getParentFile());
        final var ffprobeJAXB = probeMedia.process(assetFile.getName()).getResult();

        saveFFprobeXMLFile(fileEntity, storedOn, ffprobeJAXB);

        final var thesaurus = metadataThesaurusService.getThesaurus(this, fileEntity);
        final var technical = thesaurus.technical();
        final var technicalImage = thesaurus.technicalImage();
        final var technicalMXF = thesaurus.technicalMXF();
        final var technicalTransportStream = thesaurus.technicalTransportStream();
        final var chapter = thesaurus.chapter();
        final var xmp = thesaurus.xmp();
        final var dublinCore = thesaurus.dublinCore();

        setMediaSummary(ffprobeJAXB, technical);
        setChapters(ffprobeJAXB, chapter);

        final var programIdByMediaStreamIndex = getPrograms(ffprobeJAXB, technicalTransportStream);

        ffprobeJAXB.getStreams().forEach(mediaStream -> {
            if ("tmcd".equals(mediaStream.codecTagString())) {
                /**
                 * Quicktime timecode track
                 */
                return;
            }

            final var layer = mediaStream.index();
            final var codecType = Objects.requireNonNull(mediaStream.codecType(), "No codec type, invalid FFprobe XML");

            final var technicalStream = thesaurus.technicalStream();
            technicalStream.type().set(layer, codecType);
            technicalStream.timeBase().set(layer, mediaStream.timeBase());
            technicalStream.referenceId().set(layer, mediaStream.id());
            technicalStream.programId().set(layer, programIdByMediaStreamIndex.get(mediaStream.index()));
            technicalStream.bitrate().set(layer, mediaStream.bitRate());
            technicalStream.profile().set(layer, mediaStream.profile());
            technicalStream.startTime().set(layer, mediaStream.startTime());
            technicalStream.codec().set(layer, mediaStream.codecName());

            final var codecLongName = WELL_KNOWN_CODECS_NAMES.getOrDefault(
                    mediaStream.codecName(),
                    mediaStream.codecLongName());
            technicalStream.codecName().set(layer, codecLongName);

            final var level = mediaStream.level();
            if (level > 0) {
                technicalStream.level().set(layer, mediaStream.level());
            }

            if (codecType.equals("audio")) {
                final var technicalAudio = thesaurus.technicalAudio();
                technicalAudio.channelLayout().set(layer, mediaStream.channelLayout());
                technicalAudio.channelsCount().set(layer, mediaStream.channels());
                technicalAudio.sampleRate().set(layer, mediaStream.sampleRate());
                technicalAudio.sampleFormat().set(layer, mediaStream.sampleFmt());
            }

            if (codecType.equals("video")) {
                final var width = mediaStream.width();
                final var height = mediaStream.height();

                technicalImage.width().set(layer, width);
                technicalImage.height().set(layer, height);

                technicalImage.pixelformat().set(layer, mediaStream.pixFmt());
                technicalImage.colorspace().set(layer, removeUnknown(mediaStream.colorSpace()));
                technicalImage.colorrange().set(layer, removeUnknown(mediaStream.colorRange()));
                technicalImage.colorprimaries().set(layer, removeUnknown(mediaStream.colorPrimaries()));
                technicalImage.colortransfer().set(layer, removeUnknown(mediaStream.colorTransfer()));

                technicalImage.sampleAspectRatio().set(layer, mediaStream.sampleAspectRatio());
                technicalImage.displayAspectRatio().set(layer, mediaStream.displayAspectRatio());
                technicalImage.aspectRatio().set(aspectRatio(width, height));
                technicalImage.imageAspectFormat().set(getPageOrientation(width, height));

                final var technicalVideo = thesaurus.technicalVideo();
                technicalVideo.fieldOrder().set(layer, mediaStream.fieldOrder());
                technicalVideo.frameRate().set(layer, mediaStream.rFrameRate());
                technicalVideo.averageFrameRate().set(layer, mediaStream.avgFrameRate());
            }

            Optional.ofNullable(mediaStream.disposition())
                    .ifPresentOrElse(
                            disposition -> technicalStream.isSecondary()
                                    .set(layer,
                                            disposition.attachedPic()
                                                || disposition.stillImage()
                                                || disposition.timedThumbnails()),
                            () -> technicalStream.isSecondary()
                                    .set(layer, false));

            technicalMXF.trackName().set(layer, getTagByName(mediaStream.tags(), "track_name"));
            technicalMXF.filePackageUMID().set(layer, getTagByName(mediaStream.tags(), "file_package_umid"));
            technicalMXF.filePackageName().set(layer, getTagByName(mediaStream.tags(), "file_package_name"));
            dublinCore.language().set(layer, getTagByName(mediaStream.tags(), "language"));
        });

        ffprobeJAXB.getFormat().ifPresent(format -> {
            final var layer = -1;

            final var technicalContainer = thesaurus.technicalContainer();
            technicalContainer.timecode().set(ffprobeJAXB.getTimecode(false));
            technicalContainer.duration().set(ffprobeJAXB.getDuration());
            technicalContainer.bitrate().set(format.bitRate());
            technicalContainer.format().set(format.formatName());
            technicalContainer.formatName().set(format.formatLongName());
            technicalContainer.startTime().set(format.startTime());

            final var oModificationDate = getTagByName(format.tags(), "modification_date");
            final var oCreationDate = getTagByName(format.tags(), "creation_time");

            xmp.modifyDate().setDateISO8601(oModificationDate);
            xmp.createDate().setDateISO8601(oCreationDate);
            dublinCore.date().setDateISO8601(oModificationDate.or(() -> oCreationDate));

            final var creatorTool = new StringBuilder();
            getTagByName(format.tags(), "product_name").ifPresent(creatorTool::append);
            getTagByName(format.tags(), "product_version").ifPresent(v -> creatorTool.append(" v" + v));
            getTagByName(format.tags(), "company_name").ifPresent(cn -> creatorTool.append(" (" + cn + ")"));
            xmp.creatorTool().set(creatorTool.toString().trim());

            technicalMXF.operationalPatternUL().set(layer, getTagByName(format.tags(), "operational_pattern_ul"));
            technicalMXF.uid().set(layer, getTagByName(format.tags(), "uid"));
            technicalMXF.generationUID().set(layer, getTagByName(format.tags(), "generation_uid"));
            technicalMXF.materialPackageUMID().set(layer, getTagByName(format.tags(), "material_package_umid"));
            dublinCore.language().set(layer, getTagByName(format.tags(), "language"));
        });

        final var validVideoStreams = filterValidVideoStreams(ffprobeJAXB).toList();
        final var currentMimeType = metadataThesaurusService.getMimeType(fileEntity).orElseThrow();
        final var haveVideo = validVideoStreams.isEmpty() == false;
        final var haveAudio = ffprobeJAXB.getAudioStreams().count() > 0l;

        patchInvalidAVMimeTypes(dublinCore, currentMimeType, haveVideo, haveAudio);
    }

    static Map<Integer, Integer> getPrograms(final FFprobeJAXB ffprobeJAXB,
                                             final MtdThesaurusDefTechnicalTransportStream tsWriter) {
        final var result = new HashMap<Integer, Integer>();
        final var programs = ffprobeJAXB.getPrograms();
        if (programs.isEmpty() == false) {
            return Map.of();
        }

        programs.forEach(program -> {
            final var layer = program.programId();
            tsWriter.programNum().set(layer, program.programNum());
            tsWriter.pcrPid().set(layer, program.pcrPid());
            tsWriter.pmtPid().set(layer, program.pmtPid());
            tsWriter.serviceName().set(layer, getTagByName(program.tags(), "service_name"));
            tsWriter.serviceProvider().set(layer, getTagByName(program.tags(), "service_provider"));
            program.streams().forEach(mediaStream -> result.put(
                    mediaStream.index(),
                    program.programId()));
        });
        return unmodifiableMap(result);
    }

    static void setChapters(final FFprobeJAXB ffprobeJAXB,
                            final MtdThesaurusDefChapter chapterMtd) {
        final var chapters = ffprobeJAXB.getChapters();
        if (chapters.isEmpty()) {
            return;
        }

        for (var pos = 0; pos < chapters.size(); pos++) {
            final var chapter = chapters.get(pos);
            chapterMtd.title().set(pos + 1, getTagByName(chapter.tags(), "title"));
            chapterMtd.startTime().set(pos + 1, Math.round(chapter.startTime() * 1000));
            chapterMtd.endTime().set(pos + 1, Math.round(chapter.endTime() * 1000));
        }
    }

    static Optional<String> removeUnknown(final String value) {
        if ("unknown".equalsIgnoreCase(value)) {
            return empty();
        }
        return Optional.ofNullable(value);
    }

    static Optional<String> getTagByName(final List<FFProbeKeyValue> tags, final String name) {
        return tags.stream()
                .filter(t -> name.equalsIgnoreCase(t.key()))
                .findFirst()
                .map(FFProbeKeyValue::value);
    }

    void saveFFprobeXMLFile(final FileEntity fileEntity,
                            final RealmStorageConfiguredEnv storedOn,
                            final FFprobeJAXB ffprobeJAXB) throws IOException {
        if (storedOn.haveWorkingDir()
            && storedOn.haveRenderedDir()
            && storedOn.getActivityLimitPolicy().isLevelLowerThan(BASE_PREVIEW) == false) {
            final var workingFile = storedOn.makeWorkingFile("ffprobe.xml", fileEntity);

            log.debug("Write and save ffprobe XML {} from {}", workingFile, fileEntity);
            write(workingFile, ffprobeJAXB.getXmlContent(), UTF_8);
            mediaAssetService.declareRenderedStaticFile(
                    fileEntity, workingFile, "ffprobe.xml", true, 0, "ffprobe-base");
        }
    }

    static void patchInvalidAVMimeTypes(final MtdThesaurusDefDublinCore dublinCore, // TODO not dublinCore
                                        final String currentMimeType,
                                        final boolean haveVideo,
                                        final boolean haveAudio) {
        if (currentMimeType.startsWith(VIDEO_SLASH) && haveVideo == false && haveAudio) {
            dublinCore.format().set(currentMimeType.replace(VIDEO_SLASH, AUDIO_SLASH));
        } else if (currentMimeType.startsWith(AUDIO_SLASH) && haveVideo == true) {
            dublinCore.format().set(currentMimeType.replace(AUDIO_SLASH, VIDEO_SLASH));
        }
    }

    static Stream<FFProbeStream> filterValidVideoStreams(final FFprobeReference ffprobe) {
        return ffprobe.getVideoStreams()
                .filter(s -> s.width() > 0 && s.height() > 0)
                .filter(s -> {
                    if (s.disposition() != null) {
                        if (s.disposition().attachedPic()
                            || s.disposition().timedThumbnails()) {
                            return false;
                        }
                        if (s.disposition().forced()) {
                            return true;
                        }
                    }
                    return true;
                });
    }

    static void setMediaSummary(final FFprobeJAXB ffprobeJAXB,
                                final MtdThesaurusDefTechnical writer) {
        final var mediaSummary = ffprobeJAXB.getMediaSummary();
        if (mediaSummary.format().isEmpty() == false) {
            writer.type().set(-1, mediaSummary.format());
        }
        final var mediaSummaryStreams = mediaSummary.streams();
        for (var pos = 0; pos < mediaSummaryStreams.size(); pos++) {
            final var mediaSummaryForStream = mediaSummaryStreams.get(pos);
            if (mediaSummaryForStream.isEmpty() == false) {
                writer.type().set(pos, mediaSummaryForStream);
            }
        }
    }

    private static final Set<String> MASTER_AS_PREVIEW_MIME_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp4",
            "video/mp4",
            "audio/quicktime",
            "video/quicktime",
            "video/webm",
            "audio/webm",
            "audio/x-wav",
            "audio/ogg",
            "audio/vorbis",
            "audio/3gpp",
            "audio/amr",
            "audio/amr-wb",
            "audio/amr-wb+",
            "audio/speex",
            "audio/g722",
            "audio/g7221",
            "audio/g723",
            "audio/g726-16",
            "audio/g726-24",
            "audio/g726-32",
            "audio/g726-40",
            "audio/g729",
            "audio/g7291",
            "audio/g729d",
            "audio/g729e",
            "audio/gsm");

    private static final Set<String> MASTER_AS_PREVIEW_VIDEO_CODECS = Set.of(
            "h264",
            "hevc",
            "vp8",
            "vp9",
            "av1",
            "mpeg4");

    private static final Set<String> MASTER_AS_PREVIEW_AUDIO_CODECS = Set.of(
            "aac",
            "adpcm_g722",
            "adpcm_g726",
            "adpcm_g726le",
            "amr_nb",
            "amr_wb",
            "g723_1",
            "g729",
            "gsm",
            "ilbc",
            "mp2",
            "mp3",
            "opus",
            "pcm_f16le",
            "pcm_f24le",
            "pcm_f32be",
            "pcm_f32le",
            "pcm_s16be",
            "pcm_s16le",
            "pcm_s24be",
            "pcm_s24le",
            "pcm_s32be",
            "pcm_s32le",
            "pcm_s8",
            "speex",
            "vorbis");

    boolean isCanBeUsedInMasterAsPreview(final String mimeType, final FFprobeJAXB ffprobeJAXB) {
        if (MASTER_AS_PREVIEW_MIME_TYPES.contains(mimeType) == false) {
            return false;
        }

        final var oVideoStream = ffprobeJAXB.getFirstVideoStream();
        final var oAudioStream = ffprobeJAXB.getAudioStreams().findFirst();
        if (oVideoStream.isEmpty() && oAudioStream.isEmpty()) {
            return false;
        }

        if (oVideoStream.isPresent()) {
            final var videoStream = oVideoStream.get();
            if (videoStream.bitRate() > 8_000_000
                || videoStream.width() > 1920
                || videoStream.height() > 1080
                || Optional.ofNullable(videoStream.sampleAspectRatio()).orElse("1:1").equals("1:1") == false
                || Optional.ofNullable(videoStream.fieldOrder()).orElse("progressive").equals("progressive") == false
                || MASTER_AS_PREVIEW_VIDEO_CODECS.contains(videoStream.codecName()) == false) {
                return false;
            }
        }

        if (oAudioStream.isPresent()) {
            final var audioStream = oAudioStream.get();
            if (audioStream.sampleRate() > 48000
                || audioStream.channels() > 6
                || MASTER_AS_PREVIEW_AUDIO_CODECS.contains(audioStream.codecName()) == false) {
                return false;
            }
        }

        return true;
    }

}
