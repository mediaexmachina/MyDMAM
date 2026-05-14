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
import { inject, Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({
    name: 'nl2br',
})
export class Nl2brPipe implements PipeTransform {

    private sanitized: DomSanitizer = inject(DomSanitizer);

    /**
     * @see https://stackoverflow.com/questions/35999185/angular2-pipes-output-raw-html/51236958#51236958
     */
    transform(value: string, ...args: unknown[]): SafeHtml {
        return this.sanitized.bypassSecurityTrustHtml(value.trim().replace(/(?:\r\n|\r|\n)/g, '<br />'));
    }

}
