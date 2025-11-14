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
package media.mexm.mydmam.audittrail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public record AuditTrailBatchInsertObject(AuditTrailObjectType objectType,
										  String objectReference,
										  Object objectPayload) {

	AuditTrailItem makeAuditTrailItem(final ObjectMapper objectMapper) {
		try {
			return new AuditTrailItem(
					objectType,
					objectReference,
					objectMapper.writeValueAsString(objectPayload));
		} catch (final JsonProcessingException e) {
			throw new IllegalArgumentException("Can't convert json " + objectType, e);
		}
	}

}
