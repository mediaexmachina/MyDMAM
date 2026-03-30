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
import { Injectable, Signal, inject, signal } from '@angular/core';

import { BackendAPIService } from './backend-api.service';
import { LocalStorageService } from './local-storage.service';
import { ResetActivitiesRequest } from '../dto/reset-activities-request.interface';
import { AssetResponse } from '../dto/asset-response.interface';
import { MtdThesaurusDefTechnical } from './mtd-thesaurus-def-technical.service';
import { MtdThesaurusDefFileFormat } from './mtd-thesaurus-def-file-format.service';
import { MetadataThesaurusEntry } from '../dto/metadata-thesaurus-entry.interface';

@Injectable({
    providedIn: 'root',
})
export class AssetService {

    private readonly localStorageService = inject(LocalStorageService);
    private readonly backendAPIService = inject(BackendAPIService);
    private readonly mtdThesaurusDefFileFormat = inject(MtdThesaurusDefFileFormat);

    public async resetActivities(hashPaths: Array<string>, recursive: boolean): Promise<null> {
        const request: ResetActivitiesRequest = {
            hashPaths: hashPaths,
            recursive: recursive
        };
        const realm = this.localStorageService.getSelectedRealm();
        return this.backendAPIService.requestAsyncAPI<null>(
            "POST", `/asset/reset-activities/${realm}`, {}, request);
    }

    public getFileMetadataResponseValue(
            assetResponse: AssetResponse,
            entry: MetadataThesaurusEntry,
            defaultValue:string,
            index:number = 0): string {

        if (index in assetResponse.index == false) {
            return defaultValue;
        }
        
        return assetResponse.index[index]
            .fileMetadatas
            .filter(m => m.classifier === entry.classifier)
            .filter(m => m.key === entry.key)
            .map(m => m.value)
            .at(0) || defaultValue;
    }

    public getFileMetadataMimeType(assetResponse: AssetResponse): string {
        var mimeType = this.mtdThesaurusDefFileFormat.mimeType();
        return this.getFileMetadataResponseValue(assetResponse, mimeType, "application/octet-stream");
    }

    private makeAssetRenderedFileBaseURL(hashPath: string, name: string, index: number): string {
        const realm = this.localStorageService.getSelectedRealm();
        return `/content/rendered/${realm}/${hashPath}/${name}?index=${index}`;
    }

    public makeAssetRenderedFileURL(hashPath: string, name: string, index: number): string {
        const BASE_URL = this.backendAPIService.BASE_URL;
        return `${BASE_URL}${this.makeAssetRenderedFileBaseURL(hashPath, name, index)}`;
    }
    
    public makeAssetRenderedFileDownloadURL(hashPath: string, name: string, index: 0): string {
        return `${this.makeAssetRenderedFileURL(hashPath, name, index)}&download=1`;
    }

    public async getAssetRenderedFileString(hashPath: string, name: string, index: 0): Promise<string|null> {
        const url = this.makeAssetRenderedFileBaseURL(hashPath, name, index);
        return this.backendAPIService.requestAsyncAPI<string>("GET", url);
    }

}
