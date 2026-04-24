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
import { Routes } from '@angular/router';

import { HomePageComponent } from './components/home-page/home-page.component';
import { RealmSelectorComponent } from './components/realm-selector/realm-selector.component';
import { NavigatorStorageComponent } from './components/navigator-storage/navigator-storage.component';
import { NavigatorFolderComponent } from './components/navigator-folder/navigator-folder.component';
import { SearchPageResultsComponent } from './components/search-page-results/search-page-results.component';

export const routes: Routes = [
    {
        path: '',
        title: 'Home',
        component: HomePageComponent,
    },
    {
        path: 'realm',
        title: 'Realm selector',
        component: RealmSelectorComponent,
    },
    {
        path: 'navigator',
        title: 'Storage selector',
        component: NavigatorStorageComponent,
    },
    {
        path: 'navigator/:storage',
        component: NavigatorFolderComponent,
    },
    {
        path: 'navigator/:storage/:hashPath',
        component: NavigatorFolderComponent,
    },
    {
        path: 'search',
        component: SearchPageResultsComponent,
    }
];
