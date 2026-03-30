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
package media.mexm.mydmam.activity;

import static media.mexm.mydmam.dto.StorageStateClass.ONLINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import media.mexm.mydmam.entity.FileEntity;
import media.mexm.mydmam.pathindexing.RealmStorageConfiguredEnv;

class ActivityHandlerTest {

    class Impl implements ActivityHandler {

        @Override
        public boolean canHandle(final FileEntity file,
                                 final ActivityEventType eventType,
                                 final RealmStorageConfiguredEnv storedOn) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void handle(final FileEntity file,
                           final ActivityEventType eventType,
                           final RealmStorageConfiguredEnv storedOn) throws Exception {
            throw new UnsupportedOperationException();
        }
    }

    Impl ah;

    @BeforeEach
    void init() {
        ah = new Impl();
    }

    @Test
    void testIsEnabled() {
        assertTrue(ah.isEnabled());
    }

    @Test
    void testGetHandlerName() {
        assertThat(ah.getHandlerName()).isEqualTo(Impl.class.getSimpleName());
    }

    @Test
    void testGetSupportedStorageStateClasses() {
        assertThat(ah.getSupportedStorageStateClasses())
                .isEqualTo(Set.of(ONLINE));
    }

}
