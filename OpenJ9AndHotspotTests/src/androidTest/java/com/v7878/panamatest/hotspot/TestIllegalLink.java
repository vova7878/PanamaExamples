/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.v7878.foreign.Arena;
import com.v7878.foreign.FunctionDescriptor;
import com.v7878.foreign.Linker;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.ValueLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(DataProviderRunner.class)
public class TestIllegalLink extends NativeTestHelper {

    private static final boolean IS_SYSV = true; // CABI.current() == CABI.SYS_V;
    private static final boolean IS_LE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private static final MemorySegment DUMMY_TARGET = MemorySegment.ofAddress(1);
    private static final MethodHandle DUMMY_TARGET_MH = NewApiUtils.empty(MethodType.methodType(void.class));
    private static final Linker ABI = Linker.nativeLinker();

    @Test
    @UseDataProvider("types")
    public void testIllegalLayouts(FunctionDescriptor desc, Linker.Option[] options, String expectedExceptionMessage) {
        try {
            ABI.downcallHandle(DUMMY_TARGET, desc, options);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException | ArithmeticException e) {
            assertTrue(e.getMessage() + " does not contain " + expectedExceptionMessage,
                    e.getMessage().contains(expectedExceptionMessage));
        }
    }

    @Test
    @UseDataProvider("downcallOnlyOptions")
    public void testIllegalUpcallOptions(Linker.Option downcallOnlyOption) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ABI.upcallStub(DUMMY_TARGET_MH, FunctionDescriptor.ofVoid(), Arena.ofAuto(), downcallOnlyOption);
        });
        assertTrue(exception.getMessage().matches(".*Not supported for upcall.*"));
    }

    @Test
    @UseDataProvider("illegalCaptureState")
    public void testIllegalCaptureState(String name) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Linker.Option.captureCallState(name);
        });
        assertTrue(exception.getMessage().matches(".*Unknown name.*"));
    }

    // where

    @DataProvider(format = "%m[%i]")
    public static Object[][] illegalCaptureState() {
        if (!IS_WINDOWS) {
            return new Object[][]{
                    {"GetLastError"},
                    {"WSAGetLastError"},
            };
        }
        return new Object[][]{};
    }

    @DataProvider(format = "%m[%i]")
    public static Object[][] downcallOnlyOptions() {
        return new Object[][]{
                {Linker.Option.firstVariadicArg(0)},
                {Linker.Option.captureCallState("errno")},
                {Linker.Option.critical(false)},
        };
    }

    @DataProvider(format = "%m[%i]")
    public static Object[][] types() {
        Linker.Option[] NO_OPTIONS = new Linker.Option[0];
        List<Object[]> cases = new ArrayList<>(Arrays.asList(new Object[][]{
                {
                        FunctionDescriptor.of(MemoryLayout.sequenceLayout(2, C_INT)),
                        NO_OPTIONS,
                        IS_LE ? "Unsupported layout: [2:i4]" : "Unsupported layout: [2:I4]"
                },
                {
                        FunctionDescriptor.ofVoid(MemoryLayout.sequenceLayout(2, C_INT)),
                        NO_OPTIONS,
                        IS_LE ? "Unsupported layout: [2:i4]" : "Unsupported layout: [2:I4]"
                },
                {
                        FunctionDescriptor.ofVoid(C_INT.withByteAlignment(2)),
                        NO_OPTIONS,
                        IS_LE ? "Unsupported layout: i4%2" : "Unsupported layout: I4%2"
                },
                {
                        FunctionDescriptor.ofVoid(C_POINTER.withByteAlignment(2)),
                        NO_OPTIONS,
                        "Unsupported layout: " + (IS_LE ? "a" : "A") + ADDRESS.byteSize() + "%2"
                },
                {
                        FunctionDescriptor.ofVoid(ValueLayout.JAVA_CHAR.withByteAlignment(4)),
                        NO_OPTIONS,
                        IS_LE ? "Unsupported layout: c2%4" : "Unsupported layout: C2%4"
                },
                {
                        FunctionDescriptor.ofVoid(MemoryLayout.structLayout(
                                C_CHAR.withName("x").withByteAlignment(1),
                                C_SHORT.withName("y").withByteAlignment(1),
                                C_INT.withName("z").withByteAlignment(1)
                        ).withByteAlignment(1)),
                        NO_OPTIONS,
                        IS_LE ? "Unsupported layout: s2%1" : "Unsupported layout: S2%1"
                },
                {
                        FunctionDescriptor.ofVoid(MemoryLayout.structLayout(
                                MemoryLayout.structLayout(
                                        C_CHAR.withName("x").withByteAlignment(1),
                                        C_SHORT.withName("y").withByteAlignment(1),
                                        C_INT.withName("z").withByteAlignment(1)
                                ))),
                        NO_OPTIONS,
                        IS_LE ? "Unsupported layout: s2%1" : "Unsupported layout: S2%1"
                },
                {
                        FunctionDescriptor.ofVoid(MemoryLayout.structLayout(
                                MemoryLayout.sequenceLayout(1,
                                        C_INT.withByteAlignment(1)
                                ))),
                        NO_OPTIONS,
                        IS_LE ? "Unsupported layout: i4%1" : "Unsupported layout: I4%1"
                },
                {
                        FunctionDescriptor.ofVoid(MemoryLayout.structLayout(
                                ValueLayout.JAVA_INT,
                                MemoryLayout.paddingLayout(4), // no excess padding
                                ValueLayout.JAVA_INT)),
                        NO_OPTIONS,
                        "unexpected offset"
                },
                {
                        FunctionDescriptor.of(C_INT.withOrder(nonNativeOrder())),
                        NO_OPTIONS,
                        IS_LE ? "Unsupported layout: I4" : "Unsupported layout: i4"
                },
                {
                        FunctionDescriptor.of(MemoryLayout.structLayout(C_INT.withOrder(nonNativeOrder()))),
                        NO_OPTIONS,
                        IS_LE ? "Unsupported layout: I4" : "Unsupported layout: i4"
                },
                {
                        FunctionDescriptor.of(MemoryLayout.structLayout(MemoryLayout.sequenceLayout(1, C_INT.withOrder(nonNativeOrder())))),
                        NO_OPTIONS,
                        IS_LE ? "Unsupported layout: I4" : "Unsupported layout: i4"
                },
                {
                        FunctionDescriptor.ofVoid(MemoryLayout.structLayout(
                                ValueLayout.JAVA_INT,
                                MemoryLayout.paddingLayout(4))), // too much trailing padding
                        NO_OPTIONS,
                        "has unexpected size"
                },
        }));

        for (ValueLayout illegalLayout : List.of(C_CHAR, ValueLayout.JAVA_CHAR, C_BOOL, C_SHORT, C_FLOAT)) {
            cases.add(new Object[]{
                    FunctionDescriptor.ofVoid(C_INT, illegalLayout),
                    new Linker.Option[]{Linker.Option.firstVariadicArg(1)},
                    "Invalid variadic argument layout"
            });
        }

        if (IS_SYSV) {
            cases.add(new Object[]{
                    FunctionDescriptor.ofVoid(MemoryLayout.structLayout(
                            MemoryLayout.sequenceLayout(Long.MAX_VALUE / C_INT.byteSize(),
                                    C_INT
                            ))),
                    NO_OPTIONS,
                    // Port-changed
                    //"GroupLayout is too large"
                    "integer overflow"
            });
        }
        if (C_LONG_LONG.byteAlignment() == 8) {
            cases.add(new Object[]{
                    FunctionDescriptor.ofVoid(MemoryLayout.structLayout(
                            C_LONG_LONG, ValueLayout.JAVA_INT)), // missing trailing padding
                    NO_OPTIONS,
                    "has unexpected size"
            });
        }
        return cases.toArray(new Object[0][]);
    }

    private static ByteOrder nonNativeOrder() {
        return ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN
                ? ByteOrder.BIG_ENDIAN
                : ByteOrder.LITTLE_ENDIAN;
    }
}
