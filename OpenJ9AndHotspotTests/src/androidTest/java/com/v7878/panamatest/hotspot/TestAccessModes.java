/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.v7878.panamatest.hotspot;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.v7878.foreign.AddressLayout;
import com.v7878.foreign.Arena;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.ValueLayout;
import com.v7878.invoke.VarHandle;
import com.v7878.invoke.VarHandle.AccessMode;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Ignore("LONG TEST GROUP")
@RunWith(DataProviderRunner.class)
public class TestAccessModes {

    @Test
    @UseDataProvider("segmentsAndLayoutsAndModes")
    public void testAccessModes(MemorySegment segment, MemoryLayout layout, AccessMode mode) throws Throwable {
        VarHandle varHandle = layout instanceof ValueLayout ?
                layout.varHandle() :
                layout.varHandle(MemoryLayout.PathElement.groupElement(0));
        MethodHandle methodHandle = varHandle.toMethodHandle(mode);
        boolean compatible = AccessModeKind.supportedModes(accessLayout(layout)).contains(AccessModeKind.of(mode));
        try {
            Object o = methodHandle.invokeWithArguments(makeArgs(segment, varHandle.accessModeType(mode)));
            assertTrue(compatible);
        } catch (UnsupportedOperationException ex) {
            assertFalse(compatible);
        } catch (IllegalArgumentException ex) {
            // access is unaligned
            assertTrue(segment.maxByteAlignment() < layout.byteAlignment());
        }
    }

    static ValueLayout accessLayout(MemoryLayout layout) {
        Objects.requireNonNull(layout);
        if (layout instanceof ValueLayout vl) {
            return vl;
        } else if (layout instanceof GroupLayout gl) {
            return accessLayout(gl.memberLayouts().get(0));
        }
        throw new IllegalStateException();
    }

    Object[] makeArgs(MemorySegment segment, MethodType type) throws Throwable {
        List<Object> args = new ArrayList<>();
        args.add(segment);
        for (Class argType : type.dropParameterTypes(0, 1).parameterList()) {
            args.add(defaultValue(argType));
        }
        return args.toArray();
    }

    Object defaultValue(Class<?> clazz) throws Throwable {
        if (clazz == MemorySegment.class) {
            return MemorySegment.NULL;
        } else if (clazz.isPrimitive()) {
            return NewApiUtils.zero(clazz).invoke();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /*
     * See the javadoc of MemoryLayout::varHandle.
     */
    enum AccessModeKind {
        PLAIN,
        READ_WRITE,
        ATOMIC_UPDATE,
        ATOMIC_NUMERIC_UPDATE,
        ATOMIC_BITWISE_UPDATE;

        static AccessModeKind of(AccessMode mode) {
            return switch (mode) {
                case GET, SET -> PLAIN;
                case GET_ACQUIRE, GET_OPAQUE, GET_VOLATILE, SET_VOLATILE,
                     SET_OPAQUE, SET_RELEASE -> READ_WRITE;
                case GET_AND_SET, GET_AND_SET_ACQUIRE, GET_AND_SET_RELEASE,
                     WEAK_COMPARE_AND_SET, WEAK_COMPARE_AND_SET_RELEASE,
                     WEAK_COMPARE_AND_SET_ACQUIRE, WEAK_COMPARE_AND_SET_PLAIN,
                     COMPARE_AND_EXCHANGE, COMPARE_AND_EXCHANGE_ACQUIRE,
                     COMPARE_AND_EXCHANGE_RELEASE, COMPARE_AND_SET -> ATOMIC_UPDATE;
                case GET_AND_ADD, GET_AND_ADD_ACQUIRE, GET_AND_ADD_RELEASE -> ATOMIC_NUMERIC_UPDATE;
                default -> ATOMIC_BITWISE_UPDATE;
            };
        }

        static Set<AccessModeKind> supportedModes(ValueLayout layout) {
            Set<AccessModeKind> supportedModes = EnumSet.noneOf(AccessModeKind.class);
            supportedModes.add(PLAIN);
            if (layout.byteAlignment() >= layout.byteSize()) {
                supportedModes.add(READ_WRITE);
                // Port-changed
                /*if (layout instanceof ValueLayout.OfInt || layout instanceof ValueLayout.OfLong ||
                        layout instanceof ValueLayout.OfFloat || layout instanceof ValueLayout.OfDouble ||
                        layout instanceof AddressLayout) {
                    supportedModes.add(ATOMIC_UPDATE);
                }
                if (layout instanceof ValueLayout.OfInt || layout instanceof ValueLayout.OfLong ||
                        layout instanceof AddressLayout) {
                    supportedModes.add(ATOMIC_NUMERIC_UPDATE);
                    supportedModes.add(ATOMIC_BITWISE_UPDATE);
                }*/
                if (layout instanceof ValueLayout.OfByte || layout instanceof ValueLayout.OfBoolean ||
                        layout instanceof ValueLayout.OfChar || layout instanceof ValueLayout.OfShort ||
                        layout instanceof ValueLayout.OfInt || layout instanceof ValueLayout.OfLong ||
                        layout instanceof ValueLayout.OfFloat || layout instanceof ValueLayout.OfDouble ||
                        layout instanceof AddressLayout) {
                    supportedModes.add(ATOMIC_UPDATE);
                }
                if (layout instanceof ValueLayout.OfByte ||
                        layout instanceof ValueLayout.OfChar || layout instanceof ValueLayout.OfShort ||
                        layout instanceof ValueLayout.OfInt || layout instanceof ValueLayout.OfLong ||
                        layout instanceof ValueLayout.OfFloat || layout instanceof ValueLayout.OfDouble ||
                        layout instanceof AddressLayout) {
                    supportedModes.add(ATOMIC_NUMERIC_UPDATE);
                }
                if (layout instanceof ValueLayout.OfByte || layout instanceof ValueLayout.OfBoolean ||
                        layout instanceof ValueLayout.OfChar || layout instanceof ValueLayout.OfShort ||
                        layout instanceof ValueLayout.OfInt || layout instanceof ValueLayout.OfLong ||
                        layout instanceof AddressLayout) {
                    supportedModes.add(ATOMIC_BITWISE_UPDATE);
                }
            }
            return supportedModes;
        }
    }

    static MemoryLayout[] layouts() {
        MemoryLayout[] valueLayouts = {
                ValueLayout.JAVA_BOOLEAN,
                ValueLayout.JAVA_CHAR,
                ValueLayout.JAVA_BYTE,
                ValueLayout.JAVA_SHORT,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_FLOAT,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_DOUBLE,
                ValueLayout.ADDRESS
        };
        List<MemoryLayout> layouts = new ArrayList<>();
        for (MemoryLayout layout : valueLayouts) {
            for (int align : new int[]{1, 2, 4, 8}) {
                layouts.add(layout.withByteAlignment(align));
                layouts.add(MemoryLayout.structLayout(layout.withByteAlignment(align)));
            }
        }
        return layouts.toArray(new MemoryLayout[0]);
    }

    static MemorySegment[] segments() {
        return new MemorySegment[]{
                Arena.ofAuto().allocate(8),
                MemorySegment.ofArray(new byte[8]),
                MemorySegment.ofArray(new char[4]),
                MemorySegment.ofArray(new short[4]),
                MemorySegment.ofArray(new int[2]),
                MemorySegment.ofArray(new float[2]),
                MemorySegment.ofArray(new long[1]),
                MemorySegment.ofArray(new double[1])
        };
    }

    @DataProvider
    public static Object[][] segmentsAndLayoutsAndModes() {
        List<Object[]> segmentsAndLayouts = new ArrayList<>();
        for (MemorySegment segment : segments()) {
            for (MemoryLayout layout : layouts()) {
                for (AccessMode mode : AccessMode.values()) {
                    segmentsAndLayouts.add(new Object[]{segment, layout, mode});
                }
            }
        }
        return segmentsAndLayouts.toArray(new Object[0][]);
    }
}
