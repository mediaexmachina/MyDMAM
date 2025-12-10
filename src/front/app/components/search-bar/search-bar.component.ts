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
import { ActivatedRoute, Router } from '@angular/router';

import { SearchService } from '../../services/search.service';

@Component({
  selector: 'app-search-bar',
  imports: [],
  templateUrl: './search-bar.component.html',
  styleUrl: './search-bar.component.css',
})
export class SearchBarComponent {

    readonly route = inject(ActivatedRoute);
    readonly router = inject(Router);
    readonly searchService = inject(SearchService);
    readonly q = signal('');

    constructor() {
        this.route.queryParamMap.subscribe(params => {
            this.q.set(params.get('q') || '');
        });
    }

    onSearchSubmit(event: Event): void {
        event.preventDefault();
        const inputElement = event.target as HTMLInputElement;
        inputElement.blur();
        const q = inputElement.value.trim()
        if (q == '') {
            this.router.navigate(['/']);
        } else {
            this.router.navigate(['/search'], { queryParams: { q: q } });
        }
    }
}
