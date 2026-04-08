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
 * Copyright (C) Media ex Machina 2026
 * 
 */
import { Component, computed, input, output } from '@angular/core';

@Component({
    selector: 'app-pagination',
    imports: [],
    template: `
        <ul>
            @let previousButton = pageNavigatePreviousButton();
            @if (previousButton["display"] != 0) {
                <li class="button  preview-next" (click)="onClickBtn.emit(previousButton)">
                    Previous
                </li>
            }

            @for (page of pageNavigate(); track page["page"]) {
                @let currentButtonContent = buttonContent();
                @let imgURL = currentButtonContent(page)["imgURL"];

                @if (page["separator"] == 1) {
                    @if (page["displayCurent"] > 0) {
                        <li class="selected">
                            @if (imgURL) {
                                <div class="img">
                                    <img [src]="imgURL" />
                                </div>
                                {{ currentButtonContent(page)["label"] }}
                            }
                            @else {
                                ... {{ currentButtonContent(page)["label"] }} ...
                            }
                        </li>
                    } @else {
                        <li class="separator">...</li>
                    }
                } @else {
                    @if (page["selected"] == 1) {
                        <li class="selected">
                            @if (imgURL) {
                                <div class="img">
                                    <img [src]="imgURL" />
                                </div>
                            }
                            {{ currentButtonContent(page)["label"] }}
                        </li>
                    } @else {
                        <li class="button" (click)="onClickBtn.emit(page)">
                            @if (imgURL) {
                                <div class="img">
                                    <img [src]="imgURL" />
                                </div>
                            }
                            {{ currentButtonContent(page)["label"] }}
                        </li>
                    }
                }
            }

            @let nextButton = pageNavigateNextButton();
            @if (nextButton["display"] != 0) {
                <li class="button preview-next" (click)="onClickBtn.emit(nextButton)">
                    Next
                </li>
            }
        </ul>
    `,
    styles: `
        ul {
            font-size: 1.1em;
            list-style-type: none;
            padding-inline-start: 0em;
            display: flex;
            flex-direction: row;
            flex-wrap: wrap;
            text-align: center;
            align-items: stretch;
        }

        ul li {
            vertical-align: middle;
            margin: 0.2em;
            padding: 0.6em 1.2em;
        }

        ul li.selected {
            color: rgba(0, 0, 0, 0.484);
            border: 0.1em solid rgba(0, 0, 0, 0.048);
            cursor: default;
        }

        li.preview-next {
            display : flex;
            align-items : center;
        }

        ul li.button {
            color: blue;
            border: 0.1em solid rgba(0, 0, 0, 0.128);
            cursor: pointer;
        }

        ul li.button:hover {
            border: 0.1em dotted rgba(0, 0, 0, 0.2);
        }

        ul li.separator {
            background-color: rgba(0, 0, 0, 0);
        }

        ul li.separator:hover {
            background-color: rgba(0, 0, 0, 0);
        }

        div.img {
            width: 64px;
            height: 64px;
        }
    `,
})
export class PaginationComponent {

    readonly skip = input.required<number>();
    readonly listResultCount = input.required<number>();
    readonly listSize = input.required<number>();
    readonly total = input.required<number>();
    readonly maxPageCount = input.required<number>();
    readonly buttonContent = input.required<Function>();

    readonly onClickBtn = output<{}>();

    readonly limit = computed(() => {
        return Math.max(this.listResultCount(), this.listSize());
    });

    private makePageNavigateButton(pageNum:number, limit:number, skip: number): Record<string, number> {
        return {
            "page": pageNum + 1,
            "skip": pageNum * limit,
            "selected": ((pageNum * limit <= skip) && (skip < (pageNum + 1) * limit)) ? 1 : 0,
            "separator": 0,
            "displayCurent": 0
        }
    }

    readonly pageNavigate = computed(() => {
        const currentPage = Math.ceil(this.skip() / this.limit());
        const pageCount = Math.ceil(this.total() / this.limit());
        const result = new Array<Record<string, number>>();
        const maxPageCount = this.maxPageCount();

        if (pageCount < maxPageCount) {
            for (let pageNum = 0; pageNum < pageCount; pageNum++) {
                result.push(this.makePageNavigateButton(pageNum, this.limit(), this.skip()));
            }
        } else {
            const splitPageCount = maxPageCount / 2;
            for (let pageNum = 0; pageNum < splitPageCount; pageNum++) {
                result.push(this.makePageNavigateButton(pageNum, this.limit(), this.skip()));
            }

            if (currentPage > splitPageCount - 1
                && currentPage < (pageCount - splitPageCount)) {
                result.push({
                    "separator": 1,
                    "page": currentPage + 1,
                    "displayCurent": currentPage + 1
                });
            } else {
                result.push({
                    "separator": 1,
                    "page": 0,
                    "displayCurent": 0
                });
            }

            for (let pageNum = pageCount - splitPageCount; pageNum < pageCount; pageNum++) {
                result.push(this.makePageNavigateButton(pageNum, this.limit(), this.skip()));
            }
        }

        return result;
    });
    
    readonly pageNavigatePreviousButton = computed(() => {
        if (this.total() == 0) {
            return {"display": 0};
        }

        const currentPage = Math.ceil(this.skip() / this.limit());
        const isFirstPage = currentPage == 0;

        if (isFirstPage) {
            return {"display": 0};
        }

        return {
            "page": currentPage - 1,
            "skip": (currentPage - 1) * this.limit()
        }        
    });

    readonly pageNavigateNextButton = computed(() => {
        if (this.total() == 0) {
            return {"display": 0};
        }

        const currentPage = Math.ceil(this.skip() / this.limit());
        const isLastPage = this.total() - this.skip() <= this.limit();

        if (isLastPage) {
            return {"display": 0};
        }

        return {
            "page": currentPage + 1,
            "skip": (currentPage + 1) * this.limit()
        }        
    });   
    

}
