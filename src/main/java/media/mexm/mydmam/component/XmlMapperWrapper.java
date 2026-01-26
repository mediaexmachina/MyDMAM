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
package media.mexm.mydmam.component;

import static com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator.Feature.WRITE_XML_DECLARATION;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import lombok.Getter;

@Component
public class XmlMapperWrapper { // TODO test

	@Getter
	private final XmlMapper xmlMapper;

	public XmlMapperWrapper() {
		final var factory = new XmlFactory();
		factory.configure(WRITE_XML_DECLARATION, true);
		xmlMapper = new XmlMapper(factory);
		xmlMapper.findAndRegisterModules();
	}

}
