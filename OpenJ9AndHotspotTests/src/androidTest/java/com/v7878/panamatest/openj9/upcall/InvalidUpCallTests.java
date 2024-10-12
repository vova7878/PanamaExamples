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
package com.v7878.panamatest.openj9.upcall;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
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

/**
 * Test cases for JEP 454: Foreign Linker API in upcall,
 * which verify the illegal cases including the returned segment, etc.
 */
public class InvalidUpCallTests {
    private static Linker linker = Linker.nativeLinker();

    static {
        System.loadLibrary("clinkerffitests");
    }

    private static final SymbolLookup nativeLibLookup = SymbolLookup.loaderLookup();

    @Test
    @Ignore
    public void test_throwExceptionFromUpcallMethod() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
        VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
        VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStructByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_add2IntStructs_returnStruct_throwException,
                    FunctionDescriptor.of(structLayout, structLayout, structLayout), arena);
            MemorySegment structSegmt1 = arena.allocate(structLayout);
            intHandle1.set(structSegmt1, 0L, 11223344);
            intHandle2.set(structSegmt1, 0L, 55667788);
            MemorySegment structSegmt2 = arena.allocate(structLayout);
            intHandle1.set(structSegmt2, 0L, 99001122);
            intHandle2.set(structSegmt2, 0L, 33445566);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                MemorySegment resultSegmt = (MemorySegment) mh.invoke((SegmentAllocator) arena, structSegmt1, structSegmt2, upcallFuncAddr);
                fail("Failed to throw IllegalArgumentException from the the upcall method");
            });
            assertTrue(exception.getMessage().matches("An exception is thrown from the upcall method"));
        }
    }

    @Test
    @Ignore
    public void test_nestedUpcall_throwExceptionFromUpcallMethod() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
        VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
        VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStructByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_add2IntStructs_returnStruct_nestedUpcall,
                    FunctionDescriptor.of(structLayout, structLayout, structLayout), arena);
            MemorySegment structSegmt1 = arena.allocate(structLayout);
            intHandle1.set(structSegmt1, 0L, 11223344);
            intHandle2.set(structSegmt1, 0L, 55667788);
            MemorySegment structSegmt2 = arena.allocate(structLayout);
            intHandle1.set(structSegmt2, 0L, 99001122);
            intHandle2.set(structSegmt2, 0L, 33445566);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                MemorySegment resultSegmt = (MemorySegment) mh.invoke((SegmentAllocator) arena, structSegmt1, structSegmt2, upcallFuncAddr);
                fail("Failed to throw IllegalArgumentException from the nested upcall");
            });
            assertTrue(exception.getMessage().matches("An exception is thrown from the upcall method"));
        }
    }

    @Test
    @Ignore
    public void test_nullValueForReturnPtr() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
        VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
        VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(ADDRESS, ADDRESS, structLayout, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStructPointerByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_add2IntStructs_returnStructPointer_nullValue,
                    FunctionDescriptor.of(ADDRESS, ADDRESS, structLayout), arena);
            MemorySegment structSegmt1 = arena.allocate(structLayout);
            intHandle1.set(structSegmt1, 0L, 11223344);
            intHandle2.set(structSegmt1, 0L, 55667788);
            MemorySegment structSegmt2 = arena.allocate(structLayout);
            intHandle1.set(structSegmt2, 0L, 99001122);
            intHandle2.set(structSegmt2, 0L, 33445566);

            assertThrows(NullPointerException.class, () -> {
                MemorySegment resultAddr = (MemorySegment) mh.invoke(structSegmt1, structSegmt2, upcallFuncAddr);
                fail("Failed to throw NullPointerException in the case of the null value upon return");
            });
        }
    }

    @Test
    @Ignore
    public void test_nullValueForReturnStruct() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
        VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
        VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStructByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_add2IntStructs_returnStruct_nullValue,
                    FunctionDescriptor.of(structLayout, structLayout, structLayout), arena);
            MemorySegment structSegmt1 = arena.allocate(structLayout);
            intHandle1.set(structSegmt1, 0L, 11223344);
            intHandle2.set(structSegmt1, 0L, 55667788);
            MemorySegment structSegmt2 = arena.allocate(structLayout);
            intHandle1.set(structSegmt2, 0L, 99001122);
            intHandle2.set(structSegmt2, 0L, 33445566);

            assertThrows(NullPointerException.class, () -> {
                MemorySegment resultSegmt = (MemorySegment) mh.invoke((SegmentAllocator) arena, structSegmt1, structSegmt2, upcallFuncAddr);
                fail("Failed to throw NullPointerException in the case of the null value upon return");
            });
        }
    }

    @Test
    public void test_nullSegmentForReturnPtr() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
        VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
        VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(ADDRESS, ADDRESS, structLayout, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("validateReturnNullAddrByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_add2IntStructs_returnStructPointer_nullSegmt,
                    FunctionDescriptor.of(ADDRESS, ADDRESS, structLayout), arena);
            MemorySegment structSegmt1 = arena.allocate(structLayout);
            intHandle1.set(structSegmt1, 0L, 11223344);
            intHandle2.set(structSegmt1, 0L, 55667788);
            MemorySegment structSegmt2 = arena.allocate(structLayout);
            intHandle1.set(structSegmt2, 0L, 99001122);
            intHandle2.set(structSegmt2, 0L, 33445566);

            MemorySegment resultAddr = (MemorySegment) mh.invoke(structSegmt1, structSegmt2, upcallFuncAddr);
            MemorySegment resultSegmt = resultAddr.reinterpret(structLayout.byteSize());
            Assert.assertEquals(resultSegmt.get(JAVA_INT, 0), 11223344);
            Assert.assertEquals(resultSegmt.get(JAVA_INT, 4), 55667788);
        }
    }

    @Test
    @Ignore
    public void test_nullSegmentForReturnStruct() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
        VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
        VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStructByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_add2IntStructs_returnStruct_nullSegmt,
                    FunctionDescriptor.of(structLayout, structLayout, structLayout), arena);
            MemorySegment structSegmt1 = arena.allocate(structLayout);
            intHandle1.set(structSegmt1, 0L, 11223344);
            intHandle2.set(structSegmt1, 0L, 55667788);
            MemorySegment structSegmt2 = arena.allocate(structLayout);
            intHandle1.set(structSegmt2, 0L, 99001122);
            intHandle2.set(structSegmt2, 0L, 33445566);

            assertThrows(NullPointerException.class, () -> {
                MemorySegment resultSegmt = (MemorySegment) mh.invoke((SegmentAllocator) arena, structSegmt1, structSegmt2, upcallFuncAddr);
                fail("Failed to throw NullPointerException in the case of the null segment upon return");
            });
        }
    }

    @Test
    @Ignore
    public void test_heapSegmentForReturnPtr() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
        VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
        VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(ADDRESS, ADDRESS, structLayout, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStructPointerByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_add2IntStructs_returnStructPointer_heapSegmt,
                    FunctionDescriptor.of(ADDRESS, ADDRESS, structLayout), arena);
            MemorySegment structSegmt1 = arena.allocate(structLayout);
            intHandle1.set(structSegmt1, 0L, 11223344);
            intHandle2.set(structSegmt1, 0L, 55667788);
            MemorySegment structSegmt2 = arena.allocate(structLayout);
            intHandle1.set(structSegmt2, 0L, 99001122);
            intHandle2.set(structSegmt2, 0L, 33445566);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                MemorySegment resultAddr = (MemorySegment) mh.invoke(structSegmt1, structSegmt2, upcallFuncAddr);
                fail("Failed to throw IllegalArgumentException in the case of the heap segment upon return");
            });
            assertTrue(exception.getMessage().matches("Heap segment not allowed.*"));
        }
    }

    @Test
    public void test_heapSegmentForReturnStruct() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));
        VarHandle intHandle1 = structLayout.varHandle(PathElement.groupElement("elem1"));
        VarHandle intHandle2 = structLayout.varHandle(PathElement.groupElement("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStructByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_add2IntStructs_returnStruct_heapSegmt,
                    FunctionDescriptor.of(structLayout, structLayout, structLayout), arena);
            MemorySegment structSegmt1 = arena.allocate(structLayout);
            intHandle1.set(structSegmt1, 0L, 11223344);
            intHandle2.set(structSegmt1, 0L, 55667788);
            MemorySegment structSegmt2 = arena.allocate(structLayout);
            intHandle1.set(structSegmt2, 0L, 99001122);
            intHandle2.set(structSegmt2, 0L, 33445566);

            MemorySegment resultSegmt = (MemorySegment) mh.invoke((SegmentAllocator) arena, structSegmt1, structSegmt2, upcallFuncAddr);
            Assert.assertEquals(intHandle1.get(resultSegmt, 0L), 110224466);
            Assert.assertEquals(intHandle2.get(resultSegmt, 0L), 89113354);
        }
    }

    @Test
    public void test_InvalidLinkerOptions_firstVariadicArg() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStructByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_add2IntStructs_returnStruct,
                        FunctionDescriptor.of(structLayout, structLayout, structLayout), arena, Linker.Option.firstVariadicArg(0));
                fail("Failed to throw IllegalArgumentException in the case of the invalid linker option for upcall.");
            });
            assertTrue(exception.getMessage().matches("Not supported for upcall.*"));
        }
    }

    @Test
    public void test_InvalidLinkerOptions_captureCallState() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStructByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_add2IntStructs_returnStruct,
                        FunctionDescriptor.of(structLayout, structLayout, structLayout), arena, Linker.Option.captureCallState("errno"));
                fail("Failed to throw IllegalArgumentException in the case of the invalid linker option for upcall.");
            });
            assertTrue(exception.getMessage().matches("Not supported for upcall.*"));
        }
    }

    @Test
    public void test_InvalidLinkerOptions_isCritical_1() throws Throwable {
        GroupLayout structLayout = MemoryLayout.structLayout(JAVA_INT.withName("elem1"), JAVA_INT.withName("elem2"));

        FunctionDescriptor fd = FunctionDescriptor.of(structLayout, structLayout, structLayout, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("add2IntStructs_returnStructByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd);

        try (Arena arena = Arena.ofConfined()) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_add2IntStructs_returnStruct,
                        FunctionDescriptor.of(structLayout, structLayout, structLayout), arena, Linker.Option.critical(false));
                fail("Failed to throw IllegalArgumentException in the case of the invalid linker option for upcall.");
            });
            assertTrue(exception.getMessage().matches("Not supported for upcall.*"));
        }
    }

    @Test
    @Ignore
    public void test_InvalidLinkerOptions_isCritical_2() throws Throwable {
        FunctionDescriptor fd = FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS);
        MemorySegment functionSymbol = nativeLibLookup.find("captureTrivialOptionByUpcallMH").get();
        MethodHandle mh = linker.downcallHandle(functionSymbol, fd, Linker.Option.critical(false));

        try (Arena arena = Arena.ofConfined()) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                MemorySegment upcallFuncAddr = linker.upcallStub(UpcallMethodHandles.MH_captureCriticalOption,
                        FunctionDescriptor.of(JAVA_INT, JAVA_INT), arena);
                int result = (int) mh.invoke(111, upcallFuncAddr);
                fail("Failed to throw IllegalThreadStateException in the case of the invalid upcall during the critical downcall.");
            });
            assertTrue(exception.getMessage().matches(".* wrong thread state for upcall"));
        }
    }
}
