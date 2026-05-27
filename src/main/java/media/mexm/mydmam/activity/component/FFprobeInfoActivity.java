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
import static java.util.stream.Collectors.joining;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.BASE_PREVIEW;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.aspectRatio;
import static media.mexm.mydmam.activity.component.ImageAspectRatioDetectionActivity.getPageOrientation;
import static media.mexm.mydmam.component.FFprobeSupplier.ALL_MIME_TYPES;
import static media.mexm.mydmam.component.FFprobeSupplier.FFPROBE;
import static org.apache.commons.io.FileUtils.write;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static tv.hd3g.ffprobejaxb.MediaSummary.getChannelLayout;
import static tv.hd3g.ffprobejaxb.MediaSummary.getCodecLongName;
import static tv.hd3g.ffprobejaxb.MediaSummary.removeParenthesisContent;
import static tv.hd3g.ffprobejaxb.MediaSummary.upperCase1st;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.ActivityLimitPolicy;
import media.mexm.mydmam.component.FFprobeSupplier;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefChapter;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnical;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefTechnicalTransportStream;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.service.MediaAssetService;
import media.mexm.mydmam.service.MetadataThesaurusService;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.ffprobejaxb.data.FFProbeFormat;
import tv.hd3g.ffprobejaxb.data.FFProbeKeyValue;

@Slf4j
@Component
public class FFprobeInfoActivity implements ActivityHandler {

    private static final String AUDIO_SLASH = "audio/";
    private static final String VIDEO_SLASH = "video/";
    @Autowired
    MediaAssetService mediaAssetService;
    @Autowired
    MetadataThesaurusService metadataThesaurusService;
    @Autowired
    FFprobeSupplier ffprobeSupplier;

    @Override
    public boolean isEnabled() {
        return ffprobeSupplier.isEnabled();
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

    @Override
    public boolean canHandle(final FileEntity fileEntity,
                             final ActivityEventType eventType,
                             final RealmStorageConfiguredEnv storedOn) {
        return storedOn.isDAS()
               && getExtension(fileEntity.getName()).equalsIgnoreCase("aaf") == false /// XXX test
               && metadataThesaurusService.getMimeType(fileEntity)
                       .map(ALL_MIME_TYPES::contains)
                       .orElse(false);
    }

    @Override
    public void handle(final FileEntity fileEntity,
                       final ActivityEventType eventType,
                       final RealmStorageConfiguredEnv storedOn) throws Exception {
        final var assetFile = storedOn.getLocalInternalFile(fileEntity);
        final var ffprobeJAXB = ffprobeSupplier.processSimpleContainerAnalysis(assetFile).getResult();
        saveFFprobeXMLFile(fileEntity, storedOn, ffprobeJAXB);

        final var thesaurus = metadataThesaurusService.getThesaurus(this, fileEntity);
        final var technical = thesaurus.technical();
        final var technicalImage = thesaurus.technicalImage();
        final var technicalMXF = thesaurus.technicalMXF();
        final var technicalStream = thesaurus.technicalStream();
        final var technicalTransportStream = thesaurus.technicalTransportStream();
        final var chapter = thesaurus.chapter();
        final var xmp = thesaurus.xmp();
        final var dublinCore = thesaurus.dublinCore();

        setMediaSummary(ffprobeJAXB, technical);
        setChapters(ffprobeJAXB, chapter);

        final var programIdByMediaStreamIndex = getPrograms(ffprobeJAXB, technicalTransportStream);

        ffprobeJAXB.getStreams().forEach(mediaStream -> {
            /**
             * Quicktime timecode track
             */
            if ("tmcd".equals(mediaStream.codecTagString())
                /**
                 * Special "data" MXF empty tracks
                 */
                || mediaStream.codecName() == null) {
                return;
            }
            final var isMXF = ffprobeJAXB.getFormat().map(FFProbeFormat::formatName).orElse("").equalsIgnoreCase("mxf");

            final var layer = mediaStream.index();
            final var codecType = Objects.requireNonNull(mediaStream.codecType(), "No codec type, invalid FFprobe XML");

            if (mediaStream.isSecondary() == false) {
                technicalStream.type().set(layer, codecType);
                technicalStream.timeBase().set(layer, mediaStream.timeBase());
                technicalStream.startTime().set(layer, mediaStream.startTime());
                technicalStream.disposition()
                        .set(layer, mediaStream.disposition().resumeDispositions().collect(joining(", ")));
                technicalStream.bitrate().set(layer, mediaStream.bitRate() == 0 ? null : mediaStream.bitRate());
            } else {
                final var disposition = mediaStream.disposition();
                if (disposition.attachedPic()) {
                    technicalStream.type().set(layer, "attached-pic");
                } else if (disposition.stillImage()) {
                    technicalStream.type().set(layer, "still-image");
                } else if (disposition.timedThumbnails()) {
                    technicalStream.type().set(layer, "timed-thumbnails");
                }
            }

            technicalStream.referenceId().set(layer, mediaStream.id());
            technicalStream.programId().set(layer, programIdByMediaStreamIndex.get(mediaStream.index()));
            technicalStream.profile().set(layer, mediaStream.profile());
            technicalStream.codec().set(layer, mediaStream.codecName());
            technicalStream.isSecondary().set(layer, mediaStream.isSecondary());
            technicalStream.codecName().set(layer, upperCase1st(getCodecLongName(mediaStream)));

            final var level = mediaStream.level();
            if (level > 0 && mediaStream.isSecondary() == false) {
                technicalStream.level().set(layer, mediaStream.level());
            }

            if (codecType.equals("audio") && mediaStream.isSecondary() == false) {
                final var technicalAudio = thesaurus.technicalAudio();
                technicalAudio.channelLayout().set(layer, getChannelLayout(mediaStream.channelLayout()));
                technicalAudio.channelsCount().set(layer, mediaStream.channels());
                technicalAudio.sampleRate().set(layer, mediaStream.sampleRate());
                technicalAudio.sampleFormat().set(layer, mediaStream.sampleFmt());
                if (isMXF) {
                    technicalAudio.referenceId().set(layer, mediaStream.index());
                } else {
                    technicalAudio.referenceId().set(layer, mediaStream.id());
                }
            }

            if (codecType.equals("video")) {
                final var width = mediaStream.width();
                final var height = mediaStream.height();

                technicalImage.width().set(layer, width);
                technicalImage.height().set(layer, height);

                if (isMXF) {
                    technicalImage.referenceId().set(layer, mediaStream.index());
                } else {
                    technicalImage.referenceId().set(layer, mediaStream.id());
                }

                technicalImage.pixelformat().set(layer, mediaStream.pixFmt());
                technicalImage.colorspace().set(layer, mediaStream.colorSpace());
                technicalImage.colorrange().set(layer, mediaStream.colorRange());
                technicalImage.colorprimaries().set(layer, mediaStream.colorPrimaries());
                technicalImage.colortransfer().set(layer, mediaStream.colorTransfer());

                technicalImage.sampleAspectRatio().set(layer, mediaStream.sampleAspectRatio());
                technicalImage.displayAspectRatio().set(layer, mediaStream.displayAspectRatio());
                technicalImage.aspectRatio().set(layer, aspectRatio(width, height));
                technicalImage.imageAspectFormat().set(layer, getPageOrientation(width, height));

                if (mediaStream.isSecondary() == false) {
                    final var technicalVideo = thesaurus.technicalVideo();
                    technicalVideo.fieldOrder().set(layer, mediaStream.fieldOrder());
                    technicalVideo.frameRate().set(layer, mediaStream.getComputedRFrameRate());
                    technicalVideo.averageFrameRate().set(layer, mediaStream.getComputedAvgFrameRate());
                    technicalVideo.referenceId().set(layer, mediaStream.id());
                }
            }

            if (isMXF) {
                technicalMXF.trackIndex().set(layer, layer);
            }
            if (mediaStream.isSecondary() == false) {
                technicalMXF.trackName().set(layer, getTagByName(mediaStream.tags(), "track_name"));
                technicalMXF.filePackageUMID().set(layer, getTagByName(mediaStream.tags(), "file_package_umid"));
                technicalMXF.filePackageName().set(layer, getTagByName(mediaStream.tags(), "file_package_name"));
                dublinCore.language().set(layer, getTagByName(mediaStream.tags(), "language"));
            }
        });

        ffprobeJAXB.getFormat().ifPresent(format -> {
            final var layer = -1;

            final var technicalContainer = thesaurus.technicalContainer();
            technicalContainer.timecode().set(ffprobeJAXB.getTimecode(false));
            technicalContainer.duration().set(ffprobeJAXB.getDuration());
            technicalContainer.bitrate().set(format.bitRate());
            technicalContainer.format().set(format.formatName());
            technicalContainer.formatName().set(upperCase1st(removeParenthesisContent(format.formatLongName())));
            technicalContainer.startTime().set(format.startTime());

            final var oModificationDate = getTagByName(format.tags(), "modification_date");
            final var oCreationDate = getTagByName(format.tags(), "creation_time");

            xmp.modifyDate().setDateISO8601(oModificationDate);
            xmp.createDate().setDateISO8601(oCreationDate);
            dublinCore.date().setDateISO8601(oModificationDate.or(() -> oCreationDate));

            final var creatorTool = new StringBuilder();
            getTagByName(format.tags(), "product_name").ifPresent(creatorTool::append);
            getTagByName(format.tags(), "product_version").ifPresent(v -> creatorTool.append(" v" + v));
            getTagByName(format.tags(), "application_platform")
                    .map(String::toUpperCase)
                    .ifPresent(cn -> creatorTool.append(" on " + cn));
            getTagByName(format.tags(), "company_name").ifPresent(cn -> creatorTool.append(" (" + cn + ")"));
            xmp.creatorTool().set(creatorTool.toString().trim());

            if (format.formatName().equalsIgnoreCase("mxf")) {
                technicalMXF.trackIndex().set(layer, "container");
            }
            technicalMXF.operationalPatternUL().set(layer, getTagByName(format.tags(), "operational_pattern_ul"));
            technicalMXF.uid().set(layer, getTagByName(format.tags(), "uid"));
            technicalMXF.generationUID().set(layer, getTagByName(format.tags(), "generation_uid"));
            technicalMXF.materialPackageUMID().set(layer, getTagByName(format.tags(), "material_package_umid"));
            dublinCore.language().set(layer, getTagByName(format.tags(), "language"));
        });

        final var validVideoStreams = ffprobeJAXB.getVideoStreams().toList();
        final var haveVideo = validVideoStreams.isEmpty() == false;
        final var haveAudio = ffprobeJAXB.getAudioStreams().count() > 0l;

        patchInvalidAVMimeTypes(fileEntity, haveVideo, haveAudio);
    }

    private static Map<Integer, Integer> getPrograms(final FFprobeJAXB ffprobeJAXB,
                                                     final MtdThesaurusDefTechnicalTransportStream tsWriter) {
        final var result = new HashMap<Integer, Integer>();
        ffprobeJAXB.getPrograms().forEach(program -> {
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

    private static void setChapters(final FFprobeJAXB ffprobeJAXB,
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

    private static Optional<String> getTagByName(final List<FFProbeKeyValue> tags, final String name) {
        return tags.stream()
                .filter(t -> name.equalsIgnoreCase(t.key()))
                .findFirst()
                .map(FFProbeKeyValue::value);
    }

    private void saveFFprobeXMLFile(final FileEntity fileEntity,
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

    void patchInvalidAVMimeTypes(final FileEntity fileEntity,
                                 final boolean haveVideo,
                                 final boolean haveAudio) {
        final var currentMimeType = metadataThesaurusService.getMimeType(fileEntity).orElseThrow();
        if (currentMimeType.equals("video/mp2t")) {
            return;
        }
        if (currentMimeType.startsWith(VIDEO_SLASH) && haveVideo == false && haveAudio) {
            metadataThesaurusService.setMimeType(this, fileEntity, currentMimeType.replace(VIDEO_SLASH, AUDIO_SLASH));
        } else if (currentMimeType.startsWith(AUDIO_SLASH) && haveVideo == true) {
            metadataThesaurusService.setMimeType(this, fileEntity, currentMimeType.replace(AUDIO_SLASH, VIDEO_SLASH));
        }
    }

    private static void setMediaSummary(final FFprobeJAXB ffprobeJAXB,
                                        final MtdThesaurusDefTechnical writer) {
        final var mediaSummary = ffprobeJAXB.getMediaSummary();
        final var mediaSummaryStr = Stream.concat(
                Optional.ofNullable(mediaSummary.format()).stream(),
                mediaSummary.streams().stream())
                .collect(Collectors.joining("\n"));
        writer.type().set(mediaSummaryStr);
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

    boolean isCanBeUsedInMasterAsPreview(final String mimeType, final FFprobeJAXB ffprobeJAXB) {// TODO test isCanBeUsedInMasterAsPreview
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
