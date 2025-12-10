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

import { LocalStorageService } from './local-storage.service';
import { BackendAPIService } from './backend-api.service';
import { OpenSearchResponse } from '../dto/open-search-response.interface';
import { SearchConstraintsRequest } from '../dto/search-constraints-request.interface';

@Injectable({
  providedIn: 'root',
})
export class SearchService {

    private readonly localStorageService = inject(LocalStorageService);
    private readonly backendAPIService = inject(BackendAPIService);

    public async openSearch(q:string,
                            limit:number = 0,
                            resolveHashPaths:boolean = false,
                            constraints: SearchConstraintsRequest|null = null): Promise<OpenSearchResponse|null> {
        const realm = this.localStorageService.getSelectedRealm();
        const params = {
            q: q,
            limit: limit,
            resolveHashPaths: resolveHashPaths ? "1" : "0"
        }

        return this.backendAPIService.requestAsyncAPI<OpenSearchResponse>(
            constraints == null ? "GET": "PUT",
            `/search/${realm}`,
            params,
            constraints);
    }

}
