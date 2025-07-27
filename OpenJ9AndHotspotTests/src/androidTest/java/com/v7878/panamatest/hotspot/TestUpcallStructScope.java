/*
 * Copyright (c) 2020, 2023, Oracle and/or its affiliates. All rights reserved.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package com.v7878.panamatest.hotspot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.v7878.foreign.Arena;
import com.v7878.foreign.FunctionDescriptor;
import com.v7878.foreign.MemorySegment;

import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TestUpcallStructScope extends NativeTestHelper {
    static final MethodHandle MH_do_upcall;
    static final MethodHandle MH_do_upcall_ptr;
    static final MethodHandle MH_Consumer_accept;
    static final MethodHandle MH_BiConsumer_accept;

    static {
        System.loadLibrary("TestUpcallStructScope");
        MH_do_upcall = LINKER.downcallHandle(
                findNativeOrThrow("do_upcall"),
                FunctionDescriptor.ofVoid(C_POINTER, S_PDI_LAYOUT)
        );
        MH_do_upcall_ptr = LINKER.downcallHandle(
                findNativeOrThrow("do_upcall_ptr"),
                FunctionDescriptor.ofVoid(C_POINTER, S_PDI_LAYOUT, C_POINTER)
        );

        try {
            MH_Consumer_accept = MethodHandles.publicLookup().findVirtual(Consumer.class, "accept",
                    MethodType.methodType(void.class, Object.class));
            MH_BiConsumer_accept = MethodHandles.publicLookup().findVirtual(BiConsumer.class, "accept",
                    MethodType.methodType(void.class, Object.class, Object.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static MethodHandle methodHandle(Consumer<MemorySegment> callback) {
        return MH_Consumer_accept.bindTo(callback)
                .asType(MethodType.methodType(void.class, MemorySegment.class));
    }

    private static MethodHandle methodHandle(BiConsumer<MemorySegment, MemorySegment> callback) {
        return MH_BiConsumer_accept.bindTo(callback)
                .asType(MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class));
    }

    @Test
    public void testUpcall() throws Throwable {
        AtomicReference<MemorySegment> capturedSegment = new AtomicReference<>();
        MethodHandle target = methodHandle(capturedSegment::set);
        FunctionDescriptor upcallDesc = FunctionDescriptor.ofVoid(S_PDI_LAYOUT);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment upcallStub = LINKER.upcallStub(target, upcallDesc, arena);
            MemorySegment argSegment = arena.allocate(S_PDI_LAYOUT);
            MH_do_upcall.invoke(upcallStub, argSegment);
        }

        MemorySegment captured = capturedSegment.get();
        System.out.println(captured.scope());
        assertFalse(captured.scope().isAlive());
    }

    @Test
    public void testOtherPointer() throws Throwable {
        AtomicReference<MemorySegment> capturedSegment = new AtomicReference<>();
        MethodHandle target = methodHandle((unused, addr) -> capturedSegment.set(addr));
        FunctionDescriptor upcallDesc = FunctionDescriptor.ofVoid(S_PDI_LAYOUT, C_POINTER);
        MemorySegment argAddr = MemorySegment.ofAddress(42);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment upcallStub = LINKER.upcallStub(target, upcallDesc, arena);
            MemorySegment argSegment = arena.allocate(S_PDI_LAYOUT);
            MH_do_upcall_ptr.invoke(upcallStub, argSegment, argAddr);
        }

        // We've captured the address '42' from the upcall. This should have
        // the global scope, so it should still be alive here.
        MemorySegment captured = capturedSegment.get();
        assertEquals(argAddr, captured);
        assertTrue(captured.scope().isAlive());
    }
}
