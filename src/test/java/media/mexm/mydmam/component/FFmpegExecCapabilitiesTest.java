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

import static media.mexm.mydmam.component.FFmpegExecCapabilities.BARS1K_MKV;
import static media.mexm.mydmam.component.FFmpegExecCapabilities.FFMPEG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.processlauncher.cmdline.Parameters.bulk;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import media.mexm.mydmam.tools.ExternalExecCapabilityEvaluator;
import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;

@ExtendWith(MockToolsExtendsJunit.class)
class FFmpegExecCapabilitiesTest {

    @Fake
    String codecName;
    @Mock
    ExternalExecCapabilityEvaluator externalExecCapabilityEvaluator;
    @Mock
    ExternalExecCapabilities externalExecCapabilities;
    @Mock
    CapturedStdOutErrTextRetention capturedStdOutErrTextRetention;
    @Captor
    ArgumentCaptor<Predicate<ExternalExecCapabilityEvaluator>> evaluatorCaptor;

    FFmpegExecCapabilities ffec;

    @BeforeEach
    void init() {
        ffec = new FFmpegExecCapabilities(externalExecCapabilities);
    }

    @Test
    void testInternalServiceStart() throws Exception {
        when(externalExecCapabilities.getPassingPlaybookNames(FFMPEG))
                .thenReturn(Set.of(codecName, "libx265", "hevc_d3d12va"));

        ffec.internalServiceStart();

        verify(externalExecCapabilities, times(1)).setup(FFMPEG, List.of(bulk(
                "-y -f lavfi -i smptehdbars=duration=1:size=640x360:rate=25 -f lavfi -i sine=duration=1:frequency=1000:sample_rate=48000 -c:v ffv1 -c:a pcm_s16le bars1k.mkv")));
        verify(externalExecCapabilities, times(1)).tearDown(FFMPEG);
        verify(externalExecCapabilities, times(1)).getPassingPlaybookNames(FFMPEG);
        verify(externalExecCapabilities, atLeastOnce()).addPlaybook(eq(FFMPEG), anyString(), any(), any());
        verify(externalExecCapabilities, times(1)).addPlaybook(eq(FFMPEG), eq("libvpx-vp9"), any(), any());
        assertThat(ffec.getAvaliableCodecs()).isEqualTo(Map.of("hevc", "hevc_d3d12va"));
    }

    void checkMakePlaybookCodec(final boolean video) {
        final var outFileName = codecName + ".mp4";
        final var codecsOpts = video ? "-codec:v" : "-codec:a";
        final var disableCodec = video ? "-an" : "-vn";

        verify(externalExecCapabilities, times(1))
                .addPlaybook(eq(FFMPEG), eq(codecName), eq(bulk(
                        "-loglevel error -y -i", BARS1K_MKV, codecsOpts, codecName, disableCodec, outFileName)),
                        evaluatorCaptor.capture());

        when(externalExecCapabilityEvaluator.captured()).thenReturn(capturedStdOutErrTextRetention);
        when(externalExecCapabilityEvaluator.haveReturnCode(0)).thenReturn(true);
        when(externalExecCapabilityEvaluator.haveFile(anyString())).thenReturn(true);
        when(capturedStdOutErrTextRetention.getStderr(anyBoolean(), anyString())).thenReturn("?");

        assertTrue(evaluatorCaptor.getValue().test(externalExecCapabilityEvaluator));

        when(externalExecCapabilityEvaluator.haveReturnCode(0)).thenReturn(false);
        assertFalse(evaluatorCaptor.getValue().test(externalExecCapabilityEvaluator));

        verify(externalExecCapabilityEvaluator, atLeastOnce()).captured();
        verify(externalExecCapabilityEvaluator, atLeastOnce()).haveReturnCode(0);
        verify(externalExecCapabilityEvaluator, atLeastOnce()).haveFile(anyString());
        verify(capturedStdOutErrTextRetention, atLeastOnce()).getStderr(anyBoolean(), anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testMakePlaybookCodec(final boolean video) {
        ffec.makePlaybookCodec(codecName, video);
        checkMakePlaybookCodec(video);
    }

    @Test
    void testMakePlaybookVCodec() {
        ffec.makePlaybookCodec(codecName, true);
        checkMakePlaybookCodec(true);
    }

    @Test
    void testMakePlaybookACodec() {
        ffec.makePlaybookCodec(codecName, false);
        checkMakePlaybookCodec(false);
    }

    @Test
    void testGetInternalServiceName() {
        assertThat(ffec.getInternalServiceName()).isNotBlank();
    }

    @Test
    void testGetAvaliableCodecs() {
        assertThat(ffec.getAvaliableCodecs()).isEqualTo(Map.of());
    }

}
