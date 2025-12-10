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
import { Injectable, inject } from '@angular/core';
import { Observable, Subject, firstValueFrom } from 'rxjs';
import { HttpClient, HttpErrorResponse, HttpResponse, HttpParams } from '@angular/common/http';
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
    public requestObservableAPI<T>(method: "GET" | "HEAD" | "PUT" | "DELETE" | "POST",
                                   path: string,
                                   params: any = {},
                                   data: any = null): Observable<APIResponse<T>> {
        let url: string = `${this.BASE_URL}/${path}`;

        if (path.startsWith("/")) {
            url = `${this.BASE_URL}${path}`;
        }

        const headers = {
            "accept": "application/json",
            "content-type": "application/json; charset=utf-8",
        };

        let httpParams = new HttpParams();
        for (const key in params) {
            if (params.hasOwnProperty(key)) {
                httpParams = httpParams.append(key, params[key]);
            }
        }

        const result$ = new Subject<APIResponse<T>>;

        const subscribed = {
            next: (response: HttpResponse<Object>) => {
                const returnPath = this.getAPIEndPointURL(response.url);
                console.log(method + ':', response.ok, response.status, response.body, returnPath);

                result$.next({
                    method: method,
                    path: returnPath,
                    status: response.status,
                    isOk: true,
                    data: <T>response.body
                });
                result$.complete();
            },
            error: (httpError: HttpErrorResponse) => {
                result$.next({
                    method: method,
                    path: this.getAPIEndPointURL(httpError.url),
                    status: httpError.status,
                    isOk: false,
                    data: null
                });
                result$.complete();
                this.onRequestError(httpError);
            }
        }

        switch (method) {
            case "GET": {
                this.httpClient.get(url, {
                    params: httpParams,
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
                    params: httpParams,
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
                    params: httpParams,
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
                    params: httpParams,
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
                    params: httpParams,
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
    public async requestAsyncAPI<T>(method: "GET" | "HEAD" | "PUT" | "DELETE" | "POST",
                                    path: string,
                                    params: any = {},
                                    data: any = null): Promise<T|null> {
        const result = await firstValueFrom(this.requestObservableAPI<T>(method, path, params, data));
        if (result.isOk == false) {
            return null;
        }
        return result.data;
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
