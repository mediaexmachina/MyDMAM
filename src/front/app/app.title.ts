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
import { effect, inject, Injectable, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { InstanceService } from './services/instance.service';

@Injectable({ providedIn: 'root' })
export class AppTitle extends TitleStrategy {

    private readonly outputTitle = inject(Title);
    private readonly instanceService = inject(InstanceService);

    public set(title:string) {
        let outputTitle = this.instanceService.siteConf().name;
        if (title != "") {
            outputTitle = title + " :: " + outputTitle;
        }
        this.outputTitle.setTitle(outputTitle);
    }

    override updateTitle(routerState: RouterStateSnapshot) {
        const routerTitle = this.buildTitle(routerState);
        if (routerTitle != undefined && routerTitle != null) {
            this.set(routerTitle);
        } else {
            this.set("");
        }
    }

}
