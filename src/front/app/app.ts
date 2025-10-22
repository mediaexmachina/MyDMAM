import { Component, signal, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BackendAPIService } from '../app/services/backend-api.service';
import { PingComponent } from '../app/components/ping/ping.component';

@Component({
    selector: 'app-root',
    imports: [RouterOutlet, PingComponent],
    templateUrl: './app.html',
    styleUrl: './app.css'
})
export class App {
    protected readonly title = signal('mydmam');

    readonly backendAPIService = inject(BackendAPIService);

}
