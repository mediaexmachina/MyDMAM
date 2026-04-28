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
import java.time.Instant;
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
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusDefinitionWriter;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefChapter;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefDublinCore;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnical;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalAudio;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalContainer;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalImage;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalMXF;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalStream;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalTransportStream;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalVideo;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefXMP;
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

    private Set<String> passingPlaybookNames;

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

        final var writer = metadataThesaurusService.getWriter(
                this, fileEntity, MtdThesaurusDefTechnical.class);
        final var imageWriter = metadataThesaurusService.getWriter(
                this, fileEntity, MtdThesaurusDefTechnicalImage.class);
        final var dublinCoreWriter = metadataThesaurusService.getWriter(this, fileEntity,
                MtdThesaurusDefDublinCore.class);

        setMediaSummary(ffprobeJAXB, writer);
        setChapters(fileEntity, ffprobeJAXB);

        final var programIdByMediaStreamIndex = getPrograms(fileEntity, ffprobeJAXB);

        final var mxfWriter = metadataThesaurusService.getWriter(
                this, fileEntity, MtdThesaurusDefTechnicalMXF.class);

        ffprobeJAXB.getStreams().forEach(mediaStream -> {
            if ("tmcd".equals(mediaStream.codecTagString())) {
                /**
                 * Quicktime timecode track
                 */
                return;
            }

            final var layer = mediaStream.index();
            final var codecType = Objects.requireNonNull(mediaStream.codecType(), "No codec type, invalid FFprobe XML");

            final var streamWriter = metadataThesaurusService.getWriter(
                    this, fileEntity, MtdThesaurusDefTechnicalStream.class);
            streamWriter.set(layer, codecType).type();
            streamWriter.set(layer, mediaStream.timeBase()).timeBase();
            streamWriter.set(layer, mediaStream.id()).referenceId();
            streamWriter.set(layer, programIdByMediaStreamIndex.get(mediaStream.index())).programId();
            streamWriter.set(layer, mediaStream.bitRate()).bitrate();
            streamWriter.set(layer, mediaStream.profile()).profile();
            streamWriter.set(layer, mediaStream.startTime()).startTime();
            streamWriter.set(layer, mediaStream.codecName()).codec();

            final var codecLongName = WELL_KNOWN_CODECS_NAMES.getOrDefault(
                    mediaStream.codecName(),
                    mediaStream.codecLongName());
            streamWriter.set(layer, codecLongName).codecName();

            final var level = mediaStream.level();
            if (level > 0) {
                streamWriter.set(layer, mediaStream.level()).level();
            }

            if (codecType.equals("audio")) {
                final var audioWriter = metadataThesaurusService.getWriter(
                        this, fileEntity, MtdThesaurusDefTechnicalAudio.class);
                audioWriter.set(layer, mediaStream.channelLayout()).channelLayout();
                audioWriter.set(layer, mediaStream.channels()).channelsCount();
                audioWriter.set(layer, mediaStream.sampleRate()).sampleRate();
                audioWriter.set(layer, mediaStream.sampleFmt()).sampleFormat();
            }

            if (codecType.equals("video")) {
                final var width = mediaStream.width();
                final var height = mediaStream.height();

                imageWriter.set(layer, width).width();
                imageWriter.set(layer, height).height();

                final var videoWriter = metadataThesaurusService.getWriter(
                        this, fileEntity, MtdThesaurusDefTechnicalVideo.class);
                videoWriter.set(layer, mediaStream.fieldOrder()).fieldOrder();
                videoWriter.set(layer, mediaStream.rFrameRate()).frameRate();
                videoWriter.set(layer, mediaStream.avgFrameRate()).averageFrameRate();

                imageWriter.set(layer, mediaStream.pixFmt()).pixelformat();
                imageWriter.set(layer, removeUnknown(mediaStream.colorSpace())).colorspace();
                imageWriter.set(layer, removeUnknown(mediaStream.colorRange())).colorrange();
                imageWriter.set(layer, removeUnknown(mediaStream.colorPrimaries())).colorprimaries();
                imageWriter.set(layer, removeUnknown(mediaStream.colorTransfer())).colortransfer();

                imageWriter.set(layer, mediaStream.sampleAspectRatio()).sampleAspectRatio();
                imageWriter.set(layer, mediaStream.displayAspectRatio()).displayAspectRatio();
                imageWriter.set(aspectRatio(width, height)).aspectRatio();
                imageWriter.set(getPageOrientation(width, height)).imageAspectFormat();
            }

            Optional.ofNullable(mediaStream.disposition())
                    .ifPresentOrElse(
                            disposition -> streamWriter.set(
                                    layer,
                                    disposition.attachedPic()
                                           || disposition.stillImage()
                                           || disposition.timedThumbnails())
                                    .isSecondary(),
                            () -> streamWriter.set(
                                    layer,
                                    false)
                                    .isSecondary());

            mxfWriter.set(layer, getTagByName(mediaStream.tags(), "track_name")).trackName();
            mxfWriter.set(layer, getTagByName(mediaStream.tags(), "file_package_umid")).filePackageUMID();
            mxfWriter.set(layer, getTagByName(mediaStream.tags(), "file_package_name")).filePackageName();
            dublinCoreWriter.set(layer, getTagByName(mediaStream.tags(), "language")).language();
        });

        ffprobeJAXB.getFormat().ifPresent(format -> {
            final var layer = -1;

            final var containerWriter = metadataThesaurusService.getWriter(
                    this, fileEntity, MtdThesaurusDefTechnicalContainer.class);
            containerWriter.set(ffprobeJAXB.getTimecode(false)).timecode();
            containerWriter.set(ffprobeJAXB.getDuration()).duration();
            containerWriter.set(format.bitRate()).bitrate();
            containerWriter.set(format.formatName()).format();
            containerWriter.set(format.formatLongName()).formatName();
            containerWriter.set(format.startTime()).startTime();

            final var xmpWriter = metadataThesaurusService.getWriter(
                    this, fileEntity, MtdThesaurusDefXMP.class);

            final var oModificationDate = getTagByName(format.tags(), "modification_date")
                    .map(Instant::parse)
                    .map(Instant::getEpochSecond);
            final var oCreationDate = getTagByName(format.tags(), "creation_time")
                    .map(Instant::parse)
                    .map(Instant::getEpochSecond);

            xmpWriter.set(oModificationDate).modifyDate();
            xmpWriter.set(oCreationDate).createDate();
            dublinCoreWriter.set(oModificationDate.or(() -> oCreationDate)).date();

            final var creatorTool = new StringBuilder();
            getTagByName(format.tags(), "product_name").ifPresent(creatorTool::append);
            getTagByName(format.tags(), "product_version").ifPresent(v -> creatorTool.append(" v" + v));
            getTagByName(format.tags(), "company_name").ifPresent(cn -> creatorTool.append(" (" + cn + ")"));
            xmpWriter.set(creatorTool.toString().trim()).creatorTool();

            mxfWriter.set(layer, getTagByName(format.tags(), "operational_pattern_ul")).operationalPatternUL();
            mxfWriter.set(layer, getTagByName(format.tags(), "uid")).uid();
            mxfWriter.set(layer, getTagByName(format.tags(), "generation_uid")).generationUID();
            mxfWriter.set(layer, getTagByName(format.tags(), "material_package_umid")).materialPackageUMID();
            dublinCoreWriter.set(layer, getTagByName(format.tags(), "language")).language();
        });

        final var validVideoStreams = filterValidVideoStreams(ffprobeJAXB).toList();
        final var currentMimeType = metadataThesaurusService.getMimeType(fileEntity).orElseThrow();
        final var haveVideo = validVideoStreams.isEmpty() == false;
        final var haveAudio = ffprobeJAXB.getAudioStreams().count() > 0l;

        patchInvalidAVMimeTypes(dublinCoreWriter, currentMimeType, haveVideo, haveAudio);
    }

    Map<Integer, Integer> getPrograms(final FileEntity fileEntity,
                                      final FFprobeJAXB ffprobeJAXB) {
        final var result = new HashMap<Integer, Integer>();
        final var programs = ffprobeJAXB.getPrograms();
        if (programs.isEmpty() == false) {
            return Map.of();
        }
        final var tsWriter = metadataThesaurusService.getWriter(
                this, fileEntity, MtdThesaurusDefTechnicalTransportStream.class);

        programs.forEach(program -> {
            final var layer = program.programId();
            tsWriter.set(layer, program.programNum()).programNum();
            tsWriter.set(layer, program.pcrPid()).pcrPid();
            tsWriter.set(layer, program.pmtPid()).pmtPid();
            tsWriter.set(layer, getTagByName(program.tags(), "service_name")).serviceName();
            tsWriter.set(layer, getTagByName(program.tags(), "service_provider")).serviceProvider();
            program.streams().forEach(mediaStream -> result.put(
                    mediaStream.index(),
                    program.programId()));
        });
        return unmodifiableMap(result);
    }

    void setChapters(final FileEntity fileEntity, final FFprobeJAXB ffprobeJAXB) {
        final var chapters = ffprobeJAXB.getChapters();
        if (chapters.isEmpty()) {
            return;
        }
        final var chapterWriter = metadataThesaurusService.getWriter(
                this, fileEntity, MtdThesaurusDefChapter.class);

        for (var pos = 0; pos < chapters.size(); pos++) {
            final var chapter = chapters.get(pos);
            chapterWriter.set(pos + 1, getTagByName(chapter.tags(), "title")).title();
            chapterWriter.set(pos + 1, Math.round(chapter.startTime() * 1000)).startTime();
            chapterWriter.set(pos + 1, Math.round(chapter.endTime() * 1000)).endTime();
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

    static void patchInvalidAVMimeTypes(final MetadataThesaurusDefinitionWriter<MtdThesaurusDefDublinCore> writer,
                                        final String currentMimeType,
                                        final boolean haveVideo,
                                        final boolean haveAudio) {
        if (currentMimeType.startsWith(VIDEO_SLASH) && haveVideo == false && haveAudio) {
            writer.set(currentMimeType.replace(VIDEO_SLASH, AUDIO_SLASH)).format();
        } else if (currentMimeType.startsWith(AUDIO_SLASH) && haveVideo == true) {
            writer.set(currentMimeType.replace(AUDIO_SLASH, VIDEO_SLASH)).format();
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
                                final MetadataThesaurusDefinitionWriter<MtdThesaurusDefTechnical> writer) {
        final var mediaSummary = ffprobeJAXB.getMediaSummary();
        if (mediaSummary.format().isEmpty() == false) {
            writer.set(-1, mediaSummary.format()).type();
        }
        final var mediaSummaryStreams = mediaSummary.streams();
        for (var pos = 0; pos < mediaSummaryStreams.size(); pos++) {
            final var mediaSummaryForStream = mediaSummaryStreams.get(pos);
            if (mediaSummaryForStream.isEmpty() == false) {
                writer.set(pos, mediaSummaryForStream).type();
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
            "audio/gsm");

    boolean isCanUsedInMasterAsPreview(final String mimeType, final FFprobeJAXB ffprobeJAXB) {
        final var oVideoStream = ffprobeJAXB.getFirstVideoStream();
        final var oAudioStream = ffprobeJAXB.getAudioStreams().findFirst();

        // TODO

        if (oVideoStream.isPresent()) {
            final var videoStream = oVideoStream.get();

        }

        if (oAudioStream.isPresent()) {
            final var audioStream = oAudioStream.get();

            audioStream.sampleRate();
            audioStream.codecName();

            if (audioStream.channels() > 6) {
                return false;
            }

        }

        return false;
    }

    /*

    //TODO MasterAsPreview

    MP3
    AAC (all flavors)
    Vorbis
    
    G.729
    AMR-NB
    AMR-WB (G.722.2)
    Speex
    iSAC
    iLBC
    G.722.1 (all variants)
    G.719


                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].sample_rate", Comparator.EQUALS, 48000, 44100, 32000);
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].codec_name", Comparator.EQUALS, "aac");
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].channels", Comparator.EQUALS, 1, 2);
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].bit_rate", Comparator.EQUALS_OR_SMALLER_THAN, 384000);
                ***
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].codec_name", Comparator.EQUALS, "h264");
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].width", Comparator.EQUALS_OR_SMALLER_THAN, 1920);
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].height", Comparator.EQUALS_OR_SMALLER_THAN, 1080);
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].level", Comparator.EQUALS_OR_SMALLER_THAN, 42);
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].bit_rate", Comparator.EQUALS_OR_SMALLER_THAN, 4000000);
    
                audio_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].codec_name", Comparator.EQUALS, "aac", "mp3");
                audio_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].channels", Comparator.EQUALS, 1, 2);
                audio_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].bit_rate", Comparator.EQUALS_OR_SMALLER_THAN, 384000);

            if (video_webbrowser_validation.validate(container)) {
                Loggers.Transcode_Metadata_Validation.debug("Master as preview (video) ok for " + container.getOrigin().toString());
                return true;
            } else if (audio_webbrowser_validation.validate(container)) {
                Loggers.Transcode_Metadata_Validation.debug("Master as preview (audio) ok for " + container.getOrigin().toString());
                return true;
            }
    */
}
