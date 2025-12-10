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
 * Copyright (C) Media ex Machina 2025
 *
 */

import { Injectable, inject } from '@angular/core';
import * as forge from 'node-forge';

import { BackendAPIService } from './backend-api.service';
import { FileResponse } from '../dto/file-response.interface';
import { RealmListResponse } from '../dto/realm-list-response.interface';
import { StorageListResponse } from '../dto/storage-list-response.interface';
import { LocalStorageService } from './local-storage.service';

@Injectable({
  providedIn: 'root'
})
export class FileSystemService {

    private readonly localStorageService = inject(LocalStorageService);
    private readonly backendAPIService = inject(BackendAPIService);

    public async getRealms(): Promise<RealmListResponse|null> {
        return this.backendAPIService.requestAsyncAPI<RealmListResponse>(
            "GET", "/filesystem/list");
    }

    public async getStorages(): Promise<StorageListResponse|null> {
        const realm = this.localStorageService.getSelectedRealm();
        return this.backendAPIService.requestAsyncAPI<StorageListResponse>(
            "GET", `/filesystem/list/${realm}`);
    }

    public async listRoot(storage:string, skip:number = 0, limit:number = 0): Promise<FileResponse|null> {
        const realm = this.localStorageService.getSelectedRealm();
        const params = {
            skip: skip,
            limit: limit
        }
        return this.backendAPIService.requestAsyncAPI<FileResponse>(
            "GET", `/filesystem/list/${realm}/${storage}`, params);
    }

    public async list(storage:string, hashPath:string, skip:number = 0, limit:number = 0): Promise<FileResponse|null> {
        const realm = this.localStorageService.getSelectedRealm();
        const params = {
            skip: skip,
            limit: limit
        }
        return this.backendAPIService.requestAsyncAPI<FileResponse>(
            "GET", `/filesystem/list/${realm}/${storage}/${hashPath}`, params);
    }

    public hashPath(storage:string, path:string) {
        const realm = this.localStorageService.getSelectedRealm();
        var md = forge.md.sha256.create();
        md.update(`${realm}:${storage}:${path}`);
        return md.digest().toHex();
    }

}
