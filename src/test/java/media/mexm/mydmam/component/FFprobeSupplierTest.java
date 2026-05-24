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

import static java.io.File.createTempFile;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static media.mexm.mydmam.component.FFprobeSupplier.FFPROBE;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static tv.hd3g.processlauncher.cmdline.Parameters.bulk;

import java.io.FileNotFoundException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import media.mexm.mydmam.configuration.ExternalToolsConf;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.tools.ExternalExecCapabilityEvaluator;
import net.datafaker.Faker;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;
import tv.hd3g.fflauncher.FFprobe;
import tv.hd3g.fflauncher.processingtool.FFSourceDefinition;
import tv.hd3g.fflauncher.recipes.ProbeMedia;
import tv.hd3g.ffprobejaxb.FFprobeJAXB;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.processingtool.KeepStdoutAndErrToLogWatcher;
import tv.hd3g.processlauncher.processingtool.ProcessingToolResult;

@ExtendWith(MockToolsExtendsJunit.class)
class FFprobeSupplierTest {

    static ExecutableFinder executableFinder = new ExecutableFinder();
    static ScheduledExecutorService maxExecTimeScheduler = newScheduledThreadPool(1);

    @Mock
    ExternalExecCapabilities externalExecCapabilities;
    @Mock
    MyDMAMConfigurationProperties configuration;
    @Mock
    ExternalToolsConf tools;
    @Mock
    ExternalExecCapabilityEvaluator evaluator;
    @Mock
    CapturedStdOutErrTextRetention capturedStdOutErrTextRetention;
    @Captor
    ArgumentCaptor<Predicate<ExternalExecCapabilityEvaluator>> evaluatorCaptor;

    @Mock
    ProbeMedia probeMedia;
    @Mock
    ProcessingToolResult<FFSourceDefinition, FFprobe, FFprobeJAXB, KeepStdoutAndErrToLogWatcher> processSimpleContainerAnalysisResult;

    FFprobeSupplier ffps;
    boolean foundedBinary;

    class FFprobeSupplierForMock extends FFprobeSupplier {

        public FFprobeSupplierForMock() {
            super(externalExecCapabilities, executableFinder, maxExecTimeScheduler, configuration);
        }

        @Override
        protected ProbeMedia makeProbeMedia() {
            return probeMedia;
        }

    }

    @BeforeEach
    void init() {
        try {
            executableFinder.get(FFPROBE);
            when(externalExecCapabilities.getPassingPlaybookNames(FFPROBE)).thenReturn(Set.of("run"));
            foundedBinary = true;
        } catch (final FileNotFoundException e) {
            when(externalExecCapabilities.getPassingPlaybookNames(FFPROBE)).thenReturn(Set.of());
            foundedBinary = false;
        }

        ffps = new FFprobeSupplierForMock();
    }

    @Test
    void testGetInternalServiceName() {
        assertThat(ffps.getInternalServiceName()).isEqualTo(FFPROBE);
    }

    @Test
    void testInternalServiceStart() throws Exception {
        ffps.internalServiceStart();

        when(evaluator.haveReturnCode(0)).thenReturn(true);
        when(evaluator.haveStringInStdOutErr("ffprobe version")).thenReturn(true);
        when(evaluator.captured()).thenReturn(capturedStdOutErrTextRetention);
        when(capturedStdOutErrTextRetention.getStdouterrLines(false)).then(_ -> Stream.of("ffprobe version 123"));

        verify(externalExecCapabilities, atLeast(0))
                .addPlaybook(eq(FFPROBE), eq("run"), eq(bulk("-version")), evaluatorCaptor.capture());
        evaluatorCaptor.getValue().test(evaluator);

        verify(externalExecCapabilities, atLeast(0))
                .addPlaybook(eq(FFPROBE), eq("run"), eq(bulk("-version")), evaluatorCaptor.capture());
        Optional.ofNullable(evaluatorCaptor.getValue()).ifPresent(c -> c.test(evaluator));

        verify(evaluator, atLeast(0)).haveReturnCode(0);
        verify(evaluator, atLeast(0)).captured();
        verify(evaluator, atLeast(0)).haveStringInStdOutErr("ffprobe version");
        verify(capturedStdOutErrTextRetention, atLeast(0)).getStdouterrLines(anyBoolean());

        verify(externalExecCapabilities, atLeast(0))
                .tearDown(FFPROBE);
        verify(externalExecCapabilities, atLeast(0))
                .getPassingPlaybookNames(FFPROBE);
        assertThat(ffps.isEnabled()).isEqualTo(foundedBinary);
    }

    @Test
    void testProcessSimpleContainerAnalysis() throws Exception {
        when(externalExecCapabilities.getPassingPlaybookNames(FFPROBE)).thenReturn(Set.of("run"));
        ffps.internalServiceStart();
        verify(externalExecCapabilities, atLeast(1)).addPlaybook(eq(FFPROBE), eq("run"), eq(bulk("-version")), any());
        verify(externalExecCapabilities, atLeast(1)).tearDown(FFPROBE);
        verify(externalExecCapabilities, atLeast(1)).getPassingPlaybookNames(FFPROBE);
        assertTrue(ffps.isEnabled());

        final var source = createTempFile("mydmam-" + getClass().getSimpleName(), "source");
        when(probeMedia.process(source.getName())).thenReturn(processSimpleContainerAnalysisResult);
        final var maxExecTime = ofMillis(Faker.instance().random().nextInt(0, 100_000));
        when(configuration.tools()).thenReturn(tools);
        when(tools.ffprobeSimpleContainerAnalysisMaxExecTime()).thenReturn(maxExecTime);

        final var result = ffps.processSimpleContainerAnalysis(source);
        assertThat(result).isEqualTo(processSimpleContainerAnalysisResult);
        deleteQuietly(source);

        verify(configuration, times(1)).tools();
        verify(tools, times(1)).ffprobeSimpleContainerAnalysisMaxExecTime();
        verify(probeMedia, times(1)).setExecutableFinder(executableFinder);
        verify(probeMedia, times(1)).setMaxExecutionTime(maxExecTime, maxExecTimeScheduler);
        verify(probeMedia, times(1)).setWorkingDirectory(source.getParentFile());
        verify(probeMedia, times(1)).process(source.getName());
    }

    @Test
    void testIsEnabled() {
        assertFalse(ffps.isEnabled());
    }

}
