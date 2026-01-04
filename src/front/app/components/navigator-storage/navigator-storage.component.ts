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

import { Component, computed, inject, signal, Signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LocalStorageService } from '../../services/local-storage.service';
import { FileSystemService } from '../../services/file-system.service';
import { StorageListResponse } from '../../dto/storage-list-response.interface';
import { StorageState } from '../../dto/storage-state.interface';
import { StorageStateClass } from '../../dto/storage-state-class.enum';
import { StorageCategory } from '../../dto/storage-category.enum';
import { FirstUpperCasePipe } from '../../pipes/first-upper-case-pipe';

@Component({
    selector: 'app-navigator-storage',
    imports: [RouterLink, FirstUpperCasePipe],
    templateUrl: './navigator-storage.component.html',
    styles: `
        ul {
            padding-inline-start: 0;
        }
        li {
            display: flex;
            flex-wrap: wrap;
            padding-top: .75rem;
            padding-bottom: .75rem;
            padding-left: 1em;
            padding-right: 1em;
        }
        .row > * {
            width: 100%;
            max-width: 100%;
        }
    `
})
export class NavigatorStorageComponent {

    readonly _StorageStateClass = StorageStateClass;
    readonly _StorageCategory = StorageCategory;
    readonly localStorageService = inject(LocalStorageService);
    readonly fileSystemService = inject(FileSystemService);
    readonly storageListResponse = signal<StorageListResponse | null>(null);
    readonly storageStateNames = computed(() => Object.keys(this.storageListResponse()?.storageStates || {}));

    constructor() {
        this.fileSystemService.getStorages().then(s => this.storageListResponse.set(s));
    }

    protected getStorageStateByName(name:string):StorageState {
        const storageStates = this.storageListResponse()?.storageStates || {};
        return storageStates[name];
    }

    protected isStoragePresentInDatabase(name:string):boolean {
        return (this.storageListResponse()?.storages || []).indexOf(name) != -1;
    }
}
