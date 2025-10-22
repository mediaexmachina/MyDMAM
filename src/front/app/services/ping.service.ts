import { Injectable, inject } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { BackendAPIService } from './backend-api.service';
import { PingRequest } from '../interfaces/ping-request.interface';
import { PingResponse } from '../interfaces/ping-response.interface';

@Injectable({
  providedIn: 'root'
})
export class PingService {

    readonly backendAPIService = inject(BackendAPIService);

    makePing(payload: string): Observable<PingResponse> {
        const data: PingRequest = {
            payload: payload
        }
        return this.backendAPIService.requestAPI<PingResponse>("POST", "/ping", data);
    }

}
