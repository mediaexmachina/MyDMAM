import { Routes } from '@angular/router';

import { RealmSelectorComponent } from './components/realm-selector/realm-selector.component';
import { NavigatorStorageComponent } from './components/navigator-storage/navigator-storage.component';
import { NavigatorFolderComponent } from './components/navigator-folder/navigator-folder.component';


export const routes: Routes = [
    {
        path: 'realm',
        component: RealmSelectorComponent,
    },
    {
        path: 'navigator',
        component: NavigatorStorageComponent,
    },
    {
        path: 'navigator/:storage',
        component: NavigatorFolderComponent,
    },
    {
        path: 'navigator/:storage/:hashPath',
        component: NavigatorFolderComponent,
    }
];
