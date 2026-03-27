import { Component, signal, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { LocalStorageService } from './services/local-storage.service';
import { RealmSelectorComponent } from './components/realm-selector/realm-selector.component';
import { TopBarComponent } from "./components/top-bar/top-bar.component";
import { InstanceService } from './services/instance.service';
import { RealmAboutConf } from './dto/realm-about-conf.interface';

@Component({
    selector: 'app-root',
    imports: [RouterOutlet, RealmSelectorComponent, TopBarComponent],
    template: `
        <app-top-bar [realm]="selectedRealm()" [aboutRealm]="selectedAboutRealm()"></app-top-bar>

        <main>
            @if (selectedRealm() == "") {
            <app-realm-selector [(selectedRealm)]="selectedRealm" [(selectedAboutRealm)]="selectedAboutRealm"></app-realm-selector>
            }
            @else {
            <router-outlet />
            }
        </main>
        <footer>
            {{ siteConf().pageFooter }}
        </footer>
        `,
    styles: `
    main {
        margin-top: 1em;
        margin-bottom: 1em;
        margin-left: 0.8vw;
        margin-right: 0.8vw;
    }
    footer {
        margin-top: 2em;
        text-align: center;
        opacity: 0.5;
        font-weight: 200;
    }
    `
})
export class App {

    private readonly localStorageService = inject(LocalStorageService);
    private readonly instanceService = inject(InstanceService);

    readonly siteConf = this.instanceService.siteConf;
    readonly selectedRealm = signal("");
    readonly selectedAboutRealm = signal<RealmAboutConf>({
        longName: "",
        contact: "",
        logo: "",
        color: ""
    });

    constructor() {
        try {
            const realmName = this.localStorageService.getSelectedRealm();
            this.selectedRealm.set(realmName);
            this.instanceService.getAboutRealm()
                .then(a => {
                    if (a != null) {
                        this.selectedAboutRealm.set(a);
                    }
                })
        } catch (error) {
        }

    }

}
