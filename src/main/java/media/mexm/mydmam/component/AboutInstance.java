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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Component
public class AboutInstance {

	private final String hostName;
	@Getter
	private final long pid;
	private final String instanceName;

	public AboutInstance(@Value("${mydmamConsts.instancename:}") final String instanceName) {
		try {
			hostName = InetAddress.getLocalHost().getHostName();
		} catch (final UnknownHostException e) {
			throw new IllegalStateException("Can't get hostname", e);
		}
		pid = ProcessHandle.current().pid();
		this.instanceName = instanceName;
	}

	public String getPendingActivityHostName() {
		if (instanceName == null || instanceName.isEmpty()) {
			return hostName;
		} else {
			return hostName + "#" + instanceName;
		}
	}

}
