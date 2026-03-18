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
import { Injectable, inject } from '@angular/core';

import { BackendAPIService } from './backend-api.service';
import { LocalStorageService } from './local-storage.service';
import { ResetActivitiesRequest } from '../dto/reset-activities-request.interface';
import { AssetResponse } from '../dto/asset-response.interface';

@Injectable({
    providedIn: 'root',
})
export class AssetService {

    private readonly localStorageService = inject(LocalStorageService);
    private readonly backendAPIService = inject(BackendAPIService);

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
            classifier:string,
            key:string,
            defaultValue:string,
            index:number = 0): string {

        if (index in assetResponse.index == false) {
            return defaultValue;
        }
        
        return assetResponse.index[index]
            .fileMetadatas
            .filter(m => m.classifier === classifier)
            .filter(m => m.key === key)
            .map(m => m.value)
            .at(0) || defaultValue;
    }

    public getFileMetadataMimeType(assetResponse: AssetResponse): string {
        return this.getFileMetadataResponseValue(assetResponse, "file-format", "mime-type", "application/octet-stream");
    }

    private makeAssetRenderedFileURL(hashPath: string, name: string, index: number): string {
        const BASE_URL = this.backendAPIService.BASE_URL
        const realm = this.localStorageService.getSelectedRealm();
        return `${BASE_URL}/content/rendered/${realm}/${hashPath}/${name}?index=${index}`;
    }

    public makeAssetRenderedFileDownloadURL(hashPath: string, name: string, index: 0): string {
        const startURL = this.makeAssetRenderedFileURL(hashPath, name, index);
        return `${startURL}&download=1`;
    }
    
}
