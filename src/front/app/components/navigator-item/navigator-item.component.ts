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
import { Component, computed, inject, input } from '@angular/core';
import { FileResponse } from '../../dto/file-response.interface';
import { AssetService } from '../../services/asset.service';
import { AssetResponseIndex } from '../../dto/asset-response-index.interface';
import { FirstUpperCasePipe } from '../../pipes/first-upper-case-pipe';
import { KeyValueMetadataResponse } from '../../dto/key-value-metadata-response.interface';

@Component({
    selector: 'app-navigator-item',
    imports: [FirstUpperCasePipe],
    templateUrl: './navigator-item.component.html',
    styleUrl: './navigator-item.component.css',
})
export class NavigatorItemComponent {

    readonly assetService = inject(AssetService);
    readonly Object = Object;
    readonly Math = Math;

    readonly fileResponse = input.required<FileResponse>();
    readonly itemMedatadas = computed(() => {
        const fileResponse = this.fileResponse();
        if (fileResponse.currentItem == null) {
            return null;
        }
        const hashPath = fileResponse.currentItem.hashPath;
        if (hashPath in fileResponse.metadatas == false) {
            return null;
        }

        return fileResponse.metadatas[hashPath];
    });

    readonly defaultIndexMetadatas = computed(() => {
        const itemMedatadas = this.itemMedatadas();
        if (itemMedatadas == null || 0 in itemMedatadas.index == false) {
            return null;
        }
        return itemMedatadas.index[0];
    });

    getClassifiers(assetResponseIndex: AssetResponseIndex): Array<string> {
        const allClassifiers = assetResponseIndex.fileMetadatas.map(f => f.classifier);
        const classifiersNames = new Set(allClassifiers);
        classifiersNames.delete("file-format");
        return [...classifiersNames].sort();
    }

    getKeyValueByClassifierName(classifierName:string, assetResponseIndex: AssetResponseIndex): Array<KeyValueMetadataResponse> {
        return assetResponseIndex.fileMetadatas.filter(f => f.classifier == classifierName);
    }
    
}
