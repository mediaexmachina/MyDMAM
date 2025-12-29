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

import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-span-file-size',
  imports: [],
  template: `<span>{{ sizeToDisplay() }}</span>`,
  styles: ``,
})
export class SpanFileSizeComponent {

    readonly size = input.required<number>();
    readonly numberFormat = new Intl.NumberFormat('en-US');

    readonly sizeToDisplay = computed(() => {
        const size = this.size();
        if (size <= 0) {
            return "0 byte";
        } else if (size == 1) {
            return "1 byte";
        }

        return this.numberFormat.format(size) + " bytes";
    });

}
