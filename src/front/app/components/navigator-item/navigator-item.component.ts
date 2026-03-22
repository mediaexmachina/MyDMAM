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
import { Component, computed, inject, input, Signal, signal } from '@angular/core';
import { FileResponse } from '../../dto/file-response.interface';
import { AssetService } from '../../services/asset.service';
import { AssetResponseIndex } from '../../dto/asset-response-index.interface';
import { FirstUpperCasePipe } from '../../pipes/first-upper-case-pipe';
import { KeyValueMetadataResponse } from '../../dto/key-value-metadata-response.interface';
import { prettyPrintJson, FormatOptions } from 'pretty-print-json';
import { RenderedFileResponse } from '../../dto/rendered-file-response.interface';

@Component({
    selector: 'app-navigator-item',
    imports: [FirstUpperCasePipe],
    templateUrl: './navigator-item.component.html',
    styleUrl: './navigator-item.component.css',
})
export class NavigatorItemComponent {

    readonly downloadOnlyRenderedPreviewType = new Set([
        "image-format"
    ]);

    readonly displayOnlyRenderedPreviewType = new Set([
        "image-format"
    ]);

    readonly assetService = inject(AssetService);
    readonly fileResponse = input.required<FileResponse>();

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
            return this.assetService.makeAssetRenderedFileURL(this.fileHashPath(), heroList[0].name, 0);
        }
        return null;
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

}
