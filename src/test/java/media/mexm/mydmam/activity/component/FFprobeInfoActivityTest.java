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

import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static media.mexm.mydmam.component.FFprobeSupplier.ALL_MIME_TYPES;
import static media.mexm.mydmam.component.FFprobeSupplier.FFPROBE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import java.io.File;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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

    @Mock
    FileEntity fileEntity;
    @Mock
    ActivityEventType eventType;
    @Mock
    RealmStorageConfiguredEnv storedOn;
    @Mock
    ProcessingToolResult<FFSourceDefinition, FFprobe, FFprobeJAXB, KeepStdoutAndErrToLogWatcher> ffprobeSupplierResult;

    @Fake
    String assetFilePath;

    @Autowired
    FlatMetadataThesaurusService metadataThesaurusService;
    @Autowired
    FFprobeInfoActivity ffia;

    File assetFile;

    @BeforeEach
    void init() {
        metadataThesaurusService.reset();

        assetFile = new File(assetFilePath);
        when(storedOn.getLocalInternalFile(fileEntity)).thenReturn(assetFile);
        when(ffprobeSupplier.processSimpleContainerAnalysis(assetFile)).thenReturn(ffprobeSupplierResult);
    }

    @AfterEach
    void ends() {
        metadataThesaurusService.check();
        verifyNoMoreInteractions(ffprobeSupplier);
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
        assertTrue(ffia.canHandle(fileEntity, eventType, storedOn));
        metadataThesaurusService.check(fileEntity);
        verify(storedOn, times(1)).isDAS();
    }

    @Test
    void testCanHandle_false() {
        metadataThesaurusService.presetMimeType("nope/nope");
        when(storedOn.isDAS()).thenReturn(true);
        assertFalse(ffia.canHandle(fileEntity, eventType, storedOn));
        metadataThesaurusService.check(fileEntity);
        verify(storedOn, times(1)).isDAS();
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
                video: ffv1 352×288 L123 @ 25 fps [8568 kbps] yuv420p/colTransfer:UNKNOWN default
                audio: pcm_s16le stereo @ 48 kHz default
                data: tmcd (Time Code Media Handler)
                attached picture (prores 720×576)
                still image (prores 1×2)
                timed thumbnails (prores 3×4)
                """);

        assertThesaurus.chapter().title().set(1, "Chap One");
        assertThesaurus.chapter().startTime().set(1, 1);
        assertThesaurus.chapter().endTime().set(1, 3000);

        assertThesaurus.technicalStream().referenceId().set(0, "0x1");
        assertThesaurus.technicalStream().startTime().set(0, "0.0");
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
        assertThesaurus.technicalVideo().averageFrameRate().set(0, "25");
        assertThesaurus.technicalVideo().fieldOrder().set(0, "progressive");
        assertThesaurus.technicalVideo().frameRate().set(0, "25");
        assertThesaurus.technicalVideo().referenceId().set(0, "0x1");
        assertThesaurus.technicalStream().level().set(0, "123");
        assertThesaurus.technicalImage().displayAspectRatio().set(0, "11:9");

        assertThesaurus.technicalStream().bitrate().set(1, 1536000);
        assertThesaurus.technicalStream().codec().set(1, "pcm_s16le");
        assertThesaurus.technicalStream().codecName().set(1, "PCM signed 16-bit little-endian");
        assertThesaurus.technicalStream().disposition().set(1, "default");
        assertThesaurus.technicalStream().isSecondary().set(1, false);
        assertThesaurus.technicalStream().referenceId().set(1, "0x2");
        assertThesaurus.technicalStream().startTime().set(1, "0.0");
        assertThesaurus.technicalStream().timeBase().set(1, "1/48000");
        assertThesaurus.technicalStream().type().set(1, "audio");
        assertThesaurus.technicalAudio().channelLayout().set(1, "stereo");
        assertThesaurus.technicalAudio().channelsCount().set(1, 2);
        assertThesaurus.technicalAudio().referenceId().set(1, "0x2");
        assertThesaurus.technicalAudio().sampleFormat().set(1, "s16");
        assertThesaurus.technicalAudio().sampleRate().set(1, "48000");

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
        metadataThesaurusService.presetMimeType("nope/nope");

        ffia.handle(fileEntity, eventType, storedOn);

        final var assertThesaurus = metadataThesaurusService.getAssertThesaurus();

        assertThesaurus.technical().type().set("""
                MXF (Material eXchange Format), 00:00:01, 105 Mbps
                video: jpeg2000 1920×1080 JPEG 2000 digital cinema 2K @ 25 fps xyz12le
                audio: pcm_s16le mono @ 48 kHz
                """);

        // TODO check mxf...
    }

    @Test
    void testGetPrograms() {

    }

    @Test
    void testSetChapters() {

    }

    @Test
    void testRemoveUnknown() {

    }

    @Test
    void testGetTagByName() {

    }

    @Test
    void testSaveFFprobeXMLFile() {

    }

    @Test
    void testPatchInvalidAVMimeTypes() {

    }

    @Test
    void testSetMediaSummary() {

    }

    @Test
    void testIsCanBeUsedInMasterAsPreview() {

    }

}
