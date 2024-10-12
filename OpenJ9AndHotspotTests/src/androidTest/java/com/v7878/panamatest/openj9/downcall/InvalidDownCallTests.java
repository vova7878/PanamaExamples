/*
 * Copyright IBM Corp. and others 2023
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which accompanies this
 * distribution and is available at https://www.eclipse.org/legal/epl-2.0/
 * or the Apache License, Version 2.0 which accompanies this distribution and
 * is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * This Source Code may also be made available under the following
 * Secondary Licenses when the conditions for such availability set
 * forth in the Eclipse Public License, v. 2.0 are satisfied: GNU
 * General Public License, version 2 with the GNU Classpath
 * Exception [1] and GNU General Public License, version 2 with the
 * OpenJDK Assembly Exception [2].
 *
 * [1] https://www.gnu.org/software/classpath/license.html
 * [2] https://openjdk.org/legal/assembly-exception.html
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0 OR GPL-2.0-only WITH Classpath-exception-2.0 OR GPL-2.0-only WITH OpenJDK-assembly-exception-1.0
 */
package com.v7878.panamatest.openj9.downcall;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.panamatest.openj9.Shared.C_LONG_LONG;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.v7878.foreign.Arena;
import com.v7878.foreign.FunctionDescriptor;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.Linker;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemoryLayout.PathElement;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SegmentAllocator;
import com.v7878.foreign.SymbolLookup;
import com.v7878.invoke.VarHandle;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;

/**
 * Test cases for JEP 454: Foreign Linker API for primitive types in downcall,
 * which verify the illegal cases including unsupported layouts, etc.
 * Note: the majority of illegal cases are removed given the corresponding method type
 * is deduced from the function descriptor which is verified in OpenJDK.
 */
public class InvalidDownCallTests {
    private static Linker linker = Linker.nativeLinker();

    static {
        System.loadLibrary("clinkerffitests");
    }

    private static final SymbolLookup nativeLibLookup = SymbolLookup.loaderLookup();
    private static final SymbolLookup defaultLibLookup = linker.defaultLookup();

    @Test
    public void test_invalidMemoryLayoutForIntType() throws Throwable {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            FunctionDescriptor fd = FunctionDescriptor.ofVoid(JAVA_INT, MemoryLayout.paddingLayout(JAVA_INT.byteSize()));
            MemorySegment functionSymbol = nativeLibLookup.find("add2IntsReturnVoid").get();
            MethodHandle mh = linker.downcallHandle(functionSymbol, fd);
            fail("Failed to throw IllegalArgumentException in the case of the invalid MemoryLayout");
        });
        assertTrue(exception.getMessage().matches("Unsupported padding layout.*"));
    }

    @Test
    public void test_invalidMemoryLayoutForMemAddr() throws Throwable {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MemorySegment functionSymbol = defaultLibLookup.find("strlen").get();
            FunctionDescriptor fd = FunctionDescriptor.of(C_LONG_LONG, MemoryLayout.paddingLayout(C_LONG_LONG.byteSize()));
            MethodHandle mh = linker.downcallHandle(functionSymbol, fd);
            fail("Failed to throw IllegalArgumentException in the case of the invalid MemoryLayout");
        });
        assertTrue(exception.getMessage().matches("Unsupported padding layout.*"));
    }

    @Test
    public void test_invalidMemoryLayoutForReturnType() throws Throwable {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            MemorySegment functionSymbol = defaultLibLookup.find("strlen").get();
            FunctionDescriptor fd = FunctionDescriptor.of(MemoryLayout.paddingLayout(C_LONG_LONG.byteSize()), C_LONG_LONG);
            MethodHandle mh = linker.downcallHandle(functionSymbol, fd);
            fail("Failed to throw IllegalArgumentException in the case of the invalid MemoryLayout");
        });
        assertTrue(exception.getMessage().matches("Unsupported padding layout.*"));
    }

    @Test
    public void test_nullValueForPtrArgument() throws Throwable {
        assertThrows(NullPointerException.class, () -> {
            FunctionDescriptor fd = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS);
            MemorySegment functionSymbol = nativeLibLookup.find("addIntAndIntsFromStructPointer").get();
            MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

            int result = (int) mh.invoke(19202122, null);
            fail("Failed to throw NullPointerException in the case of the null value");
        });
    }

    @Test
    public void test_nullValueForStructArgument() throws Throwable {
        assertThrows(WrongMethodTypeException.class, () -> {
            GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
            VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
            VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

            FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout);
            MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStruct").get();
            MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

            try (Arena arena = Arena.ofConfined()) {
                MemorySegment structSegmt1 = arena.allocate(structLayout);
                intHandle1.set(structSegmt1, 0L, 11223344);
                intHandle2.set(structSegmt1, 0L, 55667788);

                MemorySegment resultSegmt = (MemorySegment) mh.invokeExact((SegmentAllocator) arena, structSegmt1, null);
                fail("Failed to throw WrongMethodTypeException in the case of the null value");
            }
        });
    }

    @Test
    public void test_nullSegmentForPtrArgument() throws Throwable {
        FunctionDescriptor fd = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("validateNullAddrArgument").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        int result = (int) mh.invoke(19202122, MemorySegment.NULL);
        Assert.assertEquals(result, 19202122);
    }

    //TODO?
    @Ignore("Hotspot (and my) implementation fails here with Segfault")
    @Test
    public void test_nullSegmentForStructArgument() throws Throwable {
        assertThrows(NullPointerException.class, () -> {
            GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
            VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
            VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

            FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout);
            MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStruct").get();
            MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

            try (Arena arena = Arena.ofConfined()) {
                MemorySegment structSegmt1 = arena.allocate(structLayout);
                intHandle1.set(structSegmt1, 0L, 11223344);
                intHandle2.set(structSegmt1, 0L, 55667788);

                MemorySegment nullSegmt = MemorySegment.NULL.reinterpret(structLayout.byteSize());
                MemorySegment resultSegmt = (MemorySegment) mh.invokeExact((SegmentAllocator) arena, structSegmt1, nullSegmt);
                fail("Failed to throw NullPointerException in the case of the null segment");
            }
        });
    }

    @Test
    public void test_heapSegmentForStructArgument() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
        VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
        VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout);
        MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStruct").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment structSegmt1 = arena.allocate(structLayout);
            intHandle1.set(structSegmt1, 0L, 11223344);
            intHandle2.set(structSegmt1, 0L, 55667788);
            MemorySegment structSegmt2 = MemorySegment.ofArray(new int[]{99001122, 33445566});

            MemorySegment resultSegmt = (MemorySegment) mh.invokeExact((SegmentAllocator) arena, structSegmt1, structSegmt2);
            Assert.assertEquals(intHandle1.get(resultSegmt, 0L), 110224466);
            Assert.assertEquals(intHandle2.get(resultSegmt, 0L), 89113354);
        }
    }

    @Test
    public void test_disallowedHeapSegmentForPtrArgument() throws Throwable {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            FunctionDescriptor fd = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS);
            MemorySegment functionSymbol = nativeLibLookup.find("addIntAndIntsFromStructPointer").get();
            MethodHandle mh = linker.downcallHandle(functionSymbol, fd, Linker.Option.critical(false));

            MemorySegment structSegmt = MemorySegment.ofArray(new int[]{11121314, 15161718});
            int result = (int) mh.invoke(19202122, structSegmt);
            fail("Failed to throw IllegalArgumentException when the heap segment is disallowed in native");
        });
        assertTrue(exception.getMessage().matches("Heap segment not allowed.*"));
    }
}
