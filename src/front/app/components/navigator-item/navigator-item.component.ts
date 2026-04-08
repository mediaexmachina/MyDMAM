/*
 * This file is part of MyDMAM.
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
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FileResponse } from '../../dto/file-response.interface';
import { AssetService } from '../../services/asset.service';
import { AssetResponseIndex } from '../../dto/asset-response-index.interface';
import { FirstUpperCasePipe } from '../../pipes/first-upper-case-pipe';
import { KeyValueMetadataResponse } from '../../dto/key-value-metadata-response.interface';
import { prettyPrintJson, FormatOptions } from 'pretty-print-json';
import { RenderedFileResponse } from '../../dto/rendered-file-response.interface';
import { MtdThesaurusDefPDF } from '../../services/mtd-thesaurus-def-pdf.service';
import { PaginationComponent } from '../toolkit/pagination.component';

@Component({
    selector: 'app-navigator-item',
    imports: [FirstUpperCasePipe, PaginationComponent],
    templateUrl: './navigator-item.component.html',
    styleUrl: './navigator-item.component.css',
})
export class NavigatorItemComponent {

    readonly assetService = inject(AssetService);
    readonly mtdThesaurusDefPDF = inject(MtdThesaurusDefPDF);
    readonly fileResponse = input.required<FileResponse>();
    readonly selectedPage = signal(0);

    skipCountPageNavigation = 0;

    constructor() {
        effect(() => {
            if (this.selectedPage() == 0 && this.pageCount() > 1) {
                this.selectedPage.set(1);
            }
        });
    }

    ngOnChanges() {
        this.skipCountPageNavigation = 0;
        this.selectedPage.set(0);
        this.getJsonContentFromRenderedSelected.set("");
        this.getMessageFromRenderedSelected.set("");
        this.renderedDisplaySelected.set(null);
    }

    readonly downloadOnlyRenderedPreviewType = new Set([
        "image-format"
    ]);

    readonly displayOnlyRenderedPreviewType = new Set([
        "image-format"
    ]);

    readonly fileHashPath = computed(() => {
        const fileResponse = this.fileResponse();
        if (fileResponse.currentItem == null) {
            return "";
        }
        return fileResponse.currentItem.hashPath;
    });

    readonly itemMedatadas = computed(() => {
        const fileResponse = this.fileResponse();
        const fileHashPath = this.fileHashPath();
        if (fileHashPath in fileResponse.metadatas == false) {
            return null;
        }
        return fileResponse.metadatas[fileHashPath];
    });

    readonly defaultIndexMetadatas = computed(() => {
        const itemMedatadas = this.itemMedatadas();
        if (itemMedatadas == null || 0 in itemMedatadas.index == false) {
            return null;
        }
        return itemMedatadas.index[0];
    });

    readonly renderedHeroImagePreview = computed(() => {
        const renderedList = this.defaultIndexMetadatas()?.rendered || [];
        const heroList = renderedList.filter(r => r.previewType == "hero-thumbnail");
        if (heroList.length > 0) {
            const page = this.selectedPage();
            let index = page;
            if (page < 2) {
                index = 0;
            }
            return this.assetService.makeAssetRenderedFileURL(this.fileHashPath(), heroList[0].name, index);
        }
        return null;
    });

    readonly pageCount = computed(() => {
        const itemMedatadas = this.itemMedatadas();
        if (itemMedatadas == null || 0 in itemMedatadas.index == false) {
            return 1;
        }
        return parseInt(this.assetService.getFileMetadataResponseValue(itemMedatadas, this.mtdThesaurusDefPDF.pageCount(), "1"));
    });

    readonly renderedDownloadList = computed(() => {
        const renderedList = this.defaultIndexMetadatas()?.rendered || [];
        return renderedList.filter(r => this.downloadOnlyRenderedPreviewType.has(r.previewType));
    });

    readonly renderedDisplayList = computed(() => {
        const renderedList = this.defaultIndexMetadatas()?.rendered || [];
        return renderedList.filter(r => this.displayOnlyRenderedPreviewType.has(r.previewType));
    });

    readonly getJsonContentFromRenderedSelected = signal("");
    readonly getMessageFromRenderedSelected = signal("");
    readonly renderedDisplaySelected = signal<RenderedFileResponse|null>(null);

    onClickPagination(pageNavigateButton:any):void {
        this.selectedPage.set((pageNavigateButton["skip"] || 0) + 1);
        this.skipCountPageNavigation = pageNavigateButton["skip"] || 0;
    }

    paginationButtonContent(pageNavigateButton:any):any {
        const fileHashPath = this.fileHashPath();
        if (fileHashPath == "") {
            return {};
        }
        const itemMedatadas = this.itemMedatadas();
        if (itemMedatadas == null || 0 in itemMedatadas.index == false) {
            return {};
        }

        const page = pageNavigateButton["page"];
        let index = page;
        if (page == 1) {
            index = 0;
        }
        if (index in itemMedatadas.index) {
            const renderedList = itemMedatadas.index[index].rendered || [];
            const iconList = renderedList.filter(r => r.previewType == "icon-thumbnail");
            if (iconList.length > 0) {
                return {
                    label: "Page " + page,
                    imgURL: this.assetService.makeAssetRenderedFileURL(fileHashPath, iconList[0].name, index)
                };
            }
        }

        return {
            label: pageNavigateButton["page"],
        };
    }

    getClassifiers(assetResponseIndex: AssetResponseIndex): Array<string> {
        const allClassifiers = assetResponseIndex.fileMetadatas.map(f => f.classifier);
        const classifiersNames = new Set(allClassifiers);
        classifiersNames.delete("file-format");
        return [...classifiersNames].sort();
    }

    getKeyValueByClassifierName(classifierName:string, assetResponseIndex: AssetResponseIndex): Array<KeyValueMetadataResponse> {
        return assetResponseIndex.fileMetadatas.filter(f => f.classifier == classifierName);
    }

    onClickSelectRenderedDisplay(e: Event, rendered: RenderedFileResponse) {
        e.preventDefault();
        this.renderedDisplaySelected.set(rendered);
        const content = this.getJsonContentFromRenderedSelected;
        const message = this.getMessageFromRenderedSelected;
        message.set("Loading...");
        content.set("");

        this.assetService.getAssetRenderedFileString(this.fileHashPath(), rendered.name, 0)
            .then(text => {
                if (text == null) {
                    message.set(`No data from ${rendered.name}`);
                } else {
                    const options: FormatOptions = {
                        indent: 2,
                        linkUrls: false,
                        trailingCommas: false
                    };
                    message.set("");
                    content.set(prettyPrintJson.toHtml(text, options));
                }
            });
    }

    onClickGoToPage(e: Event, page:number) {
        e.preventDefault();
        if (page > 0 && page <= this.pageCount()) {
            this.selectedPage.set(page);
            this.skipCountPageNavigation = page - 1;
        }
    }

}
