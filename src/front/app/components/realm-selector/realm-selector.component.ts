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
import { Component, inject, model, signal, Signal, WritableSignal } from '@angular/core';

import { LocalStorageService } from '../../services/local-storage.service';
import { FileSystemService } from '../../services/file-system.service';
import { RealmListResponse } from '../../dto/realm-list-response.interface';
import { RealmAboutConf } from '../../dto/realm-about-conf.interface';

@Component({
  selector: 'app-realm-selector',
  imports: [],
  templateUrl: './realm-selector.component.html',
  styleUrl: './realm-selector.component.css'
})
export class RealmSelectorComponent {

    readonly localStorageService = inject(LocalStorageService);
    readonly fileSystemService = inject(FileSystemService);

    readonly realmListResponse = signal<RealmListResponse|null>(null);

    selectedRealm = model("");
    selectedAboutRealm = model<RealmAboutConf|null>(null);

    constructor() {
        this.fileSystemService.getRealms()
            .then(realmListResponse => {
                if (realmListResponse == null || realmListResponse == undefined ) {
                    throw new Error("Can't get realm list from backend.");
                }
                this.realmListResponse.set(realmListResponse);

                if (realmListResponse.realms.includes(this.selectedRealm()) == false) {
                    this.localStorageService.setSelectedRealm();
                    this.selectedRealm.set("");
                }
            });
    }

    selectRealm(realm: string, about: RealmAboutConf|null): void {
        this.localStorageService.setSelectedRealm(realm);
        this.selectedRealm.set(realm);
        if (about != null) {
            this.selectedAboutRealm.set(about);
        }
    }

}
