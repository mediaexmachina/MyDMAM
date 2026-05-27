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

import static java.io.File.createTempFile;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.BASE_PREVIEW;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static media.mexm.mydmam.component.FFprobeSupplier.ALL_MIME_TYPES;
import static media.mexm.mydmam.component.FFprobeSupplier.FFPROBE;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import media.mexm.mydmam.FlatMetadataThesaurusService;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.component.FFprobeSupplier;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.service.MediaAssetService;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.fflauncher.FFprobe;
import tv.hd3g.fflauncher.processingtool.FFSourceDefinition;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.processlauncher.processingtool.KeepStdoutAndErrToLogWatcher;
import tv.hd3g.processlauncher.processingtool.ProcessingToolResult;

@SpringBootTest(webEnvironment = NONE)
@ExtendWith(MockToolsExtendsJunit.class)
@ActiveProfiles({ "Default" })
class FFprobeInfoActivityTest {

    @MockitoBean
    FFprobeSupplier ffprobeSupplier;
    @MockitoBean
    MediaAssetService mediaAssetService;

    @Mock
    FileEntity fileEntity;
    @Mock
    ActivityEventType eventType;
    @Mock
    RealmStorageConfiguredEnv storedOn;
    @Mock
    ProcessingToolResult<FFSourceDefinition, FFprobe, FFprobeJAXB, KeepStdoutAndErrToLogWatcher> ffprobeSupplierResult;

    @Fake
    String lowerMimeType;

    @Autowired
    FlatMetadataThesaurusService metadataThesaurusService;
    @Autowired
    FFprobeInfoActivity ffia;

    File assetFile;
    File workingFile;

    @BeforeEach
    void init() throws IOException {
        metadataThesaurusService.reset();

        assetFile = createTempFile("mydmam-" + getClass().getSimpleName(), "assetFile");
        deleteQuietly(assetFile);
        when(storedOn.getLocalInternalFile(fileEntity)).thenReturn(assetFile);
        when(ffprobeSupplier.processSimpleContainerAnalysis(assetFile)).thenReturn(ffprobeSupplierResult);

        workingFile = createTempFile("mydmam-" + getClass().getSimpleName(), "workingFile");
        deleteQuietly(workingFile);
    }

    @AfterEach
    void ends() {
        metadataThesaurusService.check();
        verifyNoMoreInteractions(ffprobeSupplier, mediaAssetService);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testIsEnabled(final boolean enabled) {
        when(ffprobeSupplier.isEnabled()).thenReturn(enabled);
        assertThat(ffia.isEnabled()).isEqualTo(enabled);
        verify(ffprobeSupplier, times(1)).isEnabled();
    }

    @Test
    void testGetHandlerName() {
        assertThat(ffia.getHandlerName()).isEqualTo("ffprobe-info");
    }

    @Test
    void testGetMetadataOriginName() {
        assertThat(ffia.getMetadataOriginName()).isEqualTo(FFPROBE);
    }

    @Test
    void testGetLimitPolicy() {
        assertThat(ffia.getLimitPolicy()).isEqualTo(FILE_INFORMATION);
    }

    @Test
    void testCanHandle_true() {
        metadataThesaurusService.presetMimeType(ALL_MIME_TYPES.stream().findFirst().orElseThrow());
        when(storedOn.isDAS()).thenReturn(true);
        when(fileEntity.getName()).thenReturn("something.ext");

        assertTrue(ffia.canHandle(fileEntity, eventType, storedOn));

        metadataThesaurusService.check(fileEntity);
        verify(storedOn, times(1)).isDAS();
        verify(fileEntity, times(1)).getName();
    }

    @Test
    void testCanHandle_false() {
        metadataThesaurusService.presetMimeType("nope/nope");
        when(storedOn.isDAS()).thenReturn(true);
        when(fileEntity.getName()).thenReturn("something.ext");

        assertFalse(ffia.canHandle(fileEntity, eventType, storedOn));

        metadataThesaurusService.check(fileEntity);
        verify(storedOn, times(1)).isDAS();
        verify(fileEntity, times(1)).getName();
    }

    @Test
    void testCanHandle_false_aaf() {
        when(storedOn.isDAS()).thenReturn(true);
        when(fileEntity.getName()).thenReturn("something.aaf");

        assertFalse(ffia.canHandle(fileEntity, eventType, storedOn));

        verify(storedOn, times(1)).isDAS();
        verify(fileEntity, times(1)).getName();
    }

    @Test
    void testHandle() throws Exception {
        final var ffprobeJAXB = FFprobeJAXB.load(
                """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <ffprobe>
                            <streams>
                                <stream index="0" codec_name="ffv1" color_transfer="unknown" codec_long_name="FFmpeg video codec #1" codec_type="video" codec_tag_string="FFV1" codec_tag="0x31564646" width="352" height="288" coded_width="352" coded_height="288" has_b_frames="0" sample_aspect_ratio="1:1" display_aspect_ratio="11:9" pix_fmt="yuv420p" level="123" field_order="progressive" id="0x1" r_frame_rate="25/1" avg_frame_rate="25/1" time_base="1/12800" start_pts="0" start_time="0.000000" duration_ts="64000" duration="5.000000" bit_rate="8568404" nb_frames="125">
                                    <disposition default="1" dub="0" original="0" comment="0" lyrics="0" karaoke="0" forced="0" hearing_impaired="0" visual_impaired="0" clean_effects="0" attached_pic="0" timed_thumbnails="0" non_diegetic="0" captions="0" descriptions="0" metadata="0" dependent="0" still_image="0" multilayer="0"/>
                                </stream>
                                <stream index="1" codec_name="pcm_s16le" codec_long_name="PCM signed 16-bit little-endian" codec_type="audio" codec_tag_string="sowt" codec_tag="0x74776f73" sample_fmt="s16" sample_rate="48000" channels="2" channel_layout="stereo" bits_per_sample="16" initial_padding="0" id="0x2" r_frame_rate="0/0" avg_frame_rate="0/0" time_base="1/48000" start_pts="0" start_time="0.000000" duration_ts="240000" duration="5.000000" bit_rate="1536000" nb_frames="240000">
                                    <disposition default="1" dub="0" original="0" comment="0" lyrics="0" karaoke="0" forced="0" hearing_impaired="0" visual_impaired="0" clean_effects="0" attached_pic="0" timed_thumbnails="0" non_diegetic="0" captions="0" descriptions="0" metadata="0" dependent="0" still_image="0" multilayer="0"/>
                                </stream>
                                <stream index="2" codec_type="data" codec_tag_string="tmcd">
                                    <tags>
                                        <tag key="handler_name" value="Time Code Media Handler"/>
                                        <tag key="timecode" value="30:00:00:00"/>
                                    </tags>
                                </stream>
                                <stream index="3" codec_name="prores" level="666" codec_long_name="Apple ProRes (iCodec Pro)" profile="Standard" codec_type="video" codec_time_base="1/50" codec_tag_string="apcn" codec_tag="0x6e637061" width="720" height="576" coded_width="720" coded_height="576" has_b_frames="0" sample_aspect_ratio="59:54" display_aspect_ratio="295:216" pix_fmt="yuv422p10le" color_range="tv" color_space="smpte170m" color_transfer="bt709" color_primaries="bt470bg" field_order="progressive" refs="1" r_frame_rate="50/1" avg_frame_rate="50/1" time_base="1/50" start_pts="0" start_time="0.000000" duration_ts="2500" duration="50.000000" bit_rate="76328266" bits_per_raw_sample="10" nb_frames="2500">
                                    <disposition attached_pic="1" />
                                </stream>
                                <stream index="4" codec_name="prores" width="1" height="2" codec_type="video">
                                    <disposition still_image="1" />
                                </stream>
                                <stream index="5" codec_name="prores" width="3" height="4" codec_type="video">
                                    <disposition timed_thumbnails="1" />
                                </stream>
                            </streams>

                            <chapters>
                                <chapter id="1" time_base="1/1000000000" start="1000000" start_time="0.001000" end="3000000000" end_time="3.000000">
                                    <tags>
                                        <tag key="title" value="Chap One"/>
                                    </tags>
                                </chapter>
                            </chapters>

                            <format filename="test-ffv1.mov" nb_streams="666" nb_programs="0" nb_stream_groups="0" format_name="mov,mp4,m4a,3gp,3g2,mj2" format_long_name="QuickTime / MOV" start_time="0.000000" duration="5.000000" size="6318420" bit_rate="10109472" probe_score="100">
                                <tags>
                                    <tag key="creation_time" value="2012-12-04T16:09:37.000000Z"/>
                                    <tag key="modification_date" value="2019-11-03T15:08:36.000000Z"/>
                                    <tag key="timecode" value="00:00:00:01"/>
                                </tags>
                            </format>
                        </ffprobe>
                        """);
        when(ffprobeSupplierResult.getResult()).thenReturn(ffprobeJAXB);
        when(storedOn.haveWorkingDir()).thenReturn(false);
        metadataThesaurusService.presetMimeType("audio/nope");

        ffia.handle(fileEntity, eventType, storedOn);

        final var assertThesaurus = metadataThesaurusService.getAssertThesaurus();

        assertThesaurus.technical().type().set("""
                QuickTime / MOV, 00:00:05, TCIN: 00:00:00:01, 1 chapter, 10 Mbps
                video: FFmpeg video codec #1 352×288 L123 @ 25 fps [8568 kbps] yuv420p default
                audio: PCM 16 bits stereo @ 48 kHz default
                attached picture (prores 720×576)
                still image (prores 1×2)
                timed thumbnails (prores 3×4)
                """);

        assertThesaurus.chapter().title().set(1, "Chap One");
        assertThesaurus.chapter().startTime().set(1, 1);
        assertThesaurus.chapter().endTime().set(1, 3000);

        assertThesaurus.technicalStream().referenceId().set(0, "0x1");
        assertThesaurus.technicalStream().startTime().set(0, 0.0);
        assertThesaurus.technicalStream().timeBase().set(0, "1/12800");
        assertThesaurus.technicalStream().type().set(0, "video");
        assertThesaurus.technicalStream().bitrate().set(0, 8568404);
        assertThesaurus.technicalStream().codec().set(0, "ffv1");
        assertThesaurus.technicalStream().codecName().set(0, "FFmpeg video codec #1");
        assertThesaurus.technicalStream().disposition().set(0, "default");
        assertThesaurus.technicalStream().isSecondary().set(0, false);
        assertThesaurus.technicalImage().aspectRatio().set(0, 1.222);
        assertThesaurus.technicalImage().height().set(0, 288);
        assertThesaurus.technicalImage().width().set(0, 352);
        assertThesaurus.technicalImage().imageAspectFormat().set(0, "LANDSCAPE");
        assertThesaurus.technicalImage().pixelformat().set(0, "yuv420p");
        assertThesaurus.technicalImage().referenceId().set(0, "0x1");
        assertThesaurus.technicalImage().sampleAspectRatio().set(0, "1:1");
        assertThesaurus.technicalVideo().averageFrameRate().set(0, 25.0);
        assertThesaurus.technicalVideo().fieldOrder().set(0, "progressive");
        assertThesaurus.technicalVideo().frameRate().set(0, 25.0);
        assertThesaurus.technicalVideo().referenceId().set(0, "0x1");
        assertThesaurus.technicalStream().level().set(0, "123");
        assertThesaurus.technicalImage().displayAspectRatio().set(0, "11:9");

        assertThesaurus.technicalStream().bitrate().set(1, 1536000);
        assertThesaurus.technicalStream().codec().set(1, "pcm_s16le");
        assertThesaurus.technicalStream().codecName().set(1, "PCM 16 bits");
        assertThesaurus.technicalStream().disposition().set(1, "default");
        assertThesaurus.technicalStream().isSecondary().set(1, false);
        assertThesaurus.technicalStream().referenceId().set(1, "0x2");
        assertThesaurus.technicalStream().startTime().set(1, 0.0);
        assertThesaurus.technicalStream().timeBase().set(1, "1/48000");
        assertThesaurus.technicalStream().type().set(1, "audio");
        assertThesaurus.technicalAudio().channelLayout().set(1, "stereo");
        assertThesaurus.technicalAudio().channelsCount().set(1, 2);
        assertThesaurus.technicalAudio().referenceId().set(1, "0x2");
        assertThesaurus.technicalAudio().sampleFormat().set(1, "s16");
        assertThesaurus.technicalAudio().sampleRate().set(1, 48000);

        assertThesaurus.technicalImage().aspectRatio().set(3, 1.25);
        assertThesaurus.technicalImage().colorprimaries().set(3, "bt470bg");
        assertThesaurus.technicalImage().colorrange().set(3, "tv");
        assertThesaurus.technicalImage().colorspace().set(3, "smpte170m");
        assertThesaurus.technicalImage().colortransfer().set(3, "bt709");
        assertThesaurus.technicalImage().displayAspectRatio().set(3, "295:216");
        assertThesaurus.technicalImage().imageAspectFormat().set(3, "LANDSCAPE");
        assertThesaurus.technicalImage().sampleAspectRatio().set(3, "59:54");

        assertThesaurus.technicalStream().codec().set(3, "prores");
        assertThesaurus.technicalImage().width().set(3, 720);
        assertThesaurus.technicalImage().height().set(3, 576);
        assertThesaurus.technicalStream().codecName().set(3, "Apple ProRes 422");
        assertThesaurus.technicalStream().isSecondary().set(3, true);
        assertThesaurus.technicalStream().profile().set(3, "Standard");
        assertThesaurus.technicalStream().type().set(3, "attached-pic");
        assertThesaurus.technicalImage().pixelformat().set(3, "yuv422p10le");

        assertThesaurus.technicalImage().aspectRatio().set(4, 0.5);
        assertThesaurus.technicalImage().width().set(4, 1);
        assertThesaurus.technicalImage().height().set(4, 2);
        assertThesaurus.technicalImage().imageAspectFormat().set(4, "PORTRAIT");
        assertThesaurus.technicalStream().codec().set(4, "prores");
        assertThesaurus.technicalStream().isSecondary().set(4, true);
        assertThesaurus.technicalStream().type().set(4, "still-image");

        assertThesaurus.technicalImage().aspectRatio().set(5, 0.75);
        assertThesaurus.technicalImage().width().set(5, 3);
        assertThesaurus.technicalImage().height().set(5, 4);
        assertThesaurus.technicalImage().imageAspectFormat().set(5, "PORTRAIT");
        assertThesaurus.technicalStream().codec().set(5, "prores");
        assertThesaurus.technicalStream().isSecondary().set(5, true);
        assertThesaurus.technicalStream().type().set(5, "timed-thumbnails");

        assertThesaurus.technicalContainer().bitrate().set(0, 10109472);
        assertThesaurus.technicalContainer().duration().set(0, 5000);
        assertThesaurus.technicalContainer().format().set(0, "mov,mp4,m4a,3gp,3g2,mj2");
        assertThesaurus.technicalContainer().formatName().set(0, "QuickTime / MOV");
        assertThesaurus.technicalContainer().startTime().set(0, 0.0);
        assertThesaurus.technicalContainer().timecode().set(0, "00:00:00:01");
        assertThesaurus.dublinCore().date().setDateISO8601(0, Optional.ofNullable("2019-11-03T15:08:36.000000Z"));
        assertThesaurus.xmp().createDate().setDateISO8601(0, Optional.ofNullable("2012-12-04T16:09:37.000000Z"));
        assertThesaurus.xmp().modifyDate().setDateISO8601(0, Optional.ofNullable("2019-11-03T15:08:36.000000Z"));

        metadataThesaurusService.assertMimeTypeEquals("video/nope");
        metadataThesaurusService.check(ffia).check(fileEntity);

        verify(storedOn, times(1)).getLocalInternalFile(fileEntity);
        verify(storedOn, times(1)).haveWorkingDir();
        verify(ffprobeSupplier, times(1)).processSimpleContainerAnalysis(assetFile);
        verify(ffprobeSupplierResult, times(1)).getResult();
    }

    @Test
    void testHandle_mxf() throws Exception {
        final var ffprobeJAXB = FFprobeJAXB.load(
                """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <ffprobe>
                            <streams>
                                <stream index="0" codec_name="jpeg2000" codec_long_name="JPEG 2000" profile="JPEG 2000 digital cinema 2K" codec_type="video" codec_tag_string="[0][0][0][0]" codec_tag="0x0000" width="1920" height="1080" coded_width="1920" coded_height="1080" has_b_frames="0" sample_aspect_ratio="1:1" display_aspect_ratio="16:9" pix_fmt="xyz12le" level="-99" field_order="progressive" refs="1" r_frame_rate="25/1" avg_frame_rate="0/0" time_base="1/25" start_pts="0" start_time="0.000000" duration_ts="25" duration="1.000000" bits_per_raw_sample="12">
                                    <disposition default="0" dub="0" original="0" comment="0" lyrics="0" karaoke="0" forced="0" hearing_impaired="0" visual_impaired="0" clean_effects="0" attached_pic="0" timed_thumbnails="0" non_diegetic="0" captions="0" descriptions="0" metadata="0" dependent="0" still_image="0" multilayer="0"/>
                                    <tags>
                                        <tag key="file_package_umid" value="0x060A2B340101010501010F2013000000AFC6F609516F4B23B9F9BD16727A2909"/>
                                        <tag key="file_package_name" value="File Package: SMPTE 429-4 frame wrapping of JPEG 2000 codestreams"/>
                                        <tag key="track_name" value="Picture Track"/>
                                    </tags>
                                </stream>
                                <stream index="1" codec_name="pcm_s16le" codec_long_name="PCM signed 16-bit little-endian" codec_type="audio" codec_tag_string="[0][0][0][0]" codec_tag="0x0000" sample_fmt="s16" sample_rate="48000" channels="1" bits_per_sample="16" initial_padding="0" r_frame_rate="0/0" avg_frame_rate="0/0" time_base="1/48000" start_pts="0" start_time="0.000000" duration_ts="48000" duration="1.000000" bit_rate="768000">
                                    <disposition default="0" dub="0" original="0" comment="0" lyrics="0" karaoke="0" forced="0" hearing_impaired="0" visual_impaired="0" clean_effects="0" attached_pic="0" timed_thumbnails="0" non_diegetic="0" captions="0" descriptions="0" metadata="0" dependent="0" still_image="0" multilayer="0" />
                                    <tags>
                                        <tag key="file_package_umid" value="0x060A2B340101010101010F00130000000000036C135ECB89060E2B347F7F2A80" />
                                        <tag key="reel_umid" value="0x060A2B340101010101010F00130000000000036C0E7DCB89060E2B347F7F2A80" />
                                        <tag key="reel_name" value="avid_dvcam_mxf" />
                                    </tags>
                                </stream>
                                <stream index="2" codec_type="data" codec_tag_string="[0][0][0][0]" codec_tag="0x0000" r_frame_rate="0/0" avg_frame_rate="0/0" time_base="1/90000" start_pts="0" start_time="0.000000" duration_ts="90000" duration="1.000000">
                                    <disposition default="0" dub="0" original="0" comment="0" lyrics="0" karaoke="0" forced="0" hearing_impaired="0" visual_impaired="0" clean_effects="0" attached_pic="0" timed_thumbnails="0" non_diegetic="0" captions="0" descriptions="0" metadata="0" dependent="0" still_image="0" multilayer="0" />
                                    <tags>
                                        <tag key="file_package_umid" value="0x060A2B340101010101010F00130000000000036C13AECB89060E2B347F7F2A80" />
                                        <tag key="data_type" value="audio" />
                                    </tags>
                                </stream>
                            </streams>
                            <format filename="DCP_Video.mxf" nb_streams="1" nb_programs="0" nb_stream_groups="0" format_name="mxf" format_long_name="MXF (Material eXchange Format)" start_time="0.000000" duration="1.000000" size="13094022" bit_rate="104752176" probe_score="100">
                                <tags>
                                    <tag key="operational_pattern_ul" value="060e2b34.04010102.0d010201.10000000"/>
                                    <tag key="uid" value="01558a4a-fef9-40cb-af1b-a1ab0f7022c3"/>
                                    <tag key="generation_uid" value="dae0add6-a5fd-438e-b986-3c9a9e9d6213"/>
                                    <tag key="company_name" value="OpenDCP"/>
                                    <tag key="product_name" value="OpenDCP"/>
                                    <tag key="product_version_num" value="0.0.0.0.0"/>
                                    <tag key="product_version" value="0.29.0"/>
                                    <tag key="product_uid" value="43059a1d-0432-4101-b83f-736815acf31d"/>
                                    <tag key="modification_date" value="2014-03-18T14:48:35.000000Z"/>
                                    <tag key="toolkit_version_num" value="1.8.44.27240.1"/>
                                    <tag key="application_platform" value="osx"/>
                                    <tag key="material_package_umid" value="0x060A2B340101010501010F201300000042D11DC1C87E4E418511A40FF94544DE"/>
                                    <tag key="material_package_name" value="AS-DCP Material Package"/>
                                    <tag key="timecode" value="00:00:00:00"/>
                                </tags>
                            </format>
                        </ffprobe>
                        """);

        when(ffprobeSupplierResult.getResult()).thenReturn(ffprobeJAXB);
        when(storedOn.haveWorkingDir()).thenReturn(false);
        metadataThesaurusService.presetMimeType("application/mxf");

        ffia.handle(fileEntity, eventType, storedOn);

        final var assertThesaurus = metadataThesaurusService.getAssertThesaurus();

        assertThesaurus.technical().type().set("""
                MXF, 00:00:01, 105 Mbps
                video: JPEG 2000 1920×1080 Digital cinema 2K @ 25 fps xyz12le
                audio: PCM 16 bits mono @ 48 kHz
                """);

        assertThesaurus.dublinCore().date().setDateISO8601(Optional.ofNullable("2014-03-18T14:48:35.000000Z"));
        assertThesaurus.xmp().creatorTool().set("OpenDCP v0.29.0 on OSX (OpenDCP)");
        assertThesaurus.xmp().modifyDate().setDateISO8601(Optional.ofNullable("2014-03-18T14:48:35.000000Z"));

        assertThesaurus.technicalStream().bitrate().set(1, 768000);
        assertThesaurus.technicalStream().codec().set(0, "jpeg2000");
        assertThesaurus.technicalStream().codec().set(1, "pcm_s16le");
        assertThesaurus.technicalStream().codecName().set(0, "JPEG 2000");
        assertThesaurus.technicalStream().codecName().set(1, "PCM 16 bits");
        assertThesaurus.technicalStream().isSecondary().set(0, false);
        assertThesaurus.technicalStream().isSecondary().set(1, false);

        assertThesaurus.technicalStream().profile().set(0, "JPEG 2000 digital cinema 2K");
        assertThesaurus.technicalStream().startTime().set(0, 0.0);
        assertThesaurus.technicalStream().startTime().set(1, 0.0);

        assertThesaurus.technicalStream().timeBase().set(0, "1/25");
        assertThesaurus.technicalStream().timeBase().set(1, "1/48000");
        assertThesaurus.technicalStream().type().set(0, "video");
        assertThesaurus.technicalStream().type().set(1, "audio");

        assertThesaurus.technicalImage().aspectRatio().set(0, 1.778);
        assertThesaurus.technicalImage().width().set(0, 1920);
        assertThesaurus.technicalImage().height().set(0, 1080);
        assertThesaurus.technicalImage().imageAspectFormat().set(0, "LANDSCAPE");
        assertThesaurus.technicalImage().displayAspectRatio().set(0, "16:9");
        assertThesaurus.technicalImage().pixelformat().set(0, "xyz12le");
        assertThesaurus.technicalImage().referenceId().set(0, 0);
        assertThesaurus.technicalImage().sampleAspectRatio().set(0, "1:1");

        assertThesaurus.technicalVideo().fieldOrder().set(0, "progressive");
        assertThesaurus.technicalVideo().frameRate().set(0, 25.0);

        assertThesaurus.technicalAudio().channelsCount().set(1, 1);
        assertThesaurus.technicalAudio().referenceId().set(1, 1);
        assertThesaurus.technicalAudio().sampleFormat().set(1, "s16");
        assertThesaurus.technicalAudio().sampleRate().set(1, 48000);

        assertThesaurus.technicalContainer().bitrate().set(0, 104752176);
        assertThesaurus.technicalContainer().duration().set(0, 1000);
        assertThesaurus.technicalContainer().format().set(0, "mxf");
        assertThesaurus.technicalContainer().formatName().set(0, "MXF");
        assertThesaurus.technicalContainer().startTime().set(0, 0.0);
        assertThesaurus.technicalContainer().timecode().set(0, "00:00:00:00");

        assertThesaurus.technicalMXF().filePackageName().set(0,
                "File Package: SMPTE 429-4 frame wrapping of JPEG 2000 codestreams");
        assertThesaurus.technicalMXF().filePackageUMID().set(0,
                "0x060A2B340101010501010F2013000000AFC6F609516F4B23B9F9BD16727A2909");
        assertThesaurus.technicalMXF().filePackageUMID().set(1,
                "0x060A2B340101010101010F00130000000000036C135ECB89060E2B347F7F2A80");
        assertThesaurus.technicalMXF().generationUID().set(-1,
                "dae0add6-a5fd-438e-b986-3c9a9e9d6213");
        assertThesaurus.technicalMXF().materialPackageUMID().set(-1,
                "0x060A2B340101010501010F201300000042D11DC1C87E4E418511A40FF94544DE");
        assertThesaurus.technicalMXF().operationalPatternUL().set(-1, "060e2b34.04010102.0d010201.10000000");
        assertThesaurus.technicalMXF().trackIndex().set(0, 0);
        assertThesaurus.technicalMXF().trackIndex().set(1, 1);
        assertThesaurus.technicalMXF().trackIndex().set(-1, "container");
        assertThesaurus.technicalMXF().trackName().set(0, "Picture Track");
        assertThesaurus.technicalMXF().uid().set(-1, "01558a4a-fef9-40cb-af1b-a1ab0f7022c3");

        metadataThesaurusService.assertMimeTypeEquals("application/mxf");
        metadataThesaurusService.check(ffia).check(fileEntity);

        verify(storedOn, times(1)).getLocalInternalFile(fileEntity);
        verify(storedOn, times(1)).haveWorkingDir();
        verify(ffprobeSupplier, times(1)).processSimpleContainerAnalysis(assetFile);
        verify(ffprobeSupplierResult, times(1)).getResult();
    }

    @Test
    void testHandle_ts() throws Exception {
        final var ffprobeJAXB = FFprobeJAXB.load(
                """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <ffprobe>
                            <programs>
                                <program program_id="1" program_num="1" nb_streams="1" pmt_pid="4096" pcr_pid="256">
                                    <tags>
                                        <tag key="service_name" value="Demo render"/>
                                        <tag key="service_provider" value="Media ex Machina"/>
                                    </tags>
                                    <streams>
                                        <stream index="0" />
                                    </streams>
                                </program>
                            </programs>

                            <streams>
                                <stream index="0" codec_name="mp2" codec_long_name="MP2 (MPEG audio layer 2)" codec_type="audio" codec_tag_string="[3][0][0][0]" codec_tag="0x0003" mime_codec_string="mp4a.40.33" sample_fmt="fltp" sample_rate="48000" channels="2" channel_layout="stereo" bits_per_sample="0" initial_padding="0" ts_id="1" ts_packetsize="188" id="0x101" r_frame_rate="0/0" avg_frame_rate="0/0" time_base="1/90000" start_pts="128698" start_time="1.429978" duration_ts="449280" duration="4.992000" bit_rate="256000">
                                    <disposition default="0" dub="0" original="0" comment="0" lyrics="0" karaoke="0" forced="0" hearing_impaired="0" visual_impaired="0" clean_effects="0" attached_pic="0" timed_thumbnails="0" non_diegetic="0" captions="0" descriptions="0" metadata="0" dependent="0" still_image="0" multilayer="0"/>
                                </stream>
                            </streams>

                            <format filename="../.demo-media-files/test-mpeg2.ts" nb_streams="2" nb_programs="1" nb_stream_groups="0" format_name="mpegts" format_long_name="MPEG-TS (MPEG-2 Transport Stream)" start_time="1.429978" duration="5.010022" size="2674676" bit_rate="4270920" probe_score="50"/>
                        </ffprobe>
                        """);

        when(ffprobeSupplierResult.getResult()).thenReturn(ffprobeJAXB);
        when(storedOn.haveWorkingDir()).thenReturn(false);
        metadataThesaurusService.presetMimeType("video/mp2t");

        ffia.handle(fileEntity, eventType, storedOn);

        final var assertThesaurus = metadataThesaurusService.getAssertThesaurus();

        assertThesaurus.technical().type().set("""
                MPEG-TS, 00:00:05, 1 program, 4271 kbps
                audio: MPEG/L2 stereo @ 48 kHz [256 kbps]
                """);

        assertThesaurus.technicalStream().startTime().set(0, 1.429978);
        assertThesaurus.technicalStream().bitrate().set(0, 256000);
        assertThesaurus.technicalStream().codecName().set(0, "MPEG/L2");
        assertThesaurus.technicalStream().type().set(0, "audio");
        assertThesaurus.technicalStream().codec().set(0, "mp2");
        assertThesaurus.technicalStream().isSecondary().set(0, false);
        assertThesaurus.technicalStream().timeBase().set(0, "1/90000");
        assertThesaurus.technicalStream().referenceId().set(0, "0x101");
        // XXX ?? assertThesaurus.technicalStream().programId().set(0, 1);

        assertThesaurus.technicalTransportStream().programNum().set(1, 1);
        assertThesaurus.technicalTransportStream().serviceName().set(1, "Demo render");
        assertThesaurus.technicalTransportStream().serviceProvider().set(1, "Media ex Machina");
        assertThesaurus.technicalTransportStream().pmtPid().set(1, 4096);
        assertThesaurus.technicalTransportStream().pcrPid().set(1, 256);

        assertThesaurus.technicalAudio().sampleFormat().set(0, "fltp");
        assertThesaurus.technicalAudio().referenceId().set(0, "0x101");
        assertThesaurus.technicalAudio().channelLayout().set(0, "stereo");
        assertThesaurus.technicalAudio().sampleRate().set(0, 48000);
        assertThesaurus.technicalAudio().channelsCount().set(0, 2);

        assertThesaurus.technicalContainer().duration().set(0, 5010);
        assertThesaurus.technicalContainer().formatName().set(0, "MPEG-TS");
        assertThesaurus.technicalContainer().startTime().set(0, 1.429978);
        assertThesaurus.technicalContainer().bitrate().set(0, 4270920);
        assertThesaurus.technicalContainer().format().set(0, "mpegts");

        metadataThesaurusService.assertMimeTypeEquals("video/mp2t");
        metadataThesaurusService.check(ffia).check(fileEntity);

        verify(storedOn, times(1)).getLocalInternalFile(fileEntity);
        verify(storedOn, times(1)).haveWorkingDir();
        verify(ffprobeSupplier, times(1)).processSimpleContainerAnalysis(assetFile);
        verify(ffprobeSupplierResult, times(1)).getResult();
    }

    @Test
    void testHandle_save() throws Exception {
        final var ffprobeJAXB = FFprobeJAXB.load(
                """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <ffprobe>
                            <streams>
                                <stream index="0" codec_name="aac" codec_long_name="AAC (Advanced Audio Coding)" profile="LC" codec_type="audio" codec_tag_string="[0][0][0][0]" codec_tag="0x0000" mime_codec_string="mp4a.40.2" sample_fmt="fltp" sample_rate="48000" channels="2" channel_layout="stereo" bits_per_sample="0" initial_padding="1024" r_frame_rate="0/0" avg_frame_rate="0/0" time_base="1/1000" start_pts="0" start_time="0.000000" extradata_size="5">
                                    <disposition default="0" dub="0" original="0" comment="0" lyrics="0" karaoke="0" forced="0" hearing_impaired="0" visual_impaired="0" clean_effects="0" attached_pic="0" timed_thumbnails="0" non_diegetic="0" captions="0" descriptions="0" metadata="0" dependent="0" still_image="0" multilayer="0"/>
                                </stream>
                            </streams>

                            <format filename="../.demo-media-files/test-vp8.mkv" nb_streams="2" nb_programs="0" nb_stream_groups="0" format_name="matroska,webm" format_long_name="Matroska / WebM" start_time="0.000000" duration="5.021000" size="243933" bit_rate="388660" probe_score="100">
                            </format>
                        </ffprobe>
                        """);

        when(ffprobeSupplierResult.getResult()).thenReturn(ffprobeJAXB);
        when(storedOn.haveWorkingDir()).thenReturn(true);
        when(storedOn.haveRenderedDir()).thenReturn(true);
        when(storedOn.getActivityLimitPolicy()).thenReturn(BASE_PREVIEW);
        when(storedOn.makeWorkingFile(any(), eq(fileEntity))).thenReturn(workingFile);
        when(storedOn.getLocalInternalFile(fileEntity)).thenReturn(assetFile);

        metadataThesaurusService.presetMimeType("foo/bar");

        ffia.handle(fileEntity, eventType, storedOn);

        verify(mediaAssetService, times(1))
                .declareRenderedStaticFile(fileEntity, workingFile, "ffprobe.xml", true, 0, "ffprobe-base");

        final var assertThesaurus = metadataThesaurusService.getAssertThesaurus();
        assertThesaurus.technical().type().set("""
                Matroska / WebM, 00:00:05, 389 kbps
                audio: AAC LC stereo @ 48 kHz
                """);

        assertThesaurus.technicalAudio().channelLayout().set(0, "stereo");
        assertThesaurus.technicalAudio().channelsCount().set(0, 2);
        assertThesaurus.technicalAudio().sampleFormat().set(0, "fltp");
        assertThesaurus.technicalAudio().sampleRate().set(0, 48000);

        assertThesaurus.technicalContainer().duration().set(0, 5021);
        assertThesaurus.technicalContainer().formatName().set(0, "Matroska / WebM");
        assertThesaurus.technicalContainer().startTime().set(0, 0.0);
        assertThesaurus.technicalContainer().bitrate().set(0, 388660);
        assertThesaurus.technicalContainer().format().set(0, "matroska,webm");

        assertThesaurus.technicalStream().startTime().set(0, 0.0);
        assertThesaurus.technicalStream().type().set(0, "audio");
        assertThesaurus.technicalStream().codec().set(0, "aac");
        assertThesaurus.technicalStream().codecName().set(0, "AAC");
        assertThesaurus.technicalStream().profile().set(0, "LC");
        assertThesaurus.technicalStream().isSecondary().set(0, false);
        assertThesaurus.technicalStream().timeBase().set(0, "1/1000");

        metadataThesaurusService.assertMimeTypeEquals("foo/bar");
        metadataThesaurusService.check(ffia).check(fileEntity);

        verify(storedOn, times(1)).getLocalInternalFile(fileEntity);
        verify(storedOn, times(1)).haveWorkingDir();
        verify(storedOn, times(1)).haveRenderedDir();
        verify(storedOn, times(1)).getActivityLimitPolicy();
        verify(storedOn, times(1)).makeWorkingFile(any(), eq(fileEntity));
        verify(ffprobeSupplier, times(1)).processSimpleContainerAnalysis(assetFile);
        verify(ffprobeSupplierResult, times(1)).getResult();

        assertThat(workingFile).exists().isFile().isNotEmpty();
    }

    private static Stream<Arguments> provideDoubleBooleans() {
        return Stream.of(
                Arguments.of(false, false),
                Arguments.of(false, true),
                Arguments.of(true, false),
                Arguments.of(true, true));
    }

    @ParameterizedTest
    @MethodSource("provideDoubleBooleans")
    void testPatchInvalidAVMimeTypes_mpeg2ts(final boolean haveVideo, final boolean haveAudio) {
        metadataThesaurusService.presetMimeType("video/mp2t");
        ffia.patchInvalidAVMimeTypes(fileEntity, haveVideo, haveAudio);
        metadataThesaurusService.check(fileEntity);
    }

    @ParameterizedTest
    @MethodSource("provideDoubleBooleans")
    void testPatchInvalidAVMimeTypes_videoMime(final boolean haveVideo, final boolean haveAudio) {
        metadataThesaurusService.presetMimeType("video/" + lowerMimeType);
        ffia.patchInvalidAVMimeTypes(fileEntity, haveVideo, haveAudio);

        if (haveVideo == false && haveAudio) {
            metadataThesaurusService.assertMimeTypeEquals("audio/" + lowerMimeType);
            metadataThesaurusService.check(ffia);
        }

        metadataThesaurusService.check(fileEntity);
    }

    @ParameterizedTest
    @MethodSource("provideDoubleBooleans")
    void testPatchInvalidAVMimeTypes_audioMime(final boolean haveVideo, final boolean haveAudio) {
        metadataThesaurusService.presetMimeType("audio/" + lowerMimeType);
        ffia.patchInvalidAVMimeTypes(fileEntity, haveVideo, haveAudio);

        if (haveVideo) {
            metadataThesaurusService.assertMimeTypeEquals("video/" + lowerMimeType);
            metadataThesaurusService.check(ffia);
        }

        metadataThesaurusService.check(fileEntity);
    }

    @Test
    void testIsCanBeUsedInMasterAsPreview() {

    }

}
