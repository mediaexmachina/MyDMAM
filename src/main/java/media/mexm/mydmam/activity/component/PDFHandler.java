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

import static java.lang.Math.min;
import static java.util.function.Predicate.not;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.BASE_PREVIEW;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FILE_INFORMATION;
import static media.mexm.mydmam.activity.ActivityLimitPolicy.FULL_PREVIEW;
import static media.mexm.mydmam.service.MediaAssetService.FULL_TEXT_PDF;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.forceDelete;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import media.mexm.mydmam.activity.ActivityEventType;
import media.mexm.mydmam.activity.ActivityHandler;
import media.mexm.mydmam.activity.ActivityLimitPolicy;
import media.mexm.mydmam.component.XPDF;
import media.mexm.mydmam.component.XPDF.PageInfo;
import media.mexm.mydmam.configuration.MyDMAMConfigurationProperties;
import media.mexm.mydmam.configuration.XPDFConf;
import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.mtdthesaurus.MetadataThesaurusRegister;
import media.mexm.mydmam.mtdthesaurus.MtdThesaurusDefPDF;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;
import media.mexm.mydmam.repository.FileMetadataDao;
import media.mexm.mydmam.service.MediaAssetService;
import media.mexm.mydmam.service.MediaRenderedFilesUtilsService;
import media.mexm.mydmam.service.MetadataThesaurusService;

@Slf4j
@Component
public class PDFHandler implements ActivityHandler {

    private static final Function<String, Boolean> NOT_NO = t -> "no".equalsIgnoreCase(t) == false;

    @Autowired
    MyDMAMConfigurationProperties configuration;
    @Autowired
    XPDF xpdf;
    @Autowired
    MediaAssetService mediaAssetService;
    @Autowired
    MediaRenderedFilesUtilsService mediaRenderedFilesUtilsService;
    @Autowired
    MetadataThesaurusService metadataThesaurusService;
    @Autowired
    FileMetadataDao fileMetadataDao;

    @Override
    public ActivityLimitPolicy getLimitPolicy() {
        return FILE_INFORMATION;
    }

    @Override
    public String getMetadataOriginName() {
        return "xpdf";
    }

    @Override
    public boolean isEnabled() {
        return xpdf.isEnabledPdfInfo();
    }

    @Override
    public boolean canHandle(final FileEntity fileEntity,
                             final ActivityEventType eventType,
                             final RealmStorageConfiguredEnv storedOn) {
        return storedOn.isDAS()
               && storedOn.haveWorkingDir()
               && metadataThesaurusService.getMimeType(fileEntity)
                       .map("application/pdf"::equals)
                       .orElse(false);
    }

    @Override
    public void handle(final FileEntity fileEntity,
                       final ActivityEventType eventType,
                       final RealmStorageConfiguredEnv storedOn) throws Exception {
        final var assetFile = storedOn.getLocalInternalFile(fileEntity);

        final var pdfInfo = xpdf.pdfInfo(assetFile);
        if (log.isTraceEnabled()) {
            log.trace("Get pdfinfo from {}: {}", assetFile, pdfInfo);
        }

        final var pageCount = xpdf.getInfo(pdfInfo, "Pages").map(Integer::parseInt).orElse(0);
        if (pageCount == 0) {
            log.error("Can't extract page count from PDF {}", assetFile);
            return;
        }
        final var maxPageCount = Optional.ofNullable(configuration.tools().xpdf())
                .map(XPDFConf::maxPageCount)
                .orElse(100_000);

        final var thesaurus = metadataThesaurusService.getThesaurus(this, fileEntity);

        final var pdfWriter = thesaurus.pdf();
        pdfWriter.pageCount().set(pageCount);
        pdfWriter.encrypted().set(xpdf.getInfo(pdfInfo, "Encrypted").filter(not("no"::equals)));
        pdfWriter.pdfVersion().set(xpdf.getInfo(pdfInfo, "PDF version"));
        pdfWriter.form().set(xpdf.getInfo(pdfInfo, "Form"));
        pdfWriter.optimized().set(xpdf.getInfo(pdfInfo, "Optimized").map(NOT_NO));
        pdfWriter.javascript().set(xpdf.getInfo(pdfInfo, "JavaScript").map(NOT_NO));
        pdfWriter.tagged().set(xpdf.getInfo(pdfInfo, "Tagged").map(NOT_NO));
        pdfWriter.producer().set(xpdf.getInfo(pdfInfo, "Producer"));
        pdfWriter.keywords().set(xpdf.getInfo(pdfInfo, "Keywords"));

        extractDCMtds(thesaurus, pdfInfo);
        extractXMPMtds(thesaurus, pdfInfo);
        xpdf.extractPermissions(pdfInfo, pdfWriter);

        final var pagesFormats = xpdf.extractPagesFormats(pdfInfo, maxPageCount);

        if (maxPageCount >= pageCount
            && pagesFormats.isEmpty() == false
            && pagesFormats.size() != pageCount) {
            log.warn("Missing some pages {}, expected {} in {}", pagesFormats.size(), pageCount, fileEntity);
        }

        final var first = pagesFormats.stream().findFirst().orElseThrow();
        final var samePages = pagesFormats.stream()
                .allMatch(pf -> Integer.compare(pf.rotated(), first.rotated()) == 0
                                && Float.compare(pf.w(), first.w()) == 0
                                && Float.compare(pf.h(), first.h()) == 0);
        pdfWriter.samePagesFormat().set(samePages);

        if (samePages) {
            makePageEntities(pdfWriter, first, 0);
        } else {
            pagesFormats.forEach(pageInfo -> makePageEntities(pdfWriter, pageInfo, -1));
        }

        if (storedOn.getActivityLimitPolicy()
                .isLevelLowerThan(pageCount == 1 ? BASE_PREVIEW : FULL_PREVIEW)) {
            log.trace("Cancel pdf extraction on {} {}: too low ActivityLimitPolicy={}",
                    storedOn.realmName(),
                    storedOn.storageName(),
                    storedOn.getActivityLimitPolicy());
            return;
        }

        if (storedOn.haveRenderedDir() == false || storedOn.haveWorkingDir() == false) {
            log.trace("Cancel pdf extraction on {} {}: haveRenderedDir={}, haveWorkingDir={}",
                    storedOn.realmName(),
                    storedOn.storageName(),
                    storedOn.haveRenderedDir(),
                    storedOn.haveWorkingDir());
            return;
        }

        if (xpdf.isEnabledPdfToPpm()) {
            extractPagesToImagesThumbnails(fileEntity, storedOn, assetFile, pageCount, maxPageCount);
        }

        if (xpdf.isEnabledPdfToText()) {
            extractPagesToFulltext(fileEntity, storedOn, assetFile, pageCount, maxPageCount);
        }
    }

    void extractPagesToImagesThumbnails(final FileEntity fileEntity,
                                        final RealmStorageConfiguredEnv storedOn,
                                        final File assetFile,
                                        final Integer pageCount,
                                        final Integer maxPageCount) throws IOException {
        final var pdfExportDir = new File(storedOn.realm().workingDirectory(), "pdftoppm-" + fileEntity.getId());
        if (pdfExportDir.exists()) {
            forceDelete(pdfExportDir);
        }
        final var exportedImagesByPages = xpdf.pdfToPPM(assetFile, pdfExportDir, min(maxPageCount, pageCount));

        if (exportedImagesByPages.isEmpty()) {
            log.info("Can't found images to thumbnails from {}", assetFile);
            return;
        }

        for (final var exportedImageEntry : exportedImagesByPages.entrySet()) {
            final var page = exportedImageEntry.getKey();
            final var imageFile = exportedImageEntry.getValue();
            final var index = page == 1 ? 0 : page;
            mediaRenderedFilesUtilsService.makeImageThumbnails(fileEntity, storedOn, imageFile, false, index);
            deleteQuietly(imageFile);
        }

        final var deleted = deleteQuietly(pdfExportDir);
        if (deleted == false) {
            log.warn("Can't delete working dir {}", pdfExportDir);
        }
    }

    void extractPagesToFulltext(final FileEntity fileEntity,
                                final RealmStorageConfiguredEnv storedOn,
                                final File assetFile,
                                final Integer pageCount,
                                final Integer maxPageCount) throws IOException {
        final var pdfText = new File(storedOn.realm().workingDirectory(), "pdftotxt-" + fileEntity.getId() + ".txt");

        xpdf.pdfToText(assetFile, pdfText, min(maxPageCount, pageCount));
        if (pdfText.exists() == false) {
            return;
        }
        mediaAssetService.declareTextExtractedFile(fileEntity, pdfText, FULL_TEXT_PDF);
    }

    void extractXMPMtds(final MetadataThesaurusRegister thesaurus, final Map<String, String> pdfInfo) {
        final var xmpWriter = thesaurus.xmp();
        xmpWriter.createDate().set(xpdf.getInfo(pdfInfo, "CreationDate")
                .map(Instant::parse)
                .map(Instant::getEpochSecond));
        xmpWriter.modifyDate().set(xpdf.getInfo(pdfInfo, "ModDate")
                .map(Instant::parse)
                .map(Instant::getEpochSecond));
        xmpWriter.creatorTool().set(xpdf.getInfo(pdfInfo, "Creator"));
        xmpWriter.metadataDate().set(xpdf.getInfo(pdfInfo, "MetadataDate"));
    }

    void extractDCMtds(final MetadataThesaurusRegister thesaurus, final Map<String, String> pdfInfo) {
        final var dcWriter = thesaurus.dublinCore();
        dcWriter.title().set(xpdf.getInfo(pdfInfo, "Title"));
        dcWriter.description().set(xpdf.getInfo(pdfInfo, "Subject"));
        dcWriter.creator().set(xpdf.getInfo(pdfInfo, "Author"));
    }

    void makePageEntities(final MtdThesaurusDefPDF defPdf,
                          final PageInfo pageInfo,
                          final int forcePage) {
        final var page = forcePage > -1 ? forcePage : pageInfo.page();
        defPdf.pageRotated().set(page, pageInfo.rotated());
        defPdf.pageWidthMm().set(page, pageInfo.getWMm());
        defPdf.pageHeightMm().set(page, pageInfo.getHMm());
    }

}
