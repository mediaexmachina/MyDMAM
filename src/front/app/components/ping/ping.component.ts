import { Component, inject, effect } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { Observable } from 'rxjs';
import { PingResponse } from '../../interfaces/ping-response.interface';
import { PingService } from '../../services/ping.service';

@Component({
    selector: 'app-ping',
    imports: [AsyncPipe],
    templateUrl: './ping.component.html',
    styleUrl: './ping.component.css'
})
export class PingComponent {

    readonly pingService = inject(PingService);
    pingResponse0$!: Observable<PingResponse>;
    pingResponse1$!: Observable<PingResponse>;

    constructor() {
        effect(() => {
            this.dispatch();
        });
    }

    private dispatch():void {
        this.pingResponse0$ = this.pingService.makePing("Ceci est un test0");
        this.pingResponse1$ = this.pingService.makePing("Ceci est un test1");
    }

}
