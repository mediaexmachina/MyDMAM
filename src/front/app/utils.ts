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

    public static msToHMS(durationMs: number):string {
        const sign = durationMs < 0 ? "-" : "";
        const durationSecond = Math.abs(durationMs) / 1000;
        const secondOnlyValue = Math.floor(durationSecond);
        const msecondOnlyValue = Math.round((durationSecond - secondOnlyValue) * 1000);

        if (secondOnlyValue == 0) {
            return sign + msecondOnlyValue + " ms";
        }

        const msecond = msecondOnlyValue == 0 ? "" : " " + msecondOnlyValue + " ms";
        if (secondOnlyValue < 60) {
            return sign + secondOnlyValue + " sec" + msecond;
        }

        const durationMinute = secondOnlyValue / 60;
        const minuteOnlyValue = Math.floor(durationMinute);
        const lastSeconds = Math.round((durationMinute - minuteOnlyValue) * 60);

        if (durationMinute < 60) {
            return sign + minuteOnlyValue + " min " + lastSeconds + " sec" + msecond;
        }

        const durationHour = minuteOnlyValue / 60;
        const hourOnlyValue = Math.floor(durationHour);
        const lastMinutes = Math.round((durationHour - hourOnlyValue) * 60);

        return durationMs + "ms >> " + sign + hourOnlyValue + " hr " + lastMinutes + " min " + lastSeconds + " sec" + msecond;
    }

    public static bpsToEngNotation(valueBps: number):string {
        if (valueBps < 1000) {
            return valueBps + " bits/sec";
        } else if (valueBps < 1_000_000) {
            return Math.round(valueBps / 1000) + " kbits/sec";
        } else if (valueBps < 1_000_000_000) {
            return Math.round(valueBps / 1_000_000) + " Mbits/sec";
        }
        return Math.round(valueBps / 1_000_000_000) + " Gbits/sec";
    }
}
