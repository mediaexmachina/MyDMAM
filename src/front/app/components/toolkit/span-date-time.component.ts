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

import { Component, computed, inject, input } from '@angular/core';
import { LocalStorageService } from '../../services/local-storage.service';
import { DateTimeStyleToDisplay } from '../../enums/date-time-style-to-display.enum';

@Component({
    selector: 'app-span-date-time',
    imports: [],
    template: `
    <span (click)="onClick($event)">
        {{ dateToDisplay() }}
    </span>
  `,
    styles: `
    span:hover {
        color: rgb(2, 65, 211);
        border-bottom: 1px dotted #0000005d;
        cursor: help;
        user-select: none;
    }
    `,
})
export class SpanDateTimeComponent {

    readonly ONE_MINUTE = 1000 * 60;
    readonly ONE_HOUR = this.ONE_MINUTE * 60;
    readonly ONE_DAY = this.ONE_HOUR * 24;
    readonly ONE_MONTH = this.ONE_DAY * 29.53059;
    readonly ONE_YEAR = this.ONE_DAY * 365.259636;

    readonly date = input.required<number>();
    readonly localStorageService = inject(LocalStorageService);

    readonly dateToDisplay = computed(() => {
        const date = this.date();
        const dateStyleToDisplay = this.localStorageService.dateStyleToDisplay();

        if (date <= 0) {
            return "(no date to display)";
        }
        const dateObj = new Date(date);
        const now = new Date().getTime();

        let options: Intl.DateTimeFormatOptions = {};

        if (dateStyleToDisplay == DateTimeStyleToDisplay.RELATIVE) {
            const deltaMSec = Math.abs(date - now);
            if (Math.round(deltaMSec) == 0) {
                return "Just now";
            }
            const result:Array<string> = [];
            if (date - now > 0) {
                result.push("In");
            }

            function plural(value: number, label:string):string {
                if (value > 1) {
                    return value + " " + label + "s";
                }
                return value + " " + label;
            }

            let computedMSec = deltaMSec;
            const years = Math.floor(computedMSec / this.ONE_YEAR);
            computedMSec = computedMSec - (years * this.ONE_YEAR);
            const months = Math.floor(computedMSec / this.ONE_MONTH);
            computedMSec = computedMSec - (months * this.ONE_MONTH);
            const days = Math.floor(computedMSec / this.ONE_DAY);
            computedMSec = computedMSec - (days * this.ONE_DAY);
            const hours = Math.floor(computedMSec / this.ONE_HOUR);
            computedMSec = computedMSec - (hours * this.ONE_HOUR);
            const min = Math.floor(computedMSec / this.ONE_MINUTE);
            computedMSec = computedMSec - (min * this.ONE_MINUTE);
            const sec = Math.floor(computedMSec / 1000);
            const msec = Math.round(computedMSec - (sec * 1000));

            if (years > 0) {
                result.push(plural(years, "year"));
                result.push(plural(months, "month"));
            } else if (months > 0) {
                result.push(plural(months, "month"));
                result.push(plural(days, "day"));
            } else if (days > 0) {
                result.push(plural(days, "day"));
                result.push(plural(hours, "hour"));
            } else if (hours > 0) {
                result.push(plural(hours, "hour"));
                result.push(plural(min, "minute"));
            } else if (min > 0) {
                result.push(plural(min, "minute"));
                result.push(plural(sec, "second"));
            } else if (sec > 0) {
                result.push(plural(sec, "second"));
                result.push(plural(msec, "millisecond"));
            } else {
                result.push(plural(msec, "millisecond"));
            }

            if (date - now < 0) {
                result.push("ago");
            }
            return result.join(" ");
        } else if (dateStyleToDisplay == DateTimeStyleToDisplay.FULL_DATE_TIME || date >= now) {
            options = {
                weekday: 'short',
                month: 'short',
                day: '2-digit',
                year: "numeric",
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: false
            };
        } else if (date < now - this.ONE_YEAR) {
            options = {
                month: 'short',
                year: "numeric",
            }
        } else if (date < now - this.ONE_MONTH) {
            options = {
                weekday: 'short',
                month: 'short',
                day: '2-digit',
                year: "numeric",
            };
        } else if (date < now - this.ONE_DAY) {
            options = {
                weekday: 'short',
                month: 'short',
                day: '2-digit',
                year: "numeric",
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            };
        } else {
            options = {
                weekday: 'short',
                month: 'short',
                day: '2-digit',
                year: "numeric",
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit',
                hour12: false
            };
        }

        return dateObj.toLocaleDateString("en-US", options);
    });

    onClick(e: Event): void {
        e.preventDefault();

        this.localStorageService.dateStyleToDisplay.update(v => {
            return v == 2 ? 0 : v + 1;
        });
    }

}
