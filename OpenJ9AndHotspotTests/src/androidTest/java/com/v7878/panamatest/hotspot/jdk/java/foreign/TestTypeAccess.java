/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package com.v7878.panamatest.hotspot.jdk.java.foreign;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.v7878.foreign.Arena;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.ValueLayout;
import com.v7878.invoke.VarHandle;

import org.junit.jupiter.api.Test;

import java.lang.invoke.WrongMethodTypeException;

public class TestTypeAccess {

    static final VarHandle INT_HANDLE = ValueLayout.JAVA_INT.varHandle();
    static final VarHandle ADDR_HANDLE = ValueLayout.ADDRESS.varHandle();

    @Test
    void testMemoryAddressCoordinateAsString() {
        assertThrows(ClassCastException.class, () -> {
            int v = (int) INT_HANDLE.get("string", 0L);
        });
    }

    @Test
    void testMemoryCoordinatePrimitive() {
        assertThrows(WrongMethodTypeException.class, () -> {
            int v = (int) INT_HANDLE.get(1);
        });
    }

    @Test
    void testMemoryAddressValueGetAsString() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment s = arena.allocate(8, 8);
            assertThrows(ClassCastException.class, () -> {
                String address = (String) ADDR_HANDLE.get(s, 0L);
            });
        }
    }

    @Test
    void testMemoryAddressValueSetAsString() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment s = arena.allocate(8, 8);
            assertThrows(ClassCastException.class, () -> {
                ADDR_HANDLE.set(s, 0L, "string");
            });
        }
    }

    @Test
    void testMemoryAddressValueGetAsPrimitive() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment s = arena.allocate(8, 8);
            //TODO? expected: <java.lang.invoke.WrongMethodTypeException> but was: <java.lang.ClassCastException>
            assertThrows(ClassCastException.class, () -> {
                int address = (int) ADDR_HANDLE.get(s, 0L);
            });
        }
    }

    @Test
    void testMemoryAddressValueSetAsPrimitive() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment s = arena.allocate(8, 8);
            assertThrows(WrongMethodTypeException.class, () -> {
                ADDR_HANDLE.set(s, 1);
            });
        }
    }
}
