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
import { Component, signal, inject, computed, WritableSignal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { FileSystemService } from '../../services/file-system.service';
import { LocalStorageService } from '../../services/local-storage.service';

import { FileResponse } from '../../dto/file-response.interface';
import { SpanDateTimeComponent } from "../toolkit/span-date-time.component";
import { SpanFileSizeComponent } from "../toolkit/span-file-size.component";
import { SortOrder } from '../../dto/sort-order.enum';
import { NavigatorColumnSortComponent } from "./navigator-column-sort.component";
import { AssetService } from '../../services/asset.service';
import { NavigatorItemComponent } from "../navigator-item/navigator-item.component";
import { DirListStyle } from '../../enums/dir-list-style.enum';
import { RemoveFileNameExtensionPipe } from '../../pipes/remove-file-name-extension-pipe';
import { GetFileNameExtensionPipe } from '../../pipes/get-file-name-extension-pipe';
import { PaginationComponent } from '../toolkit/pagination.component';

@Component({
  selector: 'app-navigator-folder',
  imports: [
    RouterLink,
    SpanDateTimeComponent,
    SpanFileSizeComponent,
    PaginationComponent,
    NavigatorColumnSortComponent,
    NavigatorItemComponent,
    RemoveFileNameExtensionPipe, 
    GetFileNameExtensionPipe
    ],
  templateUrl: './navigator-folder.component.html',
  styleUrl: './navigator-folder.component.css'
})
export class NavigatorFolderComponent {

    readonly _SortOrder = SortOrder;
    readonly _DirListStyle = DirListStyle;
    readonly route = inject(ActivatedRoute);
    readonly localStorageService = inject(LocalStorageService);
    readonly fileSystemService = inject(FileSystemService);
    readonly assetService = inject(AssetService);
    private readonly maxPageCount = 10;
    readonly defaultListResultCount = 20;

    readonly storage = signal<string>("");
    readonly dirListResponse = signal<FileResponse|null>(null);
    readonly listResultCount:WritableSignal<number>;
    readonly dirListStyle = this.localStorageService.dirListStyle;

    private currentSortOrder = {
        name: SortOrder.none,
        type: SortOrder.none,
        size: SortOrder.none,
        date: SortOrder.none
    };

    constructor() {
        this.listResultCount = signal(this.localStorageService.getNavigateListResultCount(this.defaultListResultCount));

        this.route.paramMap.subscribe(params => {
            const storage = params.get('storage') || "";
            this.storage.set(storage);

            const hashPath = params.get('hashPath') || "";
            this.list(hashPath);
        });
    }
    
    list(hashPath:string, skip: number = 0):void {
        if (hashPath == "") {
            this.fileSystemService.listRoot(
                this.storage(),
                skip,
                this.listResultCount(),
                true,
                true,
                this.currentSortOrder["name"],
                this.currentSortOrder["type"],
                this.currentSortOrder["date"],
                this.currentSortOrder["size"])
                .then(r => this.dirListResponse.set(r));
        } else {
            this.fileSystemService.list(
                this.storage(),
                hashPath,
                skip,
                this.listResultCount(),
                true,
                true,
                this.currentSortOrder["name"],
                this.currentSortOrder["type"],
                this.currentSortOrder["date"],
                this.currentSortOrder["size"])
                .then(r => this.dirListResponse.set(r));
        }
    }

    listRefresh() {
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

    onChangeDropdrownListResultCount(event: Event):void {
        const value = parseInt((event.target as HTMLSelectElement).value);
        this.listResultCount.set(value);
        this.localStorageService.setNavigateListResultCount(value);
        this.listRefresh();
    }

    changeSort(order:SortOrder, colName:"name"|"type"|"size"|"date"):void {
        this.currentSortOrder[colName] = order;
        this.listRefresh();
    }

    resetActivitiesOnClick(e: Event, recursive:boolean) {
        e.preventDefault();
        const currentItem = this.dirListResponse()?.currentItem;
        const parentHashPath = this.dirListResponse()?.parentHashPath;
        if (currentItem != null) {
            this.assetService.resetActivities([currentItem.hashPath], recursive);
        } else if (this.dirListResponse()?.path == "/" && parentHashPath != null) {
            this.assetService.resetActivities([parentHashPath], recursive);
        }
    }

    getFileType(hashPath:string):string {
        const metadatas = this.dirListResponse()?.metadatas || {};
        if (hashPath in metadatas) {
            return this.assetService.getFileMetadataMimeType(metadatas[hashPath]);
        }
        return "File";
    }

    onChangeDropdrownDirlistModeSwitch(event: Event):void {
        event.preventDefault();
        const value = parseInt((event.target as HTMLSelectElement).value);
        this.localStorageService.dirListStyle.set(value);
    }

    readonly breadcrumbSplitPath = computed(() => {
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
                "hashPath": this.fileSystemService.hashPath(this.storage(), fullPath),
            });
        }
        return result;
    });

    readonly hasImagePreviewOnList = computed(() => {
        const dirListResponse = this.dirListResponse();
        if (dirListResponse == null) {
            return false;
        }
        const allHashPaths = this.allHashPaths();
        for (let index = 0; index < allHashPaths.length; index++) {
            const hashPaths = allHashPaths[index];
            const renderedList = dirListResponse.metadatas[hashPaths]?.index[0]?.rendered || [];
            const renderedWithImgPreview = renderedList.filter(r => r.previewType == "icon-thumbnail"
                                                                    || r.previewType == "cartridge-thumbnail");
            if (renderedWithImgPreview.length > 0) {
                return true;
            }
        }

        return false;
    });

    readonly dirListStyleCSSClass = computed(() => {
        return "dirlist-mode-" + DirListStyle[this.dirListStyle()].toLowerCase().replace("_", "-");
    });

    readonly onlyDirectoriesToDisplay = computed(() => {
        const dirListResponse = this.dirListResponse();
        if (dirListResponse == null) {
            return false;
        }
        return dirListResponse.list.every(f => f.directory);
    });

    readonly iconThumbnailPreviewURLByHashPath = computed(() => {
        const dirListStyle = this.dirListStyle(); 
        const dirListResponse = this.dirListResponse();
        const result:Record<string, string> = {};
        if (dirListResponse == null) {
            return result;
        }

        const previewType = dirListStyle > this._DirListStyle.SMALL_ICONS ? "cartridge-thumbnail" : "icon-thumbnail";
        //const previewType = "cartridge-thumbnail";
        const allHashPaths = this.allHashPaths();

        for (let index = 0; index < allHashPaths.length; index++) {
            const hashPath = allHashPaths[index];
            const renderedList = dirListResponse.metadatas[hashPath]?.index[0]?.rendered || [];
            const renderedWithImgPreview = renderedList.filter(r => r.previewType == previewType);
            if (renderedWithImgPreview.length > 0) {
                result[hashPath] = this.assetService.makeAssetRenderedFileURL(hashPath, renderedWithImgPreview[0].name, 0);
            }
        }

        return result;
    });

    readonly allHashPaths = computed(() => {
        const dirListResponse = this.dirListResponse();
        if (dirListResponse == null) {
            return [];
        }
        const itemHashPaths = dirListResponse.list.map(f => f.hashPath);
        const currentItemHashPath = dirListResponse.currentItem?.hashPath || "";
        const parentHashPath = dirListResponse.parentHashPath || "";
        return [...itemHashPaths, currentItemHashPath, parentHashPath];
    });

    onClickPagination(pageNavigateButton:any):void {
        const hashPath = this.dirListResponse()?.currentItem?.hashPath;
        if (hashPath == null) {
            return;
        }
        this.list(hashPath, pageNavigateButton["skip"] || 0);
    }

    paginationButtonContent(pageNavigateButton:any):any {
        return {
            label: pageNavigateButton["page"]
        };
    }
}
