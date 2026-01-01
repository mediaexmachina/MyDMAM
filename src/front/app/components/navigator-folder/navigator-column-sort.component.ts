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
import { SortOrder } from '../../dto/sort-order.enum';

@Component({
  selector: 'app-navigator-column-sort',
  imports: [],
  template: `<button (click)="onClick($event)">{{ sortLabel() }}</button>`,
  styles: ``,
})
export class NavigatorColumnSortComponent {

    readonly name = input.required<string>();
    readonly sortOrder = input.required<SortOrder>();
    readonly onChangeSort = output<SortOrder>();

    private readonly isAsc = computed(() => { return this.sortOrder().toString() == SortOrder[1].toString() });
    private readonly isDesc = computed(() => { return this.sortOrder().toString() == SortOrder[2].toString() });

    readonly sortLabel = computed(() => {
        const name = this.name();
        if (this.isAsc()) {
            return "Sorted by " + name.toLowerCase();
        } else if (this.isDesc()) {
            return "Reverse sort by " + name.toLowerCase();
        } else {
            return name;
        }
    });

    onClick(e: Event) {
        e.preventDefault();
        if (this.isAsc()) {
            this.onChangeSort.emit(SortOrder.desc);
        } else if (this.isDesc()) {
            this.onChangeSort.emit(SortOrder.none);
        } else {
            this.onChangeSort.emit(SortOrder.asc);
        }
    }

}
