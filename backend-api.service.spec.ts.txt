import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpResponse, HttpHeaders } from '@angular/common/http';
import { Observable, Subject, timeout, Observer } from 'rxjs';
import { BackendAPIService } from '../services/backend-api.service';
import { APIResponse } from '../interfaces/api-response.interface';
import { NONE_TYPE } from '@angular/compiler';

describe('BackendAPIService', () => {
    let service: BackendAPIService;
    let httpClientSpy: jasmine.SpyObj<HttpClient>;
    let httpResponseSpy: jasmine.SpyObj<Observable<HttpResponse<Object>>>;
    let path:string;
    let httpResponseSpySubscribed: jasmine.SpyObj<HttpResponse<Object>>;

    beforeEach(() => {
        const spy = jasmine.createSpyObj('HttpClient', ['get', 'head', 'post', 'put', 'delete']);

        TestBed.configureTestingModule({
            providers: [{ provide: HttpClient, useValue: spy }]
        });
        service = TestBed.inject(BackendAPIService);
        httpClientSpy = TestBed.inject(HttpClient) as jasmine.SpyObj<HttpClient>;
        httpResponseSpy = jasmine.createSpyObj('Observable<HttpResponse<Object>>', ['subscribe']);
        httpClientSpy.get.and.returnValue(httpResponseSpy);
        httpClientSpy.head.and.returnValue(httpResponseSpy);
        httpClientSpy.post.and.returnValue(httpResponseSpy);
        httpClientSpy.put.and.returnValue(httpResponseSpy);
        httpClientSpy.delete.and.returnValue(httpResponseSpy);

        path = "/ddd";
        httpResponseSpySubscribed = jasmine.createSpyObj('HttpResponse<Object>', [], ['url', 'ok', 'status', 'body']);
        // spyOnProperty(httpResponseSpySubscribed, 'url').and.returnValue(`http://aaa:123${path}`);
        //httpResponseSpySubscribed.url.and
        //httpResponseSpySubscribed.url.and.returnValue("");
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    it('should be request GET ok', () => {
        service.requestAPI<UUU>("GET", path);

        expect(httpClientSpy.get.calls.count()).toBe(1);
        const callConnect = httpClientSpy.get.calls.first();
        expect(URL.parse(callConnect.args[0])?.pathname).toBe(`/api/v1${path}`);
        expect(callConnect.args[1]?.timeout).toBe(1000);
        expect(callConnect.args[1]?.observe?.trim()).toBe("response");
        expect(callConnect.args[1]?.mode?.trim()).toBe("same-origin");
        expect(callConnect.args[1]?.cache?.trim()).toBe("force-cache");
        expect(callConnect.args[1]?.credentials?.trim()).toBe("same-origin");
        expect(callConnect.args[1]?.redirect?.trim()).toBe("follow");

        const headers = (callConnect.args[1]?.headers as Record<string, string | string[]>)
        expect(headers["accept"]).toBe("application/json");
        expect(headers["content-type"]).toBe("application/json; charset=utf-8");

        expect(httpResponseSpy.subscribe.calls.count()).toBe(1);

        const subscribed = httpResponseSpy.subscribe.calls.first().args[0] as Partial<Observer<HttpResponse<Object>>>;
        if (subscribed.next != undefined) {
            subscribed.next(httpResponseSpySubscribed);
            // check...
        }

        // error
        /*if (subscribed.error != undefined) {
            subscribed.error(httpResponseSpySubscribed);
        }*/

        // console.log(httpResponseSpy.subscribe.calls.first().args[0]?.toString());
        // https://stackoverflow.com/questions/64560390/jasmine-createspyobj-with-properties

    });
});

interface UUU {
}
