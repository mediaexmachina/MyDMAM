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

import { SearchService } from '../../services/search.service';
import { OpenSearchResponse } from '../../dto/open-search-response.interface';
import { SearchConstraintsRequest } from '../../dto/search-constraints-request.interface';
import { FileSearchConstraints } from '../../dto/file-search-constraints.interface';
import { SearchConstraintCondition } from '../../dto/search-constraint-condition.enum';
import { SearchConstraintRange } from '../../dto/search-constraint-range.interface';
import { FileSystemService } from '../../services/file-system.service';
import { SearchConstraintConditionEditorComponent } from "./search-constraint-condition-editor.component";
import { SearchConstraintDateRangeEditorComponent } from "./search-constraint-date-range-editor.component";
import { SpanDateTimeComponent } from "../toolkit/span-date-time.component";
import { SearchConstraintsSizeRangeEditorComponent } from "./search-constraints-size-range-editor.component";
import { SpanFileSizeComponent } from "../toolkit/span-file-size.component";
import { SearchConstraintsStorageEditorComponent } from "./search-constraints-storage-editor.component";
import { SearchConstraintsParentPathEditorComponent } from "./search-constraints-parent-path-editor.component";

@Component({
    selector: 'app-search-page-results',
    imports: [
        RouterLink,
        SearchConstraintConditionEditorComponent,
        SearchConstraintDateRangeEditorComponent,
        SpanDateTimeComponent,
        SearchConstraintsSizeRangeEditorComponent,
        SpanFileSizeComponent,
        SearchConstraintsStorageEditorComponent,
        SearchConstraintsParentPathEditorComponent
    ],
    templateUrl: './search-page-results.component.html',
    styleUrl: './search-page-results.component.css',
})
export class SearchPageResultsComponent {

    readonly route = inject(ActivatedRoute);
    readonly searchService = inject(SearchService);
    readonly fileSystemService = inject(FileSystemService);
    readonly pendingSearch = signal<boolean>(false);
    readonly searchResponse = signal<OpenSearchResponse | null>(null);
    readonly searchConstraints = signal<SearchConstraintsRequest | null>(null);
    readonly storageList = signal<Array<string>>([]);

    constructor() {
        this.route.queryParamMap.subscribe(params => {
            this.search(params.get('q') || '');
        });
        this.fileSystemService.getStorages().then(s => {
            this.storageList.set(s?.storages || []);
        });
    }

    search(q: string): void {
        this.pendingSearch.set(true);
        const searchConstraints = this.searchConstraints();

        this.searchService.openSearch(q, 0, true, searchConstraints).then((response) => {
            this.searchResponse.set(response);
        }).finally(() => {
            this.pendingSearch.set(false);
        });
    }

    private searchAgain(): void {
        const searchResponse = this.searchResponse();
        if (searchResponse != null) {
            this.search(searchResponse.q);
        }
    }

    createSearchConstraints(event: Event): void {
        event.preventDefault();

        var fileConstraints: FileSearchConstraints = {
            directory: SearchConstraintCondition.IGNORE,
            hidden: SearchConstraintCondition.IGNORE,
            link: SearchConstraintCondition.IGNORE,
            special: SearchConstraintCondition.IGNORE,
            date: { restricted: false, min: 0, max: 0 },
            size: { restricted: false, min: 0, max: 0 },
            storages: [],
            parentPath: "",
            parentHashPath: "",
        }

        this.searchConstraints.set({
            fileConstraints: fileConstraints
        });

        this.searchAgain();
    }

    removeSearchConstraints(event: Event): void {
        event.preventDefault();
        this.searchConstraints.set(null);
        this.searchAgain();
    }

    onChangeSearchConstraintsRange(range: SearchConstraintRange,
        applyOn: "date" | "size"): void {
        this.searchConstraints.update(current => {
            if (current == null) {
                return null;
            }
            switch (applyOn) {
                case "date":
                    current.fileConstraints.date = range;
                    break;
                case "size":
                    current.fileConstraints.size = range;
                    if (range.restricted) {
                        current.fileConstraints.directory = SearchConstraintCondition.MUST_NOT;
                    }
                    break;
                default:
                    break;
            }
            return current;
        });
        this.searchAgain();
    }

    onChangeSearchConstraintsCondition(condition: SearchConstraintCondition,
        applyOn: "directory" | "hidden" | "link" | "special"): void {
        this.searchConstraints.update(current => {
            if (current == null) {
                return null;
            }
            switch (applyOn) {
                case "directory":
                    current.fileConstraints.directory = condition;
                    if (condition != SearchConstraintCondition.MUST_NOT) {
                        current.fileConstraints.size.restricted = false;
                    }
                    break;
                case "hidden":
                    current.fileConstraints.hidden = condition;
                    break;
                case "link":
                    current.fileConstraints.link = condition;
                    break;
                case "special":
                    current.fileConstraints.special = condition;
                    if (condition != SearchConstraintCondition.MUST_NOT) {
                        current.fileConstraints.size.restricted = false;
                    }
                    break;
                default:
                    break;
            }
            return current;
        });
        this.searchAgain();
    }

    onChangeSearchConstraintsStorages(storages: Array<string>): void {
        this.searchConstraints.update(current => {
            if (current == null) {
                return null;
            }
            current.fileConstraints.storages = storages;
            return current;
        });
        this.searchAgain();
    }

    onChangeSearchConstraintsParentPath(parentPath: string) {
        this.searchConstraints.update(current => {
            if (current == null) {
                return null;
            }
            current.fileConstraints.parentPath = parentPath;
            return current;
        });
        this.searchAgain();
    }
}
