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

}
