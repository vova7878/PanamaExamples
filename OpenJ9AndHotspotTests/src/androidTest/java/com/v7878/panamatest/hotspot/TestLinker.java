/*
 * Copyright (c) 2022, 2023, Oracle and/or its affiliates. All rights reserved.
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

import static com.v7878.foreign.MemoryLayout.paddingLayout;
import static com.v7878.foreign.MemoryLayout.sequenceLayout;
import static com.v7878.foreign.MemoryLayout.structLayout;
import static com.v7878.foreign.MemoryLayout.unionLayout;
import static com.v7878.foreign.ValueLayout.JAVA_BYTE;
import static com.v7878.foreign.ValueLayout.JAVA_CHAR;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static com.v7878.foreign.ValueLayout.JAVA_LONG;
import static com.v7878.foreign.ValueLayout.JAVA_SHORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import com.v7878.foreign.FunctionDescriptor;
import com.v7878.foreign.Linker;
import com.v7878.foreign.MemoryLayout;
import com.v7878.foreign.PaddingLayout;
import com.v7878.foreign.SequenceLayout;
import com.v7878.foreign.StructLayout;
import com.v7878.foreign.ValueLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(DataProviderRunner.class)
public class TestLinker extends NativeTestHelper {

    static final boolean IS_FALLBACK_LINKER = false; // CABI.current() == CABI.FALLBACK;

    record LinkRequest(FunctionDescriptor descriptor, Linker.Option... options) {
    }

    @Test
    @UseDataProvider("notSameCases")
    public void testLinkerOptionsCache(LinkRequest l1, LinkRequest l2) {
        Linker linker = Linker.nativeLinker();
        MethodHandle mh1 = linker.downcallHandle(l1.descriptor(), l1.options());
        MethodHandle mh2 = linker.downcallHandle(l2.descriptor(), l2.options());
        // assert that these are 2 distinct link request. No caching allowed
        assertNotSame(mh1, mh2);
    }

    @DataProvider(format = "%m[%i]")
    public static Object[][] notSameCases() {
        FunctionDescriptor fd_II_V = FunctionDescriptor.ofVoid(C_INT, C_INT);
        return new Object[][]{
                {new LinkRequest(fd_II_V), new LinkRequest(fd_II_V, Linker.Option.firstVariadicArg(1))},
                {new LinkRequest(FunctionDescriptor.ofVoid(JAVA_SHORT)), new LinkRequest(FunctionDescriptor.ofVoid(JAVA_CHAR))},
                {new LinkRequest(FunctionDescriptor.ofVoid(JAVA_SHORT)), new LinkRequest(FunctionDescriptor.ofVoid(JAVA_CHAR))},
        };
    }

    @Test
    @UseDataProvider("namedDescriptors")
    public void testNamedLinkerCache(FunctionDescriptor f1, FunctionDescriptor f2) {
        Linker linker = Linker.nativeLinker();
        MethodHandle mh1 = linker.downcallHandle(f1);
        MethodHandle mh2 = linker.downcallHandle(f2);
        // assert that these are the same link request, even though layout names differ
        assertSame(mh1, mh2);
    }

    @DataProvider(format = "%m[%i]")
    public static Object[][] namedDescriptors() {
        List<Object[]> cases = new ArrayList<>(Arrays.asList(new Object[][]{
                {FunctionDescriptor.ofVoid(C_INT),
                        FunctionDescriptor.ofVoid(C_INT.withName("x"))},
                {FunctionDescriptor.ofVoid(structLayout(C_INT)),
                        FunctionDescriptor.ofVoid(structLayout(C_INT).withName("x"))},
                {FunctionDescriptor.ofVoid(structLayout(C_INT)),
                        FunctionDescriptor.ofVoid(structLayout(C_INT.withName("x")))},
                {FunctionDescriptor.ofVoid(structLayout(sequenceLayout(1, C_INT))),
                        FunctionDescriptor.ofVoid(structLayout(sequenceLayout(1, C_INT).withName("x")))},
                {FunctionDescriptor.ofVoid(structLayout(sequenceLayout(1, C_INT))),
                        FunctionDescriptor.ofVoid(structLayout(sequenceLayout(1, C_INT.withName("x"))))},
                {FunctionDescriptor.ofVoid(C_POINTER),
                        FunctionDescriptor.ofVoid(C_POINTER.withName("x"))},
                {FunctionDescriptor.ofVoid(C_POINTER.withTargetLayout(C_INT)),
                        FunctionDescriptor.ofVoid(C_POINTER.withTargetLayout(C_INT.withName("x")))},
                {FunctionDescriptor.ofVoid(C_POINTER.withTargetLayout(C_INT)),
                        FunctionDescriptor.ofVoid(C_POINTER.withName("x").withTargetLayout(C_INT.withName("x")))},
        }));

        if (!IS_FALLBACK_LINKER) {
            cases.add(new Object[]{FunctionDescriptor.ofVoid(unionLayout(C_INT)),
                    FunctionDescriptor.ofVoid(unionLayout(C_INT).withName("x"))});
            cases.add(new Object[]{FunctionDescriptor.ofVoid(unionLayout(C_INT)),
                    FunctionDescriptor.ofVoid(unionLayout(C_INT.withName("x")))});
        }
        if (C_LONG_LONG.byteAlignment() == 8) {
            cases.add(new Object[]{FunctionDescriptor.ofVoid(structLayout(C_INT, paddingLayout(4), C_LONG_LONG)),
                    FunctionDescriptor.ofVoid(structLayout(C_INT, paddingLayout(4), C_LONG_LONG.withName("x")))});
            cases.add(new Object[]{FunctionDescriptor.ofVoid(structLayout(C_INT, paddingLayout(4), C_LONG_LONG)),
                    FunctionDescriptor.ofVoid(structLayout(C_INT, paddingLayout(4).withName("x"), C_LONG_LONG))});
        }

        return cases.toArray(new Object[0][]);
    }

    @DataProvider(format = "%m[%i]")
    public static Object[][] invalidIndexCases() {
        return new Object[][]{
                {-1,},
                {42,},
        };
    }

    @Test
    @UseDataProvider("invalidIndexCases")
    public void testInvalidOption(int invalidIndex) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Linker.Option option = Linker.Option.firstVariadicArg(invalidIndex);
            FunctionDescriptor desc = FunctionDescriptor.ofVoid();
            Linker.nativeLinker().downcallHandle(desc, option); // throws
        });
        assertTrue(exception.getMessage().matches(".*not in bounds for descriptor.*"));
    }

    @Test
    public void testInvalidPreservedValueName() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            Linker.Option.captureCallState("foo"); // throws
        });
        assertTrue(exception.getMessage().matches(".*Unknown name.*"));
    }

    @Test
    @UseDataProvider("canonicalTypeNames")
    public void testCanonicalLayouts(String typeName) {
        MemoryLayout layout = LINKER.canonicalLayouts().get(typeName);
        assertNotNull(layout);
        assertTrue(layout instanceof ValueLayout);
    }

    @Test
    public void embeddedPaddingLayout() {
        PaddingLayout padding = MemoryLayout.paddingLayout(64).withByteAlignment(64);
        SequenceLayout sequence = MemoryLayout.sequenceLayout(2, padding);
        StructLayout struct = MemoryLayout.structLayout(sequence);
        FunctionDescriptor fd = FunctionDescriptor.of(struct, struct);
        Linker linker = Linker.nativeLinker();
        var x = assertThrows(IllegalArgumentException.class, () -> linker.downcallHandle(fd));
        assertTrue(x.getMessage().contains("not supported because a sequence of a padding layout is not allowed"));
    }

    @Test
    public void groupLayoutWithOnlyPadding() {
        PaddingLayout padding = MemoryLayout.paddingLayout(1);
        StructLayout struct = MemoryLayout.structLayout(padding);
        FunctionDescriptor fd = FunctionDescriptor.of(struct, struct);
        Linker linker = Linker.nativeLinker();
        var x = assertThrows(IllegalArgumentException.class, () -> linker.downcallHandle(fd));
        assertTrue(x.getMessage().contains("is non-empty and only has padding layouts"));
    }

    @Test
    public void interwovenPadding() {
        Linker linker = Linker.nativeLinker();
        var padding1 = MemoryLayout.paddingLayout(1);
        var padding2 = MemoryLayout.paddingLayout(2).withByteAlignment(2);
        var struct = MemoryLayout.structLayout(JAVA_BYTE, padding1, padding2, JAVA_INT);
        var fd = FunctionDescriptor.of(struct, struct, struct);
        var e = assertThrows(IllegalArgumentException.class, () -> linker.downcallHandle(fd));
        assertEquals(e.getMessage(),
                "The padding layout x2 was preceded by another padding layout x1 in " + struct);
    }

    @Test
    public void stackedPadding() {
        Linker linker = Linker.nativeLinker();
        var struct32 = MemoryLayout.structLayout(MemoryLayout.sequenceLayout(4, JAVA_LONG));
        var padding1 = MemoryLayout.paddingLayout(1);
        var padding2 = MemoryLayout.paddingLayout(2).withByteAlignment(2);
        var padding4 = MemoryLayout.paddingLayout(4).withByteAlignment(4);
        var padding8 = MemoryLayout.paddingLayout(8).withByteAlignment(8);
        var padding16 = MemoryLayout.paddingLayout(16).withByteAlignment(16);
        var padding32 = MemoryLayout.paddingLayout(32).withByteAlignment(32);
        var union = MemoryLayout.unionLayout(struct32, padding32);
        var struct = MemoryLayout.structLayout(JAVA_BYTE, padding1, padding2, padding4, padding8, padding16, union);
        var fd = FunctionDescriptor.of(struct, struct, struct);
        var e = assertThrows(IllegalArgumentException.class, () -> linker.downcallHandle(fd));
        assertEquals(e.getMessage(),
                "The padding layout x2 was preceded by another padding layout x1 in " + struct);
    }

    @Test
    public void paddingUnionByteSize3() {
        Linker linker = Linker.nativeLinker();
        var union = MemoryLayout.unionLayout(MemoryLayout.paddingLayout(3), JAVA_INT);
        var fd = FunctionDescriptor.of(union, union, union);
        var e = assertThrows(IllegalArgumentException.class, () -> linker.downcallHandle(fd));
        assertEquals(e.getMessage(), "Superfluous padding x3 in " + union);
    }

    @Test
    public void paddingUnionByteSize4() {
        Linker linker = Linker.nativeLinker();
        var union = MemoryLayout.unionLayout(MemoryLayout.paddingLayout(4), JAVA_INT);
        var fd = FunctionDescriptor.of(union, union, union);
        var e = assertThrows(IllegalArgumentException.class, () -> linker.downcallHandle(fd));
        assertEquals(e.getMessage(), "Superfluous padding x4 in " + union);
    }

    @Test
    public void paddingUnionByteSize5() {
        Linker linker = Linker.nativeLinker();
        var union = MemoryLayout.unionLayout(MemoryLayout.paddingLayout(5), JAVA_INT);
        var fd = FunctionDescriptor.of(union, union, union);
        var e = assertThrows(IllegalArgumentException.class, () -> linker.downcallHandle(fd));
        assertEquals(e.getMessage(), "Layout '" + union + "' has unexpected size: 5 != 4");
    }

    @Test
    public void paddingUnionSeveral() {
        Linker linker = Linker.nativeLinker();
        var union = MemoryLayout.unionLayout(
                MemoryLayout.sequenceLayout(3, JAVA_INT),
                JAVA_INT,
                MemoryLayout.paddingLayout(16),
                MemoryLayout.paddingLayout(16));
        var fd = FunctionDescriptor.of(union, union, union);
        var e = assertThrows(IllegalArgumentException.class, () -> linker.downcallHandle(fd));
        assertEquals(e.getMessage(), "More than one padding in " + union);
    }

    @Test
    public void sequenceOfZeroElements() {
        Linker linker = Linker.nativeLinker();
        var sequence0a4 = MemoryLayout.sequenceLayout(0, JAVA_INT);
        var sequence3a1 = MemoryLayout.sequenceLayout(3, JAVA_BYTE);
        var padding1a1 = MemoryLayout.paddingLayout(1);
        var struct4a4 = MemoryLayout.structLayout(sequence0a4, sequence3a1, padding1a1);
        var fd = FunctionDescriptor.of(struct4a4, struct4a4, struct4a4);
        linker.downcallHandle(fd);
    }

    @DataProvider(format = "%m[%i]")
    public static Object[][] canonicalTypeNames() {
        return new Object[][]{
                {"bool"},
                {"char"},
                {"short"},
                {"int"},
                {"long"},
                {"long long"},
                {"float"},
                {"double"},
                {"void*"},
                {"size_t"},
                {"wchar_t"},
        };
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCanonicalLayoutsUnmodifiable() {
        LINKER.canonicalLayouts().put("asdf", C_INT);
    }
}
