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
 * Copyright (C) Media ex Machina 2026
 * 
 */
import { Injectable, Signal, WritableSignal, inject, signal } from '@angular/core';

import { BackendAPIService } from './backend-api.service';
import { SiteConf } from '../dto/site-conf.interface';
import { LocalStorageService } from './local-storage.service';
import { RealmAboutConf } from '../dto/realm-about-conf.interface';

@Injectable({
    providedIn: 'root',
})
export class InstanceService {

    private readonly backendAPIService = inject(BackendAPIService);
    private readonly localStorageService = inject(LocalStorageService);

    private readonly internalSiteConf: WritableSignal<SiteConf>;
    readonly siteConf: Signal<SiteConf>;

    constructor () {
        this.internalSiteConf = signal({
            name: "",
            description: "",
            location: "",
            pageFooter: ""          
        });
        this.siteConf = this.internalSiteConf.asReadonly();

        this.backendAPIService.requestAsyncAPI<SiteConf>("GET", `/instance/site`)
            .then(site => {
                if (site != null) {
                    this.internalSiteConf.set(site);
                }
            });
    }

    public async getAboutRealm(): Promise<RealmAboutConf|null> {
        const realm = this.localStorageService.getSelectedRealm();
        return this.backendAPIService.requestAsyncAPI<null>("GET", `/instance/about/${realm}`, {});
    }

}
