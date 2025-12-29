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
import { SpanFileSizeComponent } from "../toolkit/span-file-size.component";

@Component({
    selector: 'app-search-constraints-size-range-editor',
    imports: [SpanFileSizeComponent],
    template: `
    @let currentRange = range();
    @if(editMode()) {
        Select the minimum file size:
        <input
            #inputminsize
            type="number"
            placeholder="Minimum"
            [value]="currentRange.min" />
        and the maximum file size:
        <input
            #inputmaxsize
            type="number"
            placeholder="Maximum"
            [value]="rangeMax()" />
        <button (click)="switchOffEditMode($event)">Ok</button>
        <button (click)="setToNotRestricted($event)">Not restricted</button>

    } @else {
        @if (currentRange.restricted) {
            File size restricted, minimum to <app-span-file-size [size]="currentRange.min"></app-span-file-size>,
            and maximum to <app-span-file-size [size]="currentRange.max"></app-span-file-size>.
        } @else {
            No file size restricted.
        }
        <button (click)="switchToEditMode($event)">Change</button>
    }
  `,
    styles: ``,
})
export class SearchConstraintsSizeRangeEditorComponent {

    readonly range = input.required<SearchConstraintRange>();
    readonly onChange = output<SearchConstraintRange>();
    readonly editMode = signal(false);
    readonly rangeMax = computed(() => {
        const max = this.range().max;
        if (max == null || max <= 0) {
            return 50 * 1000 * 1000 * 1000;
        }
        return max;
    });

    readonly inputminsize = viewChild<ElementRef<HTMLInputElement>>('inputminsize');
    readonly inputmaxsize = viewChild<ElementRef<HTMLInputElement>>('inputmaxsize');

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

        const min = this.inputminsize()?.nativeElement.valueAsNumber || 0;
        const max = this.inputmaxsize()?.nativeElement.valueAsNumber || 0;

        this.onChange.emit({
            restricted: true,
            min: min,
            max: max
        });
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


}
