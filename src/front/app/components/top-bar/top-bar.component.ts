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

import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SearchBarComponent } from "../search-bar/search-bar.component";

@Component({
  selector: 'app-top-bar',
  imports: [RouterLink, SearchBarComponent],
  templateUrl: './top-bar.component.html',
  styleUrl: './top-bar.component.css',
})
export class TopBarComponent {

    title = input.required<String>();
    realm = input.required<String>();

    getTopMenuSelectedClassName(expected:string):string {
        return window.location.pathname.startsWith(expected) ? 'currentpage' : '';
    }

}
