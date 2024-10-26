/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
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
package com.v7878.panamatest.hotspot;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.v7878.foreign.FunctionDescriptor;
import com.v7878.foreign.GroupLayout;
import com.v7878.foreign.Linker;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SegmentAllocator;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CallGeneratorHelper extends NativeTestHelper {

    static final List<MemoryLayout> STACK_PREFIX_LAYOUTS = NewApiUtils.toList(Stream.concat(
            Stream.generate(() -> (MemoryLayout) C_LONG_LONG).limit(8),
            Stream.generate(() -> (MemoryLayout) C_DOUBLE).limit(8)
    ));

    static SegmentAllocator THROWING_ALLOCATOR = (size, align) -> {
        throw new UnsupportedOperationException();
    };

    // Port-changed
    static final int SAMPLE_FACTOR = 17; //Integer.parseInt((String) System.getProperties().getOrDefault("generator.sample.factor", "-1"));

    static final int MAX_FIELDS = 3;
    static final int MAX_PARAMS = 3;
    static final int CHUNK_SIZE = 600;

    enum Ret {
        VOID,
        NON_VOID
    }

    enum StructFieldType {
        INT("int", C_INT),
        FLOAT("float", C_FLOAT),
        DOUBLE("double", C_DOUBLE),
        POINTER("void*", C_POINTER);

        final String typeStr;
        final MemoryLayout layout;

        StructFieldType(String typeStr, MemoryLayout layout) {
            this.typeStr = typeStr;
            this.layout = layout;
        }

        MemoryLayout layout() {
            return layout;
        }

        @SuppressWarnings("unchecked")
        static List<List<StructFieldType>>[] perms = new List[10];

        static List<List<StructFieldType>> perms(int i) {
            if (perms[i] == null) {
                perms[i] = generateTest(i, values());
            }
            return perms[i];
        }
    }

    enum ParamType {
        INT("int", C_INT),
        FLOAT("float", C_FLOAT),
        DOUBLE("double", C_DOUBLE),
        POINTER("void*", C_POINTER),
        STRUCT("struct S", null);

        private final String typeStr;
        private final MemoryLayout layout;

        ParamType(String typeStr, MemoryLayout layout) {
            this.typeStr = typeStr;
            this.layout = layout;
        }

        String type(List<StructFieldType> fields) {
            return this == STRUCT ?
                    typeStr + "_" + sigCode(fields) :
                    typeStr;
        }

        MemoryLayout layout(List<StructFieldType> fields) {
            if (this == STRUCT) {
                return MemoryLayout.paddedStructLayout(
                        IntStream.range(0, fields.size())
                                .mapToObj(i -> fields.get(i).layout().withName("f" + i))
                                .toArray(MemoryLayout[]::new));
            } else {
                return layout;
            }
        }

        @SuppressWarnings("unchecked")
        static List<List<ParamType>>[] perms = new List[10];

        static List<List<ParamType>> perms(int i) {
            if (perms[i] == null) {
                perms[i] = generateTest(i, values());
            }
            return perms[i];
        }
    }

    static <Z> List<List<Z>> generateTest(int i, Z[] elems) {
        List<List<Z>> res = new ArrayList<>();
        generateTest(i, new Stack<>(), elems, res);
        return res;
    }

    static <Z> void generateTest(int i, Stack<Z> combo, Z[] elems, List<List<Z>> results) {
        if (i == 0) {
            results.add(new ArrayList<>(combo));
        } else {
            for (Z z : elems) {
                combo.push(z);
                generateTest(i - 1, combo, elems, results);
                combo.pop();
            }
        }
    }

    @DataProvider(format = "%m[%i]")
    public static Object[][] functions() {
        int functions = 0;
        List<Object[]> downcalls = new ArrayList<>();
        for (Ret r : Ret.values()) {
            for (int i = 0; i <= MAX_PARAMS; i++) {
                if (r != Ret.VOID && i == 0) continue;
                for (List<ParamType> ptypes : ParamType.perms(i)) {
                    String retCode = r == Ret.VOID ? "V" : ptypes.get(0).name().charAt(0) + "";
                    String sigCode = sigCode(ptypes);
                    if (ptypes.contains(ParamType.STRUCT)) {
                        for (int j = 1; j <= MAX_FIELDS; j++) {
                            for (List<StructFieldType> fields : StructFieldType.perms(j)) {
                                String structCode = sigCode(fields);
                                int count = functions;
                                int fCode = functions++ / CHUNK_SIZE;
                                String fName = String.format("f%d_%s_%s_%s", fCode, retCode, sigCode, structCode);
                                if (SAMPLE_FACTOR == -1 || (count % SAMPLE_FACTOR) == 0) {
                                    downcalls.add(new Object[]{count, fName, r, ptypes, fields});
                                }
                            }
                        }
                    } else {
                        String structCode = sigCode(List.<StructFieldType>of());
                        int count = functions;
                        int fCode = functions++ / CHUNK_SIZE;
                        String fName = String.format("f%d_%s_%s_%s", fCode, retCode, sigCode, structCode);
                        if (SAMPLE_FACTOR == -1 || (count % SAMPLE_FACTOR) == 0) {
                            downcalls.add(new Object[]{count, fName, r, ptypes, List.of()});
                        }
                    }
                }
            }
        }
        return downcalls.toArray(new Object[0][]);
    }

    static <Z extends Enum<Z>> String sigCode(List<Z> elems) {
        return elems.stream().map(p -> p.name().charAt(0) + "").collect(Collectors.joining());
    }

    //helper methods

    MethodHandle downcallHandle(Linker abi, MemorySegment symbol, SegmentAllocator allocator, FunctionDescriptor descriptor) {
        MethodHandle mh = abi.downcallHandle(symbol, descriptor);
        if (descriptor.returnLayout().isPresent() && descriptor.returnLayout().get() instanceof GroupLayout) {
            mh = mh.bindTo(allocator);
        }
        return mh;
    }
}
