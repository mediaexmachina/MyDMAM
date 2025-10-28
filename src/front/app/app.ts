import { Component, signal, inject } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';

import { LocalStorageService } from './services/local-storage.service';
import { RealmSelectorComponent } from './components/realm-selector/realm-selector.component';

@Component({
    selector: 'app-root',
    imports: [RouterOutlet, RouterLink, RealmSelectorComponent],
    templateUrl: './app.html',
    styleUrl: './app.css'
})
export class App {
    private readonly localStorageService = inject(LocalStorageService);

    protected readonly title = signal('MyDMAM');
    selectedRealm = signal("");

    constructor() {
        try {
            this.selectedRealm.set(this.localStorageService.getSelectedRealm());
        } catch (error) {
        }
    }

    getTopMenuSelectedClassName(expected:string):string {
        return window.location.pathname.startsWith(expected) ? 'currentpage' : '';
    }

}
