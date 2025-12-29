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

import { Component, ElementRef, input, output, signal, viewChild, computed, inject } from '@angular/core';
import { SearchConstraintRange } from '../../dto/search-constraint-range.interface';
import { DatePipe } from '@angular/common';
import { SpanDateTimeComponent } from "../toolkit/span-date-time.component";

@Component({
    selector: 'app-search-constraint-date-range-editor',
    imports: [DatePipe, SpanDateTimeComponent],
    template: `
    @let currentRange = range();
    @if(editMode()) {
        Select from the
        <input
            #inputfromdate
            type="datetime-local"
            [value]="rangeMin() | date:'yyyy-MM-ddTHH:mm'"
            (change)="onChangeDate()" />
        to the
        <input
            #inputtodate
            type="datetime-local"
            [value]="rangeMax() | date:'yyyy-MM-ddTHH:mm'"
            (change)="onChangeDate()" />
        <button (click)="switchOffEditMode($event)">Ok</button>
        <button (click)="setToNotRestricted($event)">Not restricted</button>

    } @else {
        @if (currentRange.restricted) {
            Date restricted, from
            <app-span-date-time [date]="currentRange.min"></app-span-date-time>
            to
            <app-span-date-time [date]="currentRange.max"></app-span-date-time>.
        } @else {
            No date restricted.
        }
        <button (click)="switchToEditMode($event)">Change</button>
    }
  `,
    styles: ``,
})
export class SearchConstraintDateRangeEditorComponent {

    readonly range = input.required<SearchConstraintRange>();
    readonly onChange = output<SearchConstraintRange>();
    readonly editMode = signal(false);
    readonly rangeMin = computed(() => {
        const min = this.range().min;
        if (min == null || min <= 0) {
            const now = new Date();
            now.setFullYear(now.getFullYear() - 1);
            return now.getTime();
        }
        return min;
    });
    readonly rangeMax = computed(() => {
        const max = this.range().max;
        if (max == null || max <= 0) {
            return (new Date()).getTime();
        }
        return max;
    });
    readonly inputfromdate = viewChild<ElementRef<HTMLInputElement>>('inputfromdate');
    readonly inputtodate = viewChild<ElementRef<HTMLInputElement>>('inputtodate');

    switchToEditMode(e: Event): void {
        e.preventDefault();
        this.editMode.set(true);
        this.onChange.emit({
            restricted: true,
            min: this.range().min,
            max: this.range().max
        })
    }

    switchOffEditMode(e: Event): void {
        e.preventDefault();
        this.editMode.set(false);
        this.onChangeDate();
    }

    setToNotRestricted(e: Event): void {
        e.preventDefault();
        this.editMode.set(false);
        this.onChange.emit({
            restricted: false,
            min: this.range().min,
            max: this.range().max
        });
    }

    onChangeDate(): void {
        const min = this.inputfromdate()?.nativeElement.valueAsNumber || 0;
        const max = this.inputtodate()?.nativeElement.valueAsNumber || 0;

        this.onChange.emit({
            restricted: true,
            min: min,
            max: max
        });
    }
}
