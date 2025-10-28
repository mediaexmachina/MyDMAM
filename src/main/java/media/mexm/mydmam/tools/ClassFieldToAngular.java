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
 * Copyright (C) Media ex Machina 2025
 *
 */
package media.mexm.mydmam.tools;

import static media.mexm.mydmam.tools.DTORecordToAngularInterfaceConverter.getAngularFileName;

import java.util.Map;
import java.util.stream.Stream;

public record ClassFieldToAngular(String fieldName,
								  String angularType,
								  Map<String, String> importTypes,
								  boolean isEnum) {

	public Stream<String> makeImportLines() {
		return importTypes.entrySet().stream()
				.map(t -> "import { " + t.getKey() + " } from './"
						  + getAngularFileName(t.getKey(), t.getValue())
						  + "';");
	}

	public String makeDefinitionLine() {
		if (isEnum) {
			return "    " + fieldName + ",";
		}
		return "    " + fieldName + ": " + angularType + ";";
	}

}
