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

import static java.lang.Integer.compare;
import static java.lang.Math.round;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static java.util.stream.Stream.empty;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.apache.commons.io.FileUtils.forceMkdirParent;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.isExtension;
import static tv.hd3g.processlauncher.CapturedStreams.BOTH_STDOUT_STDERR;
import static tv.hd3g.processlauncher.cmdline.Parameters.bulk;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefPDF;
import media.mexm.mydmam.tools.ExternalExecCapabilityEvaluator;
import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.ExecutionTimeLimiter;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

@Slf4j
@Component
public class XPDF implements InternalService {

    private static final String VERSION = "version";

    public static final char FORM_FEED = (char) 0x0C;

    private static final String PDFFILE = "pdffile";
    private static final String PDFINFO = "pdfinfo";
    private static final String PDFTOPPM = "pdftoppm";
    private static final String PDFTOTEXT = "pdftotext";
    private static final String USE_XPDF = "Use XPDF {}";

    private final ExecutableFinder executableFinder;
    private final ScheduledExecutorService maxExecTimeScheduler;
    private final MyDMAMConfigurationProperties configuration;
    private final ExternalExecCapabilities externalExecCapabilities;

    private ExecutionTimeLimiter executionTimeLimiter;

    private File tempDir;
    @Getter
    private boolean enabledPdfInfo;
    @Getter
    private boolean enabledPdfToPpm;
    @Getter
    private boolean enabledPdfToText;

    private int maxPageCount;
    private int resolution;

    public XPDF(@Autowired final ExecutableFinder executableFinder,
                @Autowired final ScheduledExecutorService maxExecTimeScheduler,
                @Autowired final MyDMAMConfigurationProperties configuration,
                @Autowired final ExternalExecCapabilities externalExecCapabilities) {
        this.executableFinder = executableFinder;
        this.maxExecTimeScheduler = maxExecTimeScheduler;
        this.configuration = configuration;
        this.externalExecCapabilities = externalExecCapabilities;
        enabledPdfInfo = false;
        enabledPdfToPpm = false;
        enabledPdfToText = false;
        maxPageCount = 100_000;
        resolution = 200;
    }

    @Override
    public String getInternalServiceName() {
        return "XPDF";
    }

    @Override
    public void internalServiceStart() throws Exception {
        final var xpdfConf = configuration.tools().xpdf();
        if (xpdfConf == null) {// NOSONAR S2583 - for test purposes (mock conf)
            return;
        }

        maxPageCount = xpdfConf.maxPageCount();
        resolution = xpdfConf.resolution();
        tempDir = new File(xpdfConf.tempDir()).getAbsoluteFile().getCanonicalFile();

        final var magickMaxExecTimeSeconds = xpdfConf.maxExecTime().toSeconds();
        executionTimeLimiter = new ExecutionTimeLimiter(
                magickMaxExecTimeSeconds + 1, SECONDS, maxExecTimeScheduler);

        final var param = bulk("-v");
        externalExecCapabilities.addPlaybook(PDFINFO, "info", param, this::checkAndEvaluate);
        externalExecCapabilities.tearDown(PDFINFO);
        enabledPdfInfo = externalExecCapabilities.getPassingPlaybookNames(PDFINFO).contains("info");

        externalExecCapabilities.addPlaybook(PDFTOPPM, "image", param, this::checkAndEvaluate);
        externalExecCapabilities.tearDown(PDFTOPPM);
        enabledPdfToPpm = externalExecCapabilities.getPassingPlaybookNames(PDFTOPPM).contains("image");

        externalExecCapabilities.addPlaybook(PDFTOTEXT, "text", param, this::checkAndEvaluate);
        externalExecCapabilities.tearDown(PDFTOTEXT);
        enabledPdfToText = externalExecCapabilities.getPassingPlaybookNames(PDFTOTEXT).contains("text");
    }

    private boolean checkAndEvaluate(final ExternalExecCapabilityEvaluator evaluator) {
        if (evaluator.haveReturnCode(0, 99) == false
            || evaluator.haveStringInStdOutErr(VERSION) == false) {
            return false;
        }
        log.info(USE_XPDF, evaluator.captured().getStdouterrLines(false)
                .filter(l -> l.toLowerCase().contains(VERSION))
                .findFirst().orElse(""));
        return true;
    }

    /**
     * @return like "encrypted" &gt; "AES 128-bit"
     */
    public Map<String, String> pdfInfo(final File file) throws IOException {
        if (enabledPdfInfo == false) {
            throw new IOException("XPDF pdfinfo is disabled");
        }

        final var params = bulk("-rawdates -f 1 -l <%maxPageCount%> <%pdffile%>")
                .directInjectVariable("maxPageCount", String.valueOf(maxPageCount))
                .directInjectVariable(PDFFILE, file.getAbsolutePath());
        final var commandLine = new CommandLine(PDFINFO, params, executableFinder);

        final var processlauncherBuilder = new ProcesslauncherBuilder(commandLine);
        final var capText = new CapturedStdOutErrTextRetention();
        processlauncherBuilder.getSetCaptureStandardOutputAsOutputText(BOTH_STDOUT_STDERR)
                .addObserver(capText);
        processlauncherBuilder.setExecutionTimeLimiter(executionTimeLimiter);
        processlauncherBuilder.setWorkingDirectory(tempDir);
        processlauncherBuilder.start().checkExecution();

        return capText.getStdoutLines(false)
                .flatMap(XPDF::splitFirstColon)
                .collect(toUnmodifiableMap(Entry::getKey, Entry::getValue));
    }

    public static record PageInfo(int page, float w, float h, int rotated) {
        public String getWMm() {
            return ptsToMm(w);
        }

        public String getHMm() {
            return ptsToMm(h);
        }

    }

    public static String ptsToMm(final float pts) {
        return String.valueOf(round(pts / (72.0f / 25.4f) * 10.0) / 10.0);
    }

    public List<PageInfo> extractPagesFormats(final Map<String, String> pdfInfo, final Integer maxPageCount) {
        return pdfInfo.entrySet()
                .stream()
                .filter(entry -> entry.getKey().toLowerCase().startsWith("page"))
                .filter(entry -> entry.getKey().equalsIgnoreCase("pages") == false)
                .filter(entry -> entry.getKey().contains("size"))
                .flatMap(entry -> {
                    /**
                     * Like "Page 3 size"
                     */
                    final var key = entry.getKey();
                    final var pageNum = Integer.parseInt(key
                            .substring("page".length(), key.length() - "size".length())
                            .trim());

                    /**
                     * Like "413.637 x 651.315 pts (A4) (rotated 0 degrees)"
                     * Like "413.637 x 651.315 pts (rotated 0 degrees)"
                     * ........[0]..[1]..[2]...[3]....[4]..[5]...[6]
                     */
                    final var rawValue = entry.getValue();
                    final var vEntries = StringUtils.split(rawValue);
                    if (vEntries.length < 4
                        || vEntries[1].equals("x") == false
                        || vEntries[3].equals("pts") == false) {
                        log.warn("Can't parse info page: \"{}\" > \"{}\"", key, rawValue);
                        return empty();
                    }

                    var rotated = 0;
                    if (vEntries[vEntries.length - 1].equals("degrees)")
                        && vEntries[vEntries.length - 3].equals("(rotated")) {
                        rotated = Integer.parseInt(vEntries[vEntries.length - 2]);
                    }

                    return Stream.of(new PageInfo(
                            pageNum,
                            Float.parseFloat(vEntries[0]),
                            Float.parseFloat(vEntries[2]),
                            rotated));
                })
                .sorted((l, r) -> compare(l.page, r.page))
                .limit(maxPageCount)
                .toList();
    }

    public Optional<String> getInfo(final Map<String, String> pdfInfo, final String key) {
        return Optional.ofNullable(pdfInfo.get(key.toLowerCase()));
    }

    public void extractPermissions(final Map<String, String> pdfInfo,
                                   final MtdThesaurusDefPDF pdfWriter) {
        getInfo(pdfInfo, "Permissions")
                .filter(not(String::isBlank))
                .map(p -> Stream.of(StringUtils.split(p.toLowerCase()))
                        .flatMap(sub -> {
                            final var entry = StringUtils.split(sub, ":");
                            if (entry.length != 2) {
                                log.warn("Can't parse sub: \"{}\" (can't found colon), on \"{}\"", sub, p);
                                return empty();
                            }
                            final var rawValue = entry[1];
                            final String value;
                            if (rawValue.equalsIgnoreCase("yes")) {
                                value = "true";
                            } else if (rawValue.equalsIgnoreCase("no")) {
                                value = "false";
                            } else {
                                value = rawValue;
                            }
                            return Stream.of(Map.entry(entry[0].toLowerCase(), value));
                        })
                        .collect(toUnmodifiableMap(Entry::getKey, Entry::getValue)))
                .ifPresentOrElse(permission -> {
                    pdfWriter.permissionPrint().set(Optional.ofNullable(permission.get("print")));
                    pdfWriter.permissionCopy().set(Optional.ofNullable(permission.get("copy")));
                    pdfWriter.permissionChange().set(Optional.ofNullable(permission.get("change")));
                    pdfWriter.permissionAddNotes().set(Optional.ofNullable(permission.get("addnotes")));
                }, () -> {
                    pdfWriter.permissionPrint().set("true");
                    pdfWriter.permissionCopy().set("true");
                    pdfWriter.permissionChange().set("true");
                    pdfWriter.permissionAddNotes().set("true");
                });
    }

    public Map<Integer, File> pdfToPPM(final File file, final File destDir, final int pageCount) throws IOException {
        if (enabledPdfToPpm == false) {
            throw new IOException("XPDF pdftoppm is disabled");
        }
        if (pageCount > maxPageCount) {
            throw new IllegalArgumentException(
                    "Too many page to process, max=" + maxPageCount + ", wanted=" + pageCount);
        }

        forceMkdir(destDir);
        final var baseNameExport = "export_" + System.currentTimeMillis();

        final var params = bulk("-f 1 -l <%pageCount%> -r <%resolution%> <%pdffile%> <%destDir%>")
                .directInjectVariable("pageCount", String.valueOf(pageCount))
                .directInjectVariable("resolution", String.valueOf(resolution))
                .directInjectVariable(PDFFILE, file.getAbsolutePath())
                .directInjectVariable("destDir", new File(destDir, baseNameExport).getAbsolutePath());
        final var commandLine = new CommandLine(PDFTOPPM, params, executableFinder);

        final var processlauncherBuilder = new ProcesslauncherBuilder(commandLine);
        final var capText = new CapturedStdOutErrTextRetention();
        processlauncherBuilder.getSetCaptureStandardOutputAsOutputText(BOTH_STDOUT_STDERR)
                .addObserver(capText);
        processlauncherBuilder.setExecutionTimeLimiter(executionTimeLimiter);
        processlauncherBuilder.setWorkingDirectory(tempDir);
        final var lifeCycle = processlauncherBuilder.start().waitForEnd();

        if (lifeCycle.isCorrectlyDone() == false) {
            log.error("Can't extract images from pdf={} {}", file, capText.getStdouterr(false, "; "));
            lifeCycle.checkExecution();
        }

        /**
         * Files are name based like "export_1234567890-000001.ppm"
         */
        final var skipLetters = baseNameExport.length() + 1;
        final var producedFiles = Stream.of(destDir.listFiles())
                .filter(f -> isExtension(f.getName().toLowerCase(), "ppm"))
                .filter(f -> f.getName().startsWith(baseNameExport))
                .collect(toUnmodifiableMap(
                        f -> {
                            final var baseName = getBaseName(f.getName()).substring(skipLetters);
                            return Integer.parseInt(baseName);
                        },
                        f -> f));

        if (producedFiles.size() != pageCount) {
            log.warn("Invalid created file count after pdf extraction, expect={}, have={}, on {} from {}",
                    pageCount, producedFiles.size(), destDir, file);
        }

        final var pagesNumSet = producedFiles.keySet();
        final var missingPages = IntStream.range(1, pageCount)
                .filter(page -> pagesNumSet.contains(page) == false)
                .mapToObj(page -> page)
                .toList();
        if (missingPages.isEmpty() == false) {
            log.warn("Missing extracted pages from {}: {}", file, missingPages);
        }

        return producedFiles;
    }

    /**
     * Lines separated by "\n", page by "0x0C" (Form Feed)
     * @param destFile can not be exists
     */
    public void pdfToText(final File sourceFile, final File destFile, final int pageCount) throws IOException {
        if (enabledPdfToText == false) {
            throw new IOException("XPDF pdftotext is disabled");
        }
        if (pageCount > maxPageCount) {
            throw new IllegalArgumentException(
                    "Too many page to process, max=" + maxPageCount + ", wanted=" + pageCount);
        }

        forceMkdirParent(destFile);
        if (destFile.exists()) {
            forceDelete(destFile);
        }

        /**
         * -clip option is not widespread
         */
        final var params = bulk("-f 1 -l <%pageCount%> -eol unix -enc UTF-8 <%pdffile%> <%destFile%>")
                .directInjectVariable("pageCount", String.valueOf(pageCount))
                .directInjectVariable(PDFFILE, sourceFile.getAbsolutePath())
                .directInjectVariable("destFile", destFile.getAbsolutePath());
        final var commandLine = new CommandLine(PDFTOTEXT, params, executableFinder);

        final var processlauncherBuilder = new ProcesslauncherBuilder(commandLine);
        final var capText = new CapturedStdOutErrTextRetention();
        processlauncherBuilder.getSetCaptureStandardOutputAsOutputText(BOTH_STDOUT_STDERR)
                .addObserver(capText);
        processlauncherBuilder.setExecutionTimeLimiter(executionTimeLimiter);
        processlauncherBuilder.setWorkingDirectory(tempDir);
        final var lifeCycle = processlauncherBuilder.start().waitForEnd();

        if (lifeCycle.isCorrectlyDone() == false) {
            log.error("Can't extract text from pdf={} {}", sourceFile, capText.getStdouterr(false, "; "));
            lifeCycle.checkExecution();
        }

        if (destFile.exists() == false) {
            log.error("Can't found output file created by pdftotext {} from {}", destFile, sourceFile);
        } else if (destFile.length() == 0) {
            log.info("Pdftotext has returned an empty file from {}", sourceFile);
            deleteQuietly(destFile);
        }
    }

    static Stream<Entry<String, String>> splitFirstColon(final String line) {
        final var colon = line.indexOf(":");
        if (colon < 1 || colon + 1 == line.length()) {
            log.warn("Can't parse line: \"{}\" (can't found colon)", line);
            return empty();
        }

        final var left = line.substring(0, colon).trim().toLowerCase();
        final var right = line.substring(colon + 1).trim();
        return Stream.of(Map.entry(left, right));
    }

}
