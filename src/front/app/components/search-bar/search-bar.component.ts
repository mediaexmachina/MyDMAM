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
import { Component, computed, DestroyRef, effect, ElementRef, inject, Renderer2, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { SearchService } from '../../services/search.service';
import { OpenSearchResponse } from '../../dto/open-search-response.interface';

@Component({
    selector: 'app-search-bar',
    imports: [RouterLink],
    templateUrl: './search-bar.component.html',
    styleUrl: './search-bar.component.css',
})
export class SearchBarComponent {

    readonly route = inject(ActivatedRoute);
    readonly router = inject(Router);
    readonly renderer = inject(Renderer2);
    readonly destroyRef = inject(DestroyRef);
    readonly searchService = inject(SearchService);

    readonly q = signal('');
    readonly searchResponse = signal<OpenSearchResponse | null>(null);
    readonly suggestionCount = computed(() => {
        return this.searchResponse()?.result.foundedFiles.length || 0;
    });
    readonly suggestionMenuOpened = computed(() => {
        return this.suggestionCount() > 0;
    });
    readonly selectedSuggestionMenuEntry = signal<number>(-1);
    readonly selectedSuggestionMenuItemHashPath = computed(() => {
        const foundedFiles = this.searchResponse()?.result.foundedFiles || [];
        const selectedSuggestionMenuEntry = this.selectedSuggestionMenuEntry();
        if (selectedSuggestionMenuEntry < 0 || foundedFiles.length == 0) {
            return "";
        }
        return foundedFiles[selectedSuggestionMenuEntry].hashPath;
    });
    readonly topsearchinput = viewChild<ElementRef<HTMLDivElement>>('topsearchinput');

    constructor() {
        this.route.queryParamMap.subscribe(params => {
            this.q.set(params.get('q') || '');
        });

        const unlistenDocumentKeydown = this.renderer.listen("document", "keydown", (e) => {
            if (this.suggestionMenuOpened() == false) {
                return;
            }
            if (e.key == "Escape") {
                e.preventDefault();
                this.searchResponse.set(null);
            } else if (e.key == "ArrowUp") {
                e.preventDefault();
                const selectedSuggestionMenuEntry = this.selectedSuggestionMenuEntry();
                if (selectedSuggestionMenuEntry < 1) {
                    this.topsearchinput()?.nativeElement.focus();
                    this.selectedSuggestionMenuEntry.set(-1);
                } else {
                    this.selectedSuggestionMenuEntry.set(selectedSuggestionMenuEntry - 1);
                }
            } else if (e.key == "ArrowDown") {
                e.preventDefault();
                this.selectedSuggestionMenuEntry.set(Math.min(this.selectedSuggestionMenuEntry() + 1, this.suggestionCount()));
            } else if (e.key == "Enter") {
                e.preventDefault();
                const foundedFiles = this.searchResponse()?.result.foundedFiles || [];
                const selectedSuggestionMenuEntry = this.selectedSuggestionMenuEntry();
                if (selectedSuggestionMenuEntry > -1 && selectedSuggestionMenuEntry < foundedFiles.length) {
                    const item = foundedFiles[selectedSuggestionMenuEntry];
                    this.searchResponse.set(null);
                    this.router.navigate(['/navigator', item.storage, item.hashPath], { queryParams: { q: this.q() } });
                }
            }
        });

        const unlistenDocumentClick = this.renderer.listen("document", "click", (e) => {
            if (e.target == this.topsearchinput()?.nativeElement) {
                this.selectedSuggestionMenuEntry.set(-1);
                this.suggestion();
            } else if (this.searchResponse() != null) {
                this.searchResponse.set(null);
            }
        });

        this.destroyRef.onDestroy(() => {
            unlistenDocumentKeydown();
            unlistenDocumentClick();
        });

        effect(() => {
            if (this.suggestionMenuOpened()) {
                this.selectedSuggestionMenuEntry.set(-1);
            }
        });
        effect(() => {
            const selectedSuggestionMenuEntry = this.selectedSuggestionMenuEntry();
            if (selectedSuggestionMenuEntry > -1) {
                this.topsearchinput()?.nativeElement.blur();
            }
        });
    }

    onSearchSubmit(event: Event): void {
        event.preventDefault();
        this.searchResponse.set(null);
        const inputElement = event.target as HTMLInputElement;
        inputElement.blur();
        const q = inputElement.value.trim()
        if (q == '') {
            this.router.navigate(['/']);
        } else {
            this.router.navigate(['/search'], { queryParams: { q: q } });
        }
    }

    suggestion(): void {
        const q = (this.topsearchinput()?.nativeElement as HTMLInputElement).value.trim();
        if (q.length < 3) {
            return;
        }

        this.searchService.openSearch(q, 10, false, null).then((response) => {
            this.searchResponse.set(response);
        });
    }
}
