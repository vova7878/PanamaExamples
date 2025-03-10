/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
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

import com.v7878.foreign.Arena;
import com.v7878.foreign.FunctionDescriptor;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SegmentAllocator;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class TestDowncallBase extends CallGeneratorHelper {

    Object doCall(MemorySegment symbol, SegmentAllocator allocator, FunctionDescriptor descriptor, Object[] args) throws Throwable {
        MethodHandle mh = downcallHandle(LINKER, symbol, allocator, descriptor);
        return mh.invokeWithArguments(args);
    }

    static FunctionDescriptor function(Ret ret, List<ParamType> params, List<StructFieldType> fields, List<MemoryLayout> prefix) {
        List<MemoryLayout> pLayouts = NewApiUtils.toList(params.stream().map(p -> p.layout(fields)));
        MemoryLayout[] paramLayouts = Stream.concat(prefix.stream(), pLayouts.stream()).toArray(MemoryLayout[]::new);
        return ret == Ret.VOID ?
                FunctionDescriptor.ofVoid(paramLayouts) :
                FunctionDescriptor.of(paramLayouts[prefix.size()], paramLayouts);
    }

    static Object[] makeArgs(Arena arena, FunctionDescriptor descriptor, List<Consumer<Object>> checks, int returnIdx) {
        List<MemoryLayout> argLayouts = descriptor.argumentLayouts();
        TestValue[] args = new TestValue[argLayouts.size()];
        int argNum = 0;
        for (MemoryLayout layout : argLayouts) {
            args[argNum++] = genTestValue(layout, arena);
        }

        if (descriptor.returnLayout().isPresent()) {
            checks.add(args[returnIdx].check());
        }
        return Stream.of(args).map(TestValue::value).toArray();
    }
}
