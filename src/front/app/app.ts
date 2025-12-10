import { Component, signal, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { LocalStorageService } from './services/local-storage.service';
import { RealmSelectorComponent } from './components/realm-selector/realm-selector.component';
import { TopBarComponent } from "./components/top-bar/top-bar.component";

@Component({
    selector: 'app-root',
    imports: [RouterOutlet, RealmSelectorComponent, TopBarComponent],
    template: `
        <app-top-bar [title]="title()" [realm]="selectedRealm()"></app-top-bar>

        <main style="margin: 1em">
            @if (selectedRealm() == "") {
            <app-realm-selector [(selectedRealm)]="selectedRealm"></app-realm-selector>
            }
            @else {
            <router-outlet />
            }
        </main>`,
    styles: ``
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

}
