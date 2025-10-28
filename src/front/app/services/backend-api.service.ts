import { Injectable, inject } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { APIResponse } from '../interfaces/api-response.interface';

@Injectable({
    providedIn: 'root'
})
export class BackendAPIService {

    private readonly httpClient = inject(HttpClient);
    private readonly API_BASE_PATH = "/api/v1";
    private readonly BASE_URL = `${window.location.protocol}//${window.location.host}${this.API_BASE_PATH}`;
    private readonly TIMEOUT = 1000;

    private onRequestError(httpError: HttpErrorResponse) {
        console.error('Managed error', httpError.status, httpError.error, this.getAPIEndPointURL(httpError.url));
    }

    /**
     * @param data only for POST and PUT
     */
    public fullRequestAPI<T>(method: "GET" | "HEAD" | "PUT" | "DELETE" | "POST", path: string, data: any = null): Observable<APIResponse<T>> {
        let url: string = `${this.BASE_URL}/${path}`;

        if (path.startsWith("/")) {
            url = `${this.BASE_URL}${path}`;
        }

        const headers = {
            "accept": "application/json",
            "content-type": "application/json; charset=utf-8",
            "x-toto": "ZZZZZZZZZZZZZZ"
        };

        const result$ = new Subject<APIResponse<T>>;

        const subscribed = {
            next: (response: HttpResponse<Object>) => {
                const returnPath = this.getAPIEndPointURL(response.url);
                console.log(method + ':', response.ok, response.status, response.body, returnPath);

                result$.next({
                    method: method,
                    path: returnPath,
                    status: response.status,
                    data: <T>response.body
                });
                result$.complete();
            },
            error: (httpError: HttpErrorResponse) => {
                result$.complete();
                this.onRequestError(httpError);
            }
        }

        switch (method) {
            case "GET": {
                this.httpClient.get(url, {
                    headers: headers,
                    timeout: this.TIMEOUT,
                    observe: 'response',
                    mode: 'same-origin',
                    cache: 'force-cache',
                    credentials: 'same-origin',
                    redirect: 'follow'
                }).subscribe(subscribed);
                break;
            }
            case "HEAD": {
                this.httpClient.head(url, {
                    headers: headers,
                    timeout: this.TIMEOUT,
                    observe: 'response',
                    mode: 'same-origin',
                    cache: 'force-cache',
                    credentials: 'same-origin',
                    redirect: 'follow'
                }).subscribe(subscribed);
                break;
            }
            case "POST": {
                this.httpClient.post(url, data, {
                    headers: headers,
                    timeout: this.TIMEOUT,
                    observe: 'response',
                    mode: 'same-origin',
                    cache: 'no-cache',
                    credentials: 'same-origin',
                    redirect: 'follow'
                }).subscribe(subscribed);
                break;
            }
            case "PUT": {
                this.httpClient.put(url, data, {
                    headers: headers,
                    timeout: this.TIMEOUT,
                    observe: 'response',
                    mode: 'same-origin',
                    cache: 'no-cache',
                    credentials: 'same-origin',
                    redirect: 'follow'
                }).subscribe(subscribed);
                break;
            }
            case "DELETE": {
                this.httpClient.delete(url, {
                    headers: headers,
                    timeout: this.TIMEOUT,
                    observe: 'response',
                    mode: 'same-origin',
                    cache: 'no-cache',
                    credentials: 'same-origin',
                    redirect: 'follow'
                }).subscribe(subscribed);
                break;
            }
        }

        return result$.asObservable();
    }

    /**
     * @param data only for POST and PUT
     */
    public requestAPI<T>(method: "GET" | "HEAD" | "PUT" | "DELETE" | "POST", path: string, data: any = null): Observable<T> {
        const result$ = new Subject<T>;
        this.fullRequestAPI<T>(method, path, data)
            .subscribe(result => result$.next(result.data));
        return result$.asObservable();
    }

    /**
     * @param url like "http://localhost:4200/api/v1/ping/fail"
     * @returns end url like "/ping/fail"
     */
    private getAPIEndPointURL(url: string | null): string {
        if (url == null) {
            return "";
        }
        return url.replace(this.BASE_URL, "");
    }

}
