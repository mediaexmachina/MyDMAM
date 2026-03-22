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
package media.mexm.mydmam.tools;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.lang.Runtime.getRuntime;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.write;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static tv.hd3g.processlauncher.CapturedStreams.BOTH_STDOUT_STDERR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.jayway.jsonpath.JsonPath;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.component.XmlMapperWrapper;
import media.mexm.mydmam.configuration.MagickConf;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.ExecutionTimeLimiter;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Slf4j
public class ImageMagick {

    private static final String POLICY_RESOURCE = "resource";
    private static final String POLICY_CODER = "coder";

    private final ExecutableFinder executableFinder;
    private final ScheduledExecutorService maxExecTimeScheduler;
    private final MyDMAMConfigurationProperties configuration;
    private final XmlMapperWrapper xmlMapper;
    private final ObjectMapper objectMapper;

    private String execName;
    private File magickConfigurationDir;
    private ExecutionTimeLimiter executionTimeLimiter;
    private File iccSRGBProfile;

    @Getter
    private String magickVersion;
    @Getter
    private boolean enabled;

    public ImageMagick(final ExecutableFinder executableFinder,
                       final ScheduledExecutorService maxExecTimeScheduler,
                       final MyDMAMConfigurationProperties configuration,
                       final XmlMapperWrapper xmlMapper,
                       final ObjectMapper objectMapper) {
        this.executableFinder = executableFinder;
        this.maxExecTimeScheduler = maxExecTimeScheduler;
        this.configuration = configuration;
        this.xmlMapper = xmlMapper;
        this.objectMapper = objectMapper;
        enabled = false;
    }

    public void init() {
        final var magickConf = configuration.magick();
        if (magickConf == null) {
            return;
        }

        final var magickMaxExecTimeSeconds = magickConf.maxExecTime().toSeconds();
        executionTimeLimiter = new ExecutionTimeLimiter(
                magickMaxExecTimeSeconds + 1, SECONDS, maxExecTimeScheduler);

        execName = "magick";
        File execFile;
        try {
            log.debug("Try to found ImageMagick binary with {}", execName);
            execFile = executableFinder.get(execName);
        } catch (final FileNotFoundException e) {
            try {
                execName = "convert";
                log.debug("Fail. Now, try to found ImageMagick binary with {}", execName);
                execFile = executableFinder.get(execName);
            } catch (final FileNotFoundException e2) {
                log.warn("Can't found ImageMagick binary (magick / convert), disable all operations with it");
                return;
            }
        }

        final var tempDir = prepareTempDir(magickConf);
        magickConfigurationDir = preparePolicyFile(xmlMapper, magickConf, magickMaxExecTimeSeconds, tempDir);

        try {
            magickVersion = getVersion();
            final var majorVersion = Integer.valueOf(magickVersion.split("\\.")[0]);
            if (majorVersion < 6 || majorVersion > 7) {
                log.warn("Use ImageMagick version {} (probably unsupported) from {}", magickVersion, execFile);
            } else {
                log.info("Use ImageMagick version {} from {}", magickVersion, execFile);
            }
            enabled = true;
        } catch (final IOException e) {
            log.error("Can't exec ImageMagick, disable all operations with it", e);
        }

        iccSRGBProfile = extractICCFile(magickConfigurationDir);
    }

    /**
     * @return configurationDir
     */
    private static File preparePolicyFile(final XmlMapperWrapper xmlMapper,
                                          final MagickConf magickConf,
                                          final long magickMaxExecTimeSeconds,
                                          final File tempDir) {
        try {
            final var configurationDir = new File(magickConf.confDir())
                    .getAbsoluteFile()
                    .getCanonicalFile();
            forceMkdir(configurationDir);

            final var policyFile = new File(configurationDir, "policy.xml");

            if (policyFile.exists() == false) {
                log.info("Write to {} the policy file", policyFile);

                final var maxThreadCount = magickConf.maxThreadCount();
                final var availableProcessors = getRuntime().availableProcessors();
                final var usableProcessors = availableProcessors == 1 ? 1 : availableProcessors / 2;
                final var cpuCount = maxThreadCount == 0 ? usableProcessors : maxThreadCount;

                final List<Policy> policies = List.of(
                        new PolicyRight(POLICY_CODER, "none", "HTTP"),
                        new PolicyRight(POLICY_CODER, "none", "HTTPS"),
                        new PolicyRight(POLICY_CODER, "none", "FTP"),
                        new PolicyRight("delegate", "none", "*"),
                        new PolicyRight("filter", "none", "*"),
                        new PolicyRight("path", "none", "@*"),
                        new PolicyRight("module", "none", "{MSL,MVG,PS,SVG,URL,XPS}"),
                        new PolicyKV(POLICY_RESOURCE, "time", String.valueOf(magickMaxExecTimeSeconds)),
                        new PolicyKV(POLICY_RESOURCE, "thread", String.valueOf(cpuCount)),
                        new PolicyKV(POLICY_RESOURCE, "memory", magickConf.maxMemory() + "MiB"),
                        new PolicyKV(POLICY_RESOURCE, "map", magickConf.maxMap() + "MiB"),
                        new PolicyKV(POLICY_RESOURCE, "disk", magickConf.maxDisk() + "MiB"),
                        new PolicyKV(POLICY_RESOURCE, "width", magickConf.maxWidth() + "P"),
                        new PolicyKV(POLICY_RESOURCE, "height", magickConf.maxHeight() + "P"),
                        new PolicyKV(POLICY_RESOURCE, "temporary-path", tempDir.getPath()),
                        new PolicyKV("cache", "synchronize", "true"),
                        new PolicyKV("system", "memory-map", "anonymous"),
                        new PolicyKV("system", "max-memory-request", magickConf.maxMemoryRequest() + "MiB"));

                xmlMapper.getXmlMapper().writer()
                        .withRootName("policymap")
                        .with(INDENT_OUTPUT)
                        .writeValue(policyFile, new Policymap(policies));
            }
            return configurationDir;
        } catch (final IOException e) {
            throw new UncheckedIOException("Can't prepare/write policy.xml file", e);
        }
    }

    private static File extractICCFile(final File magickConfigurationDir) {
        try {
            final var iccFile = new File(magickConfigurationDir, "srgb.icc");

            if (iccFile.exists() == false) {
                log.info("Write to {} the ICC SRGB profile file", iccFile);
                final var iccProfileResource = new ClassPathResource("srgb.icc");
                writeByteArrayToFile(iccFile, iccProfileResource.getContentAsByteArray(), false);
            }

            return iccFile;
        } catch (final IOException e) {
            throw new UncheckedIOException("Can't extract internal \"srgb.icc\" file", e);
        }
    }

    private static File prepareTempDir(final MagickConf magickConf) {
        final File tempDir;
        try {
            tempDir = new File(magickConf.tempDir())
                    .getAbsoluteFile()
                    .getCanonicalFile();
            forceMkdir(tempDir);
            log.debug("Use {} as temp directory", tempDir);
            return tempDir;
        } catch (final IOException e) {
            throw new UncheckedIOException("Can't prepare magick temp directory: " + magickConf.tempDir(), e);
        }
    }

    private CapturedStdOutErrTextRetention runMagick(final CommandLine commandLine,
                                                     final File workingDir) throws IOException {
        final var processlauncherBuilder = new ProcesslauncherBuilder(commandLine);
        final var capText = new CapturedStdOutErrTextRetention();
        processlauncherBuilder.getSetCaptureStandardOutputAsOutputText(BOTH_STDOUT_STDERR)
                .addObserver(capText);
        processlauncherBuilder.setExecutionTimeLimiter(executionTimeLimiter);
        processlauncherBuilder.setWorkingDirectory(workingDir);
        processlauncherBuilder.setEnvironmentVarIfNotFound(
                "MAGICK_CONFIGURE_PATH", magickConfigurationDir.getPath());
        processlauncherBuilder.start().checkExecution();

        if (log.isDebugEnabled()) {
            final var env = new HashMap<String, String>();
            processlauncherBuilder.forEachEnvironmentVar(env::put);
            log.debug("RunMagick: {} ; [{}] env: {}", commandLine, workingDir, env);
        }

        return capText;
    }

    /**
     * @return like "7.1.2-13"
     */
    private String getVersion() throws IOException {
        final var commandLine = new CommandLine(execName, "-version", executableFinder);
        final var capText = runMagick(commandLine, new File(".").getAbsoluteFile());

        final var versionLine = capText.getStdoutLines(false)
                .filter(l -> l.toLowerCase().startsWith("version"))
                .findFirst()
                .orElseThrow(() -> new IOException("Can't extract version string from ImageMagick " + commandLine));

        /**
         * Version: ImageMagick 7.1.2-13 Q16-HDRI x64 dd991e2:20260119 https://imagemagick.org
         */
        final var items = Arrays.asList(versionLine.split(" "));
        if (items.size() < 3) {
            throw new IOException("Invalid version string from ImageMagick: \"" + versionLine + "\", " + commandLine);
        }

        return items.get(2);
    }

    private void checkDisabled() throws IOException {
        if (enabled == false) {
            throw new IOException("ImageMagick is disabled");
        }
    }

    public JsonPathHelper extractIdentifyJsonFile(final File source, final File saveJsonDest) throws IOException {
        checkDisabled();
        log.info("Start to read image from {}", source);

        final var params = new Parameters();
        params.addParameters(source.getName(), "json:-");
        final var commandLine = new CommandLine(execName, params, executableFinder);
        final var capText = runMagick(commandLine, source.getParentFile());

        final var rawJson = capText.getStdout(false, "\n").trim();
        if (rawJson.isEmpty()) {
            throw new IOException(
                    "ImageMagick as returned an empty string from stdout, via " + commandLine.toString()
                                  + " ; from \"" + source.getParentFile() + "\"");
        }

        final var jsonRoot = objectMapper.readTree(rawJson);
        if (jsonRoot.isArray() == false) {
            throw new IOException("Invalid JSON (root is not an array): \"" + rawJson.replace("\n", " ") + "\"");
        }
        final var jsonItems = jsonRoot.valueStream().toList();
        if (jsonItems.isEmpty()) {
            throw new IOException("Invalid JSON (empty root array): \"" + rawJson.replace("\n", " ") + "\"");
        }

        final var selectedNode = jsonItems.get(0);

        final var cleanedJson = objectMapper.writeValueAsString(selectedNode);

        log.info("Write extracted JSON to {}", saveJsonDest);
        write(saveJsonDest, cleanedJson, UTF_8, false);

        return new JsonPathHelper(JsonPath.parse(cleanedJson));
    }

    /**
     * @param parameters setup &lt;%INPUTFILE%&gt;, &lt;%OUTPUTFILE%&gt; and &lt;%ICCPROFILE%&gt; vars
     */
    public void convertImage(final String parameters, final File source, final File dest) throws IOException {
        checkDisabled();
        log.info("Start to process thumbnail image from \"{}\" to \"{}\"", source, dest);

        final var params = Parameters.bulk(parameters)
                .directInjectVariable(
                        "INPUTFILE", source.getAbsolutePath(),
                        "ICCPROFILE", iccSRGBProfile.getAbsolutePath(),
                        "OUTPUTFILE", dest.getAbsolutePath());

        final var commandLine = new CommandLine(execName, params, executableFinder);
        runMagick(commandLine, source.getParentFile());
    }

    public Set<String> getManagedRasterMimeTypes() {
        return Set.of(
                "image/jpeg",
                "image/png",
                "image/bmp",
                "image/gif",
                "image/vnd.adobe.photoshop",
                "image/tiff",
                "image/jp2",
                "application/dicom",
                "image/x-icon",
                "image/pict",
                "image/vndwapwbmp",
                "image/x-pcx",
                "image/x-portable-bitmap",
                "image/x-xbm",
                "image/xpm",
                "image/cineon",
                "image/dpx",
                "image/tga",
                "image/exr",
                "image/vnd.radiance",
                "image/webp",
                "image/sgi",
                "image/x-palm-pixmap",
                "image/x-g3-fax",
                "image/jpcd",
                "image/x-sct",
                "image/jbig",
                "image/x-miff",
                "image/x-sun");
    }

    public static record Policymap(@JacksonXmlElementWrapper(useWrapping = false) List<Policy> policy) {

    }

    public interface Policy {

    }

    public static record PolicyRight(@JacksonXmlProperty(isAttribute = true) String domain,
                                     @JacksonXmlProperty(isAttribute = true) String rights,
                                     @JacksonXmlProperty(isAttribute = true) String pattern) implements Policy {
    }

    public static record PolicyKV(@JacksonXmlProperty(isAttribute = true) String domain,
                                  @JacksonXmlProperty(isAttribute = true) String name,
                                  @JacksonXmlProperty(isAttribute = true) String value) implements Policy {
    }

}
