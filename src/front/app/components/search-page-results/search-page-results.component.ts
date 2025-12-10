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

import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';

import { SearchService } from '../../services/search.service';
import { OpenSearchResponse } from '../../dto/open-search-response.interface';
import { FileSystemService } from '../../services/file-system.service';

@Component({
  selector: 'app-search-page-results',
  imports: [RouterLink, DatePipe],
  templateUrl: './search-page-results.component.html',
  styleUrl: './search-page-results.component.css',
})
export class SearchPageResultsComponent {

    readonly route = inject(ActivatedRoute);
    readonly searchService = inject(SearchService);
    readonly fileSystemService = inject(FileSystemService);
    readonly numberFormat = new Intl.NumberFormat('en-US');
    readonly pendingSearch = signal<boolean>(false);
    readonly searchResponse = signal<OpenSearchResponse|null>(null);

    constructor() {
        this.route.queryParamMap.subscribe(params => {
            this.search(params.get('q') || '');
        });
    }

    search(q:string): void {
        this.pendingSearch.set(true);
        this.searchService.openSearch(q, 0, true, null).then((response) => {
            this.searchResponse.set(response);
        }).finally(() => {
            this.pendingSearch.set(false);
        });
    }

}
