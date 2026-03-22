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
package media.mexm.mydmam.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import tv.hd3g.commons.testtools.Fake;
import tv.hd3g.commons.testtools.MockToolsExtendsJunit;

@ExtendWith(MockToolsExtendsJunit.class)
class ManagedMimeTraitTest {

    record Impl(Set<String> managedMimeTypes) implements ManagedMimeTrait {

        @Override
        public Set<String> getManagedMimeTypes() {
            return managedMimeTypes;
        }

    }

    @Mock
    MediaAsset asset;
    @Fake
    String mimeType;

    Impl impl;

    @BeforeEach
    void init() {
        impl = new Impl(Set.of(mimeType));
    }

    @Test
    final void testCanHandleMimeType() {
        when(asset.getMimeType()).thenReturn(Optional.ofNullable(mimeType));
        assertThat(impl.canHandleMimeType(asset)).isTrue();

        when(asset.getMimeType()).thenReturn(Optional.ofNullable("other"));
        assertThat(impl.canHandleMimeType(asset)).isFalse();

        verify(asset, times(2)).getMimeType();
    }
}
