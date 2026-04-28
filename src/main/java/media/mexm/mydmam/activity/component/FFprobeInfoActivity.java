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
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.BASE_PREVIEW;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static org.apache.commons.io.FileUtils.write;
import static tv.hd3g.processlauncher.cmdline.Parameters.bulk;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
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
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefDublinCore;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnical;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalAudio;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalStream;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalVideo;
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
                    "ffprobe",
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

            externalExecCapabilities.tearDown("ffprobe");
            passingPlaybookNames = externalExecCapabilities.getPassingPlaybookNames("ffprobe");
        }
        return passingPlaybookNames.contains("run");
    }

    @Override
    public String getHandlerName() {
        return "ffprobe-info";
    }

    @Override
    public String getMetadataOriginName() {
        return "ffprobe";
    }

    @Override
    public ActivityLimitPolicy getLimitPolicy() {
        return FILE_INFORMATION;
    }

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

    // TODO manage raster images

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
        final var probeMedia = new ProbeMedia("ffprobe", maxExecTimeScheduler);
        probeMedia.setExecutableFinder(executableFinder);
        probeMedia.setWorkingDirectory(assetFile.getParentFile());
        final var ffprobeJAXB = probeMedia.process(assetFile.getName()).getResult();

        saveFFprobeXMLFile(fileEntity, storedOn, ffprobeJAXB);

        final var writer = metadataThesaurusService.getWriter(
                this, fileEntity, MtdThesaurusDefTechnical.class);
        final var audioWriter = metadataThesaurusService.getWriter(
                this, fileEntity, MtdThesaurusDefTechnicalAudio.class);
        final var streamWriter = metadataThesaurusService.getWriter(
                this, fileEntity, MtdThesaurusDefTechnicalStream.class);
        final var videoWriter = metadataThesaurusService.getWriter(
                this, fileEntity, MtdThesaurusDefTechnicalVideo.class);

        writer.set(ffprobeJAXB.getTimecode(false)).timecode();
        writer.set(ffprobeJAXB.getDuration()).duration();
        setMediaSummary(ffprobeJAXB, writer);

        final var programIdByMediaStreamIndex = new HashMap<Integer, Integer>();
        ffprobeJAXB.getPrograms().forEach(program -> {
            // XXX
            program.programId();

            program.programNum();
            program.pcrPid();
            program.pmtPid();

            getTagByName(program.tags(), "service_name");
            getTagByName(program.tags(), "service_provider");

            program.streams().forEach(mediaStream -> {
                programIdByMediaStreamIndex.put(mediaStream.index(), program.programId());
            });
        });

        ffprobeJAXB.getStreams().forEach(mediaStream -> {
            if ("tmcd".equals(mediaStream.codecTagString())) {
                /**
                 * Quicktime timecode track
                 */
                return;
            }

            final var layer = mediaStream.index();
            final var codecType = Objects.requireNonNull(mediaStream.codecType(), "No codec type, invalid FFprobe XML");

            streamWriter.set(layer, codecType).type();
            streamWriter.set(layer, mediaStream.timeBase()).timeBase();
            streamWriter.set(layer, mediaStream.id()).referenceId();
            streamWriter.set(layer, programIdByMediaStreamIndex.get(mediaStream.index())).programId();
            streamWriter.set(layer, mediaStream.codecName()).codec();
            streamWriter.set(layer, mediaStream.codecLongName()).codecName();
            streamWriter.set(layer, mediaStream.bitRate()).bitrate();
            streamWriter.set(layer, mediaStream.profile()).profile();

            final var level = mediaStream.level();
            if (level > 0) {
                streamWriter.set(layer, mediaStream.level()).level();
            }

            if (codecType.equals("audio")) {
                audioWriter.set(layer, mediaStream.channelLayout()).channelLayout();
                audioWriter.set(layer, mediaStream.channels()).channelsCount();
                audioWriter.set(layer, mediaStream.sampleRate()).sampleRate();
                audioWriter.set(layer, mediaStream.sampleFmt()).sampleFormat();
            }

            if (codecType.equals("video")) {
                writer.set(layer, mediaStream.width()).width();
                writer.set(layer, mediaStream.height()).height();

                videoWriter.set(layer, mediaStream.fieldOrder()).fieldOrder();
                videoWriter.set(layer, mediaStream.rFrameRate()).frameRate();
                videoWriter.set(layer, mediaStream.avgFrameRate()).averageFrameRate();
            }

            writer.set(layer, mediaStream.pixFmt()).pixelformat();
            writer.set(layer, removeUnknown(mediaStream.colorSpace())).colorspace();
            writer.set(layer, removeUnknown(mediaStream.colorRange())).colorrange();
            writer.set(layer, removeUnknown(mediaStream.colorPrimaries())).colorprimaries();
            writer.set(layer, removeUnknown(mediaStream.colorTransfer())).colortransfer();
            writer.set(layer, mediaStream.sampleAspectRatio()).sampleAspectRatio();
            writer.set(layer, mediaStream.displayAspectRatio()).displayAspectRatio();

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

            // XXX mediaStream.tags()
            // MXF <tag key="track_name" value="Track 4"/> + file_package_umid + file_package_name
        });

        // TODO
        ffprobeJAXB.getFormat().ifPresent(format -> {
            // XXX

            format.bitRate();
            format.formatLongName();
            format.formatName();
            format.startTime();
            //

            /*
             * MXF
              XXX format.tags();
            final var xmpWriter = metadataThesaurusService.getWriter(this, fileEntity, MtdThesaurusDefXMP.class);
            xmpWriter.set(xpdf.getInfo(pdfInfo, "CreationDate")
                .map(Instant::parse)
                .map(Instant::getEpochSecond))
                .createDate();
            xmpWriter.set(xpdf.getInfo(pdfInfo, "ModDate")
                .map(Instant::parse)
                .map(Instant::getEpochSecond))
                .modifyDate();
            xmpWriter.set(xpdf.getInfo(pdfInfo, "Creator"))
                .creatorTool();
            xmpWriter.set(xpdf.getInfo(pdfInfo, "MetadataDate"))
                .metadataDate();
            
             *
            <tag key="operational_pattern_ul" value="060e2b34.04010101.0d010201.01010900"/>
            <tag key="uid" value="fd05544c-f01e-11e8-8466-1831bf24284f"/>
            <tag key="generation_uid" value="fd05544d-f01e-11e8-b238-1831bf24284f"/>

            <tag key="company_name" value="Adobe Systems Incorporated"/>
            <tag key="product_name" value="Adobe Media Encoder"/>
            <tag key="product_version_num" value="5.0.15.0.1"/>
            <tag key="product_version" value="13.0.1"/>
            <tag key="application_platform" value="win32"/>
            <tag key="product_uid" value="0c3919fe-46e8-11e5-a151-feff819cdc9f"/>

            <tag key="toolkit_version_num" value="5.0.15.0.1"/>
            <tag key="modification_date" value="2018-11-24T19:27:31.944000Z"/>
            <tag key="material_package_umid" value="0x060A2B340101010501010D1213AABA676DE82C04468405A54BA51831BF24284F"/>
            <tag key="timecode" value="09:59:30:00"/>
             * */

        });

        ffprobeJAXB.getChapters().forEach(chapter -> {
            // XXX
            chapter.startTime();
            chapter.endTime();
            getTagByName(chapter.tags(), "title");
        });

        final var validVideoStreams = filterValidVideoStreams(ffprobeJAXB).toList();
        final var currentMimeType = metadataThesaurusService.getMimeType(fileEntity).orElseThrow();
        final var haveVideo = validVideoStreams.isEmpty() == false;
        final var haveAudio = ffprobeJAXB.getAudioStreams().count() > 0l;

        final var dublinCoreWriter = metadataThesaurusService.getWriter(this, fileEntity,
                MtdThesaurusDefDublinCore.class);
        patchInvalidAVMimeTypes(dublinCoreWriter, currentMimeType, haveVideo, haveAudio);

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

    // XXX
    private static Properties translated_codecs_names;

    static {
        translated_codecs_names = new Properties();
        translated_codecs_names.setProperty("dvvideo", "DV");
        translated_codecs_names.setProperty("dvcp", "DV/DVCPro");
        translated_codecs_names.setProperty("dv5p", "DVCPro 50");
        translated_codecs_names.setProperty("avc1", "h264");
        translated_codecs_names.setProperty("mpeg2video", "MPEG2");
        translated_codecs_names.setProperty("mx5p", "MPEG2/4:2:2");
        translated_codecs_names.setProperty("mpeg", "MPEG");
        translated_codecs_names.setProperty("wmv3", "WMV9");
        translated_codecs_names.setProperty("apch", "Apple ProRes 422 HQ");
        translated_codecs_names.setProperty("apcn", "Apple ProRes 422");
        translated_codecs_names.setProperty("apcs", "Apple ProRes 422 LT");
        translated_codecs_names.setProperty("apco", "Apple ProRes 422 Proxy");
        translated_codecs_names.setProperty("ap4h", "Apple ProRes 4444");

        translated_codecs_names.setProperty("mp2", "MPEG/L2");
        translated_codecs_names.setProperty("mp3", "MP3");
        translated_codecs_names.setProperty("wmav2", "WMA9");
        translated_codecs_names.setProperty("aac", "AAC");
        translated_codecs_names.setProperty("mp4a", "AAC");
        translated_codecs_names.setProperty("eac3", "EAC3");
        translated_codecs_names.setProperty("ec-3", "EAC3");
        translated_codecs_names.setProperty("pcm_s16le", "PCM 16b");
        translated_codecs_names.setProperty("pcm_s16le_planar", "PCM 16b");
        translated_codecs_names.setProperty("pcm_s16be", "PCM 16b/BE");
        translated_codecs_names.setProperty("pcm_s24le", "PCM 24b");
        translated_codecs_names.setProperty("pcm_s24be", "PCM 24b/BE");
        translated_codecs_names.setProperty("pcm_f32le", "PCM 32b float");
        translated_codecs_names.setProperty("pcm_f32be", "PCM 32b float/BE");
    }

    /*

    public List<String> getMimeFileListCanUsedInMasterAsPreview() {
        final var al = new ArrayList<String>();
        al.add("audio/mpeg");
        al.add("audio/mp4");
        al.add("audio/quicktime");
        al.add("video/quicktime");
        al.add("video/mp4");
        return al;
    }


    public boolean isCanUsedInMasterAsPreview(Container container) {
        if (mime_list_master_as_preview == null) {
            mime_list_master_as_preview = getMimeFileListCanUsedInMasterAsPreview().toArray(new String[0]);
        }
        if (container.getSummary().equalsMimetype(mime_list_master_as_preview)) {
            if (video_webbrowser_validation == null) {
                video_webbrowser_validation = new ValidatorCenter();
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].sample_rate", Comparator.EQUALS, 48000, 44100, 32000);
                video_webbrowser_validation.and();
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].codec_name", Comparator.EQUALS, "aac");
                video_webbrowser_validation.and();
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].channels", Comparator.EQUALS, 1, 2);
                video_webbrowser_validation.and();
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].bit_rate", Comparator.EQUALS_OR_SMALLER_THAN, 384000);
                video_webbrowser_validation.and();
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].codec_name", Comparator.EQUALS, "h264");
                video_webbrowser_validation.and();
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].width", Comparator.EQUALS_OR_SMALLER_THAN, 1920);
                video_webbrowser_validation.and();
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].height", Comparator.EQUALS_OR_SMALLER_THAN, 1080);
                video_webbrowser_validation.and();
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].level", Comparator.EQUALS_OR_SMALLER_THAN, 42);
                video_webbrowser_validation.and();
                video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].bit_rate", Comparator.EQUALS_OR_SMALLER_THAN, 4000000);
            }
            if (audio_webbrowser_validation == null) {
                audio_webbrowser_validation = new ValidatorCenter();
                audio_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].codec_name", Comparator.EQUALS, "aac", "mp3");
                audio_webbrowser_validation.and();
                audio_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].channels", Comparator.EQUALS, 1, 2);
                audio_webbrowser_validation.and();
                audio_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].bit_rate", Comparator.EQUALS_OR_SMALLER_THAN, 384000);
            }

            if (video_webbrowser_validation.validate(container)) {
                Loggers.Transcode_Metadata_Validation.debug("Master as preview (video) ok for " + container.getOrigin().toString());
                return true;
            } else if (audio_webbrowser_validation.validate(container)) {
                Loggers.Transcode_Metadata_Validation.debug("Master as preview (audio) ok for " + container.getOrigin().toString());
                return true;
            }
        }
        return false;
    }
    */
}
