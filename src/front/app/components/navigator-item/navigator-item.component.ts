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
import { prettyPrintJson, FormatOptions } from 'pretty-print-json';
import sax, { SAXOptions, Tag } from 'sax';
import xmlFormat, { XMLFormatterOptions } from 'xml-formatter';
import he from 'he';
import { FileResponse } from '../../dto/file-response.interface';
import { AssetService } from '../../services/asset.service';
import { AssetResponseIndex } from '../../dto/asset-response-index.interface';
import { FirstUpperCasePipe } from '../../pipes/first-upper-case-pipe';
import { KeyValueMetadataResponse } from '../../dto/key-value-metadata-response.interface';
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

    readonly getJsonContentFromRenderedSelected = signal("");
    readonly getMessageFromRenderedSelected = signal("");
    readonly renderedDisplaySelected = signal<RenderedFileResponse|null>(null);

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
        "image-format", "ffprobe-base"
    ]);

    readonly displayOnlyRenderedPreviewType = new Set([
        "image-format", "ffprobe-base"
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

    // TODO display all layers of informations, like technical::type
    
    onClickSelectRenderedDisplay(e: Event, rendered: RenderedFileResponse) {
        e.preventDefault();
        this.renderedDisplaySelected.set(rendered);
        const content = this.getJsonContentFromRenderedSelected;
        const message = this.getMessageFromRenderedSelected;
        message.set("Loading...");
        content.set("");

        this.assetService.getAssetRenderedTextFile(this.fileHashPath(), rendered.name, 0)
            .then(data => {
                if (data == null) {
                    message.set(`No data from ${rendered.name}`);
                } else if (rendered.name.endsWith(".json")) {
                    const options: FormatOptions = {
                        indent: 2,
                        linkUrls: false,
                        trailingCommas: false
                    };
                    message.set("");
                    content.set(`<pre class="json-container">${prettyPrintJson.toHtml(JSON.parse(data), options)}</pre>`);
                } else if (rendered.name.endsWith(".xml")) {
                    const options:SAXOptions = {
                        lowercase: false,
                        normalize: false,
                        position: false,
                        xmlns: true,
                        trim: false,
                        noscript: false,
                    };
                    const xmlFormatOptions:XMLFormatterOptions = {
                        indentation: '\xa0\xa0',
                        lineSeparator: '\n',
                        throwOnFailure: true,
                        collapseContent: true
                    }

                    const parser = sax.parser(true, options);
                    const payload: string[] = [];

                    const makeTag = function(tagClass:string, rowTagContent:string) {
                        const lines = rowTagContent.split("\n");
                        const content = lines.map(line => he.encode(line)).join("<br />");
                        payload.push(`<span class="${tagClass}">${content}</span>`);
                    };

                    let isSelfClosing = false;
                    parser.onprocessinginstruction = function (node) {
                        makeTag("xml-header", "<?");
                        makeTag("xml-tag", `${node.name}`);
                        makeTag("xml-attr-value", ` ${node.body}`);
                        makeTag("xml-header", "?>");
                    }
                    parser.ondoctype = function (doctype: string) {
                        makeTag("xml-header", doctype);
                    }
                    parser.ontext = function (t: string) {
                        makeTag("xml-text", t);
                    }
                    parser.onopentag = function (tag:Tag) {
                        makeTag("xml-tag", `<${tag.name}`);
                        Object.entries(tag.attributes).forEach(entry => {
                            makeTag("xml-attr-key", ` ${entry[0]}`);
                            makeTag("", "=");
                            const tagValue:any = entry[1];
                            makeTag("xml-attr-value", `"${tagValue["value"]}"`);
                        });

                        if (tag.isSelfClosing) {
                            isSelfClosing = true;
                            makeTag("xml-tag", " />");
                        } else {
                            makeTag("xml-tag", ">");
                        }
                    }
                    parser.onclosetag = function (tagName) {
                        if (isSelfClosing) {
                            isSelfClosing = false;
                            return;
                        }
                        makeTag("xml-tag", `</${tagName}>`);
                    }
                    parser.oncomment = function (comment: string) {
                        makeTag("xml-comment", `<!-- $comment} -->`);
                    }
                    parser.onscript = function (script: string) {
                        makeTag("xml-script", `${'script'}`);
                    }
                    parser.onopencdata = function () {
                        makeTag("xml-cdata-tag", "<![CDATA[");
                    }
                    parser.oncdata = function (cdata: string) {
                        makeTag("xml-cdata-content", cdata);
                    }
                    parser.onclosecdata = function () {
                        makeTag("xml-cdata-tag", "]]>");
                    }

                    parser.write(xmlFormat(String(data), xmlFormatOptions)).close();
                    message.set("");
                    content.set(`<pre class="json-container xml-container">${payload.join("")}</pre>`);
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
