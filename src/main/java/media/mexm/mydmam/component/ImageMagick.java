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

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.lang.Runtime.getRuntime;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.write;
import static tv.hd3g.processlauncher.CapturedStreams.BOTH_STDOUT_STDERR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.jayway.jsonpath.JsonPath;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.tools.JsonPathHelper;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.ExecutionTimeLimiter;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

@Component
@Slf4j
public class ImageMagick {

	private static final String POLICY_RESOURCE = "resource";
	private static final String POLICY_CODER = "coder";
	public static final String EXEC_NAME = "magick";
	private final ExecutableFinder executableFinder;
	private final ExecutionTimeLimiter executionTimeLimiter;
	private final File magickConfigurationDir;
	private final ObjectMapper objectMapper;

	@Getter
	private String magickVersion;
	@Getter
	private boolean enabled;

	public ImageMagick(@Autowired final ExecutableFinder executableFinder,
					   @Autowired final ScheduledExecutorService maxExecTimeScheduler,
					   @Autowired final MyDMAMConfigurationProperties configuration,
					   @Autowired final XmlMapperWrapper xmlMapper,
					   @Autowired final ObjectMapper objectMapper) {
		this.executableFinder = executableFinder;
		this.objectMapper = objectMapper;
		final var magickConf = configuration.magick();
		if (magickConf == null) {
			enabled = false;
			magickVersion = null;
			magickConfigurationDir = null;
			executionTimeLimiter = null;
			return;
		}

		final var magickMaxExecTimeSeconds = magickConf.maxExecTime().toSeconds();
		executionTimeLimiter = new ExecutionTimeLimiter(
				magickMaxExecTimeSeconds + 1, SECONDS, maxExecTimeScheduler);

		final File execFile;
		try {
			execFile = executableFinder.get(EXEC_NAME);
		} catch (final FileNotFoundException e) {
			log.warn("Can't found ImageMagick binary ({}), disable all operations with it", EXEC_NAME);
			enabled = false;
			magickVersion = null;
			magickConfigurationDir = null;
			return;
		}

		try {
			magickConfigurationDir = new File(magickConf.confDir())
					.getAbsoluteFile()
					.getCanonicalFile();
			forceMkdir(magickConfigurationDir);

			final var tempDir = new File(magickConf.tempDir())
					.getAbsoluteFile()
					.getCanonicalFile();
			forceMkdir(tempDir);

			log.debug("Write to {} the policy.xml file, with {} as temp directory", magickConfigurationDir, tempDir);

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
					.writeValue(new File(magickConfigurationDir, "policy.xml"), new Policymap(policies));

		} catch (final IOException e) {
			throw new UncheckedIOException("Can't write policy.xml file", e);
		}

		try {
			magickVersion = getVersion();
			final var majorVersion = Integer.valueOf(magickVersion.split("\\.")[0]);
			if (majorVersion != 7) {
				log.warn("Use ImageMagick version {} (probably unsupported) from {}", magickVersion, execFile);
			} else {
				log.info("Use ImageMagick version {} from {}", magickVersion, execFile);
			}
			enabled = true;
		} catch (final IOException e) {
			log.error("Can't exec ImageMagick, disable all operations with it", e);
			enabled = false;
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
		return capText;
	}

	/**
	 * @return like "7.1.2-13"
	 */
	private String getVersion() throws IOException {
		final var commandLine = new CommandLine(EXEC_NAME, "-version", executableFinder);
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
			throw new IOException(EXEC_NAME + " is disabled");
		}
	}

	public JsonPathHelper extractIdentifyJsonFile(final File source, final File saveJsonDest) throws IOException {
		checkDisabled();
		log.info("Start to read image from {}", source);

		final var params = new Parameters();
		params.addParameters(source.getName(), "json:-");
		final var commandLine = new CommandLine(EXEC_NAME, params, executableFinder);
		final var capText = runMagick(commandLine, source.getParentFile());

		final var rawJson = capText.getStdout(false, "\n");

		final var jsonRoot = objectMapper.readTree(rawJson);
		if (jsonRoot.isArray() == false) {
			throw new IOException("Invalid JSON (root is not an array): " + rawJson.replace("\n", " "));
		}
		final var jsonItems = jsonRoot.valueStream().toList();
		if (jsonItems.size() > 1) {
			throw new IOException("Invalid JSON (to big root array): " + rawJson.replace("\n", " "));
		}

		final var selectedNode = jsonItems.get(0);

		final var cleanedJson = objectMapper.writeValueAsString(selectedNode);

		log.info("Write extracted JSON to {}", saveJsonDest);
		write(saveJsonDest, cleanedJson, UTF_8, false);

		return new JsonPathHelper(JsonPath.parse(cleanedJson));
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
