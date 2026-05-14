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
export abstract class Utils {

    public static distinct<T>(list: Array<T>, compareFn?: (a: T, b: T) => number) {
        var items = [...new Set(list)].sort(compareFn);
        list.length = 0;
        list.push(...items);
        return list;
    }

}
