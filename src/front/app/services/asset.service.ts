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
import { Observable, Subject,  firstValueFrom, retry } from 'rxjs';

import { BackendAPIService } from './backend-api.service';
import { LocalStorageService } from './local-storage.service';
import { ResetActivitiesRequest } from '../dto/reset-activities-request.interface';
import { AssetResponse } from '../dto/asset-response.interface';
import { MtdThesaurusDefDublinCore } from './mtd-thesaurus-def-dublin-core.service';
import { MetadataThesaurusEntry } from '../dto/metadata-thesaurus-entry.interface';
import { HttpClient, HttpErrorResponse, HttpParams, HttpResponse } from '@angular/common/http';

@Injectable({
    providedIn: 'root',
})
export class AssetService {

    private readonly localStorageService = inject(LocalStorageService);
    private readonly backendAPIService = inject(BackendAPIService);
    private readonly mtdThesaurusDefDublinCore = inject(MtdThesaurusDefDublinCore);
    private readonly httpClient = inject(HttpClient);

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
        var mimeType = this.mtdThesaurusDefDublinCore.format();
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

    public async getAssetRenderedTextFile(hashPath: string, name: string, index: 0): Promise<string|null> {
        const url = this.makeAssetRenderedFileBaseURL(hashPath, name, index);
        return this.requestTextAsset(url);
    }

    /**
     * @param data only for POST and PUT
     */
    public async requestTextAsset(path: string): Promise<string|null> {
        let url: string = `${this.backendAPIService.BASE_URL}/${path}`;
        if (path.startsWith("/")) {
            url = `${this.backendAPIService.BASE_URL}${path}`;
        }

        const result$ = new Subject<string|null>;
        this.httpClient.get(url, {
            headers: {
                "accept": "*/*",
            },
            timeout: this.backendAPIService.TIMEOUT,
            observe: 'response',
            mode: 'same-origin',
            cache: 'force-cache',
            credentials: 'same-origin',
            redirect: 'follow',
            responseType: 'text'
        })
        .pipe(retry(0))
        .subscribe({
            next: (response: HttpResponse<string>) => {
                result$.next(response.body);
                result$.complete();
                result$.unsubscribe();
            },
            error: (httpError: HttpErrorResponse) => {
                console.error(httpError);
                result$.next(null);
                result$.complete();
                result$.unsubscribe();
            }
        });

        return await firstValueFrom(result$.asObservable());
    }

}
