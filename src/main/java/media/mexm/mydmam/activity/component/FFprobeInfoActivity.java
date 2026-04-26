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

import static java.util.stream.Collectors.toUnmodifiableSet;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static tv.hd3g.processlauncher.cmdline.Parameters.bulk;

import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.ActivityLimitPolicy;
import media.mexm.mydmam.component.ExternalExecCapabilities;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.service.MediaAssetService;
import media.mexm.mydmam.service.MetadataThesaurusService;

@Slf4j
@Component
public class FFprobeInfoActivity implements ActivityHandler { // TODO test

    @Autowired
    MediaAssetService mediaAssetService;
    @Autowired
    MetadataThesaurusService metadataThesaurusService;
    @Autowired
    ExternalExecCapabilities externalExecCapabilities;

    private Set<String> passingPlaybookNames;

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
        // TODO Auto-generated method stub

        /*
        if (container.getSummary().getMimetype().startsWith("video") && result.hasVideo() == false) {
             // No video, only audio is present but with bad mime category
            container.getSummary().setMimetype("audio" + container.getSummary().getMimetype().substring(5));
        }
        */

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
