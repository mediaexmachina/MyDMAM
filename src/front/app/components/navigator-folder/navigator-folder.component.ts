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
import { Component, signal, inject, effect, WritableSignal, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { RouterLink } from '@angular/router';

import { FileSystemService } from '../../services/file-system.service';
import { LocalStorageService } from '../../services/local-storage.service';

import { FileResponse } from '../../dto/file-response.interface';

@Component({
  selector: 'app-navigator-folder',
  imports: [RouterLink, DatePipe],
  templateUrl: './navigator-folder.component.html',
  styleUrl: './navigator-folder.component.css'
})
export class NavigatorFolderComponent {

    readonly route = inject(ActivatedRoute);
    readonly localStorageService = inject(LocalStorageService);
    readonly fileSystemService = inject(FileSystemService);
    readonly numberFormat = new Intl.NumberFormat('en-US');
    private readonly maxPageCount = 10;

    readonly storage:string;
    dirListResponse = signal<FileResponse|null>(null);
    listResultCount = signal(20);
    breadcrumbSplitPath = computed(this.splitPath.bind(this));
    pageNavigate = computed(this.computePageNavigate.bind(this));
    pageNavigatePreviousButton = computed(this.computePageNavigatePreviousButton.bind(this));
    pageNavigateNextButton = computed(this.computePageNavigateNextButton.bind(this));

    constructor() {
        this.storage = this.route.snapshot.params['storage'];

        const hashPath = this.route.snapshot.params['hashPath']
        if (hashPath != undefined) {
            this.list(hashPath);
        } else {
            this.fileSystemService.listRoot(this.storage, 0, this.listResultCount()).then(r => this.dirListResponse.set(r));
        }
    }

    list(hashPath:string, skip: number = 0):void {
        if (hashPath == "") {
            this.fileSystemService.listRoot(this.storage, skip, this.listResultCount()).then(r => this.dirListResponse.set(r));
        } else {
            this.fileSystemService.list(this.storage, hashPath, skip, this.listResultCount()).then(r => this.dirListResponse.set(r));
        }
    }

    private splitPath(): Array<Record<string, string>> {
        const dirListResponse = this.dirListResponse();
        if (dirListResponse == null
            || dirListResponse.path == null
            || dirListResponse.path == "/") {
            return [];
        }

        const splited = dirListResponse.path.split("/");
        if (splited.length < 2) {
            return [];
        }
        const result = new Array<Record<string, string>>();

        let fullPath = "";
        for (let index = 1; index < splited.length - 1; index++) {
            const name = splited[index];
            fullPath += "/" + name;

            result.push({
                "name": name,
                "hashPath": this.fileSystemService.hashPath(this.storage, fullPath),
            });
        }
        return result;
    }

    private makePageNavigateButton(pageNum:number, limit:number, skip: number): Record<string, number> {
        return {
            "page": pageNum + 1,
            "skip": pageNum * limit,
            "selected": ((pageNum * limit <= skip) && (skip < (pageNum + 1) * limit)) ? 1 : 0,
            "separator": 0,
        }
    }

    private computePageNavigatePreviousButton(): Record<string, number> {
        const dirListResponse = this.dirListResponse();
        if (dirListResponse == null) {
            return {"display": 0};
        }

        const skip = dirListResponse.skipCount;
        const limit = Math.max(this.listResultCount(), dirListResponse.listSize);
        const currentPage = Math.ceil(skip / limit);
        const isFirstPage = currentPage == 0;

        if (isFirstPage) {
            return {"display": 0};
        }

        return {
            "page": currentPage - 1,
            "skip": (currentPage - 1) * limit
        }
    }

    private computePageNavigateNextButton(): Record<string, number> {
        const dirListResponse = this.dirListResponse();
        if (dirListResponse == null) {
            return {"display": 0};
        }

        const skip = dirListResponse.skipCount;
        const limit = Math.max(this.listResultCount(), dirListResponse.listSize);
        const total = dirListResponse.total;
        const currentPage = Math.ceil(skip / limit);
        const isLastPage = total - skip < limit;

        if (isLastPage) {
            return {"display": 0};
        }

        return {
            "page": currentPage + 1,
            "skip": (currentPage + 1) * limit
        }
    }

    private computePageNavigate(): Array<Record<string, number>> {
        const dirListResponse = this.dirListResponse();
        if (dirListResponse == null) {
            return [];
        }
        const skip = dirListResponse.skipCount;
        const limit = Math.max(this.listResultCount(), dirListResponse.listSize);
        const total = dirListResponse.total;
        const currentPage = Math.ceil(skip / limit);
        const pageCount = Math.ceil(total / limit);
        const result = new Array<Record<string, number>>();

        if (pageCount < this.maxPageCount) {
            for (let pageNum = 0; pageNum < pageCount; pageNum++) {
                result.push(this.makePageNavigateButton(pageNum, limit, skip));
            }
        } else {
            const splitPageCount = this.maxPageCount / 2;
            for (let pageNum = 0; pageNum < splitPageCount; pageNum++) {
                result.push(this.makePageNavigateButton(pageNum, limit, skip));
            }

            if (currentPage > splitPageCount - 1
                && currentPage < (pageCount - splitPageCount)) {
                result.push({
                    "separator": 1,
                    "displayCurent": currentPage + 1
                });
            } else {
                result.push({
                    "separator": 1,
                    "displayCurent": 0
                });
            }

            for (let pageNum = pageCount - splitPageCount; pageNum < pageCount; pageNum++) {
                result.push(this.makePageNavigateButton(pageNum, limit, skip));
            }

        }

        return result;
    }

    onChangeDropdrownListResultCount(event: Event):void {
        const value = parseInt((event.target as HTMLSelectElement).value);
        this.listResultCount.set(value);

        const dirListResponse = this.dirListResponse();
        if (dirListResponse == null) {
            return;
        }
        if (dirListResponse.currentItem != null) {
            this.list(dirListResponse.currentItem.hashPath);
        } else {
            this.list("");
        }
    }

}
