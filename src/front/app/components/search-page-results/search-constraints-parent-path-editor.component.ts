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

import { Component, computed, ElementRef, inject, input, output, signal, viewChild } from '@angular/core';
import { FileSystemService } from '../../services/file-system.service';

@Component({
    selector: 'app-search-constraints-parent-path-editor',
    imports: [],
    template: `
    @if(editMode()) {
        <input type="text" #pathselector [value]="parentPath()" (keyup)="onChangePath()" />
        <button (click)="switchOffEditMode($event)">Ok</button>
        <button (click)="removeConstraint($event)">Remove constraint</button>
    } @else {
        @if (parentPath() == "") {
            No parent path restricted.
        } @else {
            Only files on based on {{ parentPath() }} directory and sub-directories.
        }
        <button (click)="switchToEditMode($event)">Change</button>
    }
    `,
    styles: ``,
})
export class SearchConstraintsParentPathEditorComponent {

    readonly fileSystemService = inject(FileSystemService);
    readonly parentPath = input.required<string>();
    readonly onChange = output<string>();

    readonly editMode = signal(false);
    readonly pathselector = viewChild<ElementRef<HTMLInputElement>>('pathselector');

    switchToEditMode(e: Event): void {
        e.preventDefault();
        this.editMode.set(true);
    }

    switchOffEditMode(e: Event): void {
        e.preventDefault();
        this.onChangePath();
        this.editMode.set(false);
    }

    removeConstraint(e: PointerEvent) {
        e.preventDefault();
        this.editMode.set(false);
        this.onChange.emit("");
    }

    onChangePath() {
        let pathselectorValue = this.pathselector()?.nativeElement.value || "";
        if (pathselectorValue.startsWith("/") == false) {
            pathselectorValue = "/" + pathselectorValue;
        }
        this.onChange.emit(pathselectorValue);
    }

}
