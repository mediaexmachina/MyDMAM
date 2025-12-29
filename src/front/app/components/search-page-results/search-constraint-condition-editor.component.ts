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

import { Component, input, output, signal } from '@angular/core';
import { SearchConstraintCondition } from '../../dto/search-constraint-condition.enum';

@Component({
    selector: 'app-search-constraint-condition-editor',
    imports: [],
    template: `
    @if(editMode()) {
        <button (click)="selectTo($event, SearchConstraintConditionEnum.IGNORE)">
            Switch to {{ conditionToString(SearchConstraintConditionEnum.IGNORE) }}
        </button>
        <button (click)="selectTo($event, SearchConstraintConditionEnum.MUST)">
            Switch to {{ conditionToString(SearchConstraintConditionEnum.MUST) }}
        </button>
        <button (click)="selectTo($event, SearchConstraintConditionEnum.MUST_NOT)">
            Switch to {{ conditionToString(SearchConstraintConditionEnum.MUST_NOT) }}
        </button>
    } @else {
        {{ conditionToString(condition()) }}
        <button (click)="switchToEditMode($event)">Change</button>
    }
  `,
    styles: ``,
})
export class SearchConstraintConditionEditorComponent {

    readonly SearchConstraintConditionEnum = SearchConstraintCondition;
    readonly labelStateIgnore = input.required<string>();
    readonly labelStateMust = input.required<string>();
    readonly labelStateMustNot = input.required<string>();
    readonly condition = input.required<SearchConstraintCondition>();
    readonly onChange = output<SearchConstraintCondition>();
    readonly editMode = signal(false);

    conditionToString(condition: SearchConstraintCondition): string {
        switch (condition) {
            case SearchConstraintCondition.IGNORE:
                return this.labelStateIgnore();
            case SearchConstraintCondition.MUST:
                return this.labelStateMust();
            case SearchConstraintCondition.MUST_NOT:
                return this.labelStateMustNot();
            default:
                return "(?)";
        }
    }

    switchToEditMode(e: Event): void {
        e.preventDefault();
        this.editMode.set(true);
    }

    selectTo(e: Event, newCondition: SearchConstraintCondition): void {
        e.preventDefault();
        this.editMode.set(false);
        this.onChange.emit(newCondition);
    }

}
