/*
 * This file is part of mydmam.
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
package media.mexm.mydmam.mtdthesaurus;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryNumericalUnit.NO_UNIT;
import static media.mexm.mydmam.mtdthesaurus.MetadataThesaurusEntryType.DISPLAYED_AS_IT;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface MetadataThesaurusEntryAttribute {

    public String longname() default "";

    public MetadataThesaurusEntryType type() default DISPLAYED_AS_IT;

    public MetadataThesaurusEntryNumericalUnit unit() default NO_UNIT;

}
/*
TODO add @
                                           |Front Displayed|
Entry:
 - long name                               |               |
 - Type=Identifier                         |               |
 - Type=UnixTimeMsec                       |               |
 - Type=Boolean                            |               |
 - NumericalUnit                           |               |
 - MetadataThesaurusSortIndexOrder         |               |

TODO check UNIX_TIME_MSEC and BOOLEAN during write
 * */
