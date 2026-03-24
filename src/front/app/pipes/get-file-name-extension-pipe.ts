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
import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
    name: 'getFileNameExtension',
})
export class GetFileNameExtensionPipe implements PipeTransform {

    transform(value: string | null, addDot:boolean): string {
        if (value == null || value == "") {
            return "";
        }

        const lastDot = value.lastIndexOf(".");
        if (lastDot < 1  || lastDot + 1 >= value.length) {
            return "";
        }
        const dot = addDot ? "." : "";
        return dot + value.substring(lastDot + 1, value.length);
    }

}
