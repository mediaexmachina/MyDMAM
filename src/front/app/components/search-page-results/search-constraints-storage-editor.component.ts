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

import { Component, input, output, signal, computed } from '@angular/core';

@Component({
    selector: 'app-search-constraints-storage-editor',
    imports: [],
    template: `
    @if(editMode()) {
        @for (storageName of addList(); track storageName) {
            <button (click)="addStorage($event, storageName)">
                Add {{ storageName }}
            </button>
        }

        @for (storageName of removeList(); track storageName) {
            <button (click)="removeStorage($event, storageName)">
                Remove {{ storageName }}
            </button>
        }

        <button (click)="switchOffEditMode($event)">Ok</button>
    } @else {
        @if (condition().length == 0) {
            No storages restricted.
        } @else if (condition().length == 1) {
            Limit search to {{ condition()[0] }} storage.
        } @else {
            Limit search to these storages: {{ condition().join(", ") }}.
        }

        <button (click)="switchToEditMode($event)">Change</button>
    }
    `,
    styles: ``,
})
export class SearchConstraintsStorageEditorComponent {

    readonly condition = input.required<Array<string>>();
    readonly storageList = input.required<Array<string>>();
    readonly onChange = output<Array<string>>();
    readonly editMode = signal(false);

    readonly addList = computed(() => {
        const storageList = this.storageList();
        const condition = this.condition();
        const result: Array<string> = [];

        for (let pos = 0; pos < storageList.length; pos++) {
            const storage = storageList[pos];
            if (condition.includes(storage) == false) {
                result.push(storage);
            }
        }
        return result;
    });

    readonly removeList = computed(() => {
        const storageList = this.storageList();
        const condition = this.condition();
        const result: Array<string> = [];

        for (let pos = 0; pos < storageList.length; pos++) {
            const storage = storageList[pos];
            if (condition.includes(storage)) {
                result.push(storage);
            }
        }
        return result;
    });

    switchToEditMode(e: Event): void {
        e.preventDefault();
        this.editMode.set(true);
    }

    switchOffEditMode(e: Event): void {
        e.preventDefault();
        this.editMode.set(false);
    }

    addStorage(e: Event, storageName: string): void {
        e.preventDefault();
        const newList = this.condition().slice();
        newList.push(storageName);
        this.onChange.emit(newList);
    }

    removeStorage(e: Event, storageName: string): void {
        e.preventDefault();
        const newList = this.condition().slice();
        const pos = newList.indexOf(storageName);
        if (pos < 0) {
            return;
        }
        newList.splice(pos, 1);
        this.onChange.emit(newList);
    }
}
