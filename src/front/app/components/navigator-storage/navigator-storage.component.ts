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

import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LocalStorageService } from '../../services/local-storage.service';
import { FileSystemService } from '../../services/file-system.service';
import { StorageListResponse } from '../../dto/storage-list-response.interface';

@Component({
    selector: 'app-navigator-storage',
    imports: [RouterLink],
    templateUrl: './navigator-storage.component.html',
    styleUrl: './navigator-storage.component.css'
})
export class NavigatorStorageComponent {

    readonly localStorageService = inject(LocalStorageService);
    readonly fileSystemService = inject(FileSystemService);

    storageListResponse: StorageListResponse | null = null;

    constructor() {
        this.fileSystemService.getStorages().then(s => this.storageListResponse = s);
    }

}
