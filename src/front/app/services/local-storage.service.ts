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

import { Injectable, WritableSignal, effect, signal } from '@angular/core';
import { DateTimeStyleToDisplay } from '../enums/date-time-style-to-display.enum';

@Injectable({
    providedIn: 'root'
})
export class LocalStorageService {

    readonly dateStyleToDisplay: WritableSignal<DateTimeStyleToDisplay>;

    constructor() {
        this.dateStyleToDisplay = signal(this.getByDefaultNumber("dateStyleToDisplay", DateTimeStyleToDisplay.SIMPLIFIED_VIEW));
        effect(() => {
            this.setNumber("dateStyleToDisplay", this.dateStyleToDisplay());
        });
    }

    public getSelectedRealm(): string {
        var r = localStorage.getItem("realm");
        if (r == null) {
            throw new Error("No realm set");
        }
        return  r;
    }

    public setSelectedRealm(value: string | null = null): void {
        if (value == null) {
            localStorage.removeItem("realm");
        } else {
            localStorage.setItem("realm", value);
        }
    }

    private getByDefault(key: string, defaultValue: string): string {
        var r = localStorage.getItem(key);
        if (r == null) {
            localStorage.setItem(key, defaultValue);
            return defaultValue;
        }
        return r;
    }

    private getByDefaultNumber(key: string, defaultValue: number): number {
        return Number.parseInt(this.getByDefault(key, String(defaultValue)));
    }

    private setNumber(key: string, value: number): void {
        localStorage.setItem(key, String(value));
    }

    public getNavigateListResultCount(defaultValue:number): number {
        return this.getByDefaultNumber("navigateListResultCount", defaultValue);
    }

    public setNavigateListResultCount(value: number): void {
        this.setNumber("navigateListResultCount", Math.abs(value));
    }

}
