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

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Startup implements InitializingBean, DisposableBean {

    @Autowired
    List<InternalService> internalServices;

    @Override
    public void afterPropertiesSet() throws Exception {
        for (final var service : internalServices) {
            log.info("Start internal service {}", service.getInternalServiceName());
            service.internalServiceStart();
        }

    }

    @Override
    public void destroy() throws Exception {
        for (final var service : internalServices) {
            log.debug("Stop internal service {}", service.getInternalServiceName());
            service.internalServiceStop();
        }
    }

}
