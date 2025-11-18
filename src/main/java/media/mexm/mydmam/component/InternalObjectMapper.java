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
package media.mexm.mydmam.component;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class InternalObjectMapper {

	public static final TypeReference<List<String>> TYPE_LIST_STRING = new TypeReference<>() {};

	@Autowired
	ObjectMapper objectMapper;

	public String writeValueAsString(final Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (final JsonProcessingException e) {
			throw new IllegalArgumentException("Can't convert value", e);
		}
	}

	public <T> T readValue(final String content, final Class<T> valueType) {
		try {
			return objectMapper.readValue(content, valueType);
		} catch (final JsonProcessingException e) {
			throw new IllegalArgumentException("Can't convert from json", e);
		}
	}

	public <T> T readValue(final String content, final TypeReference<T> valueType) {
		try {
			return objectMapper.readValue(content, valueType);
		} catch (final JsonProcessingException e) {
			throw new IllegalArgumentException("Can't convert from json", e);
		}
	}

}
