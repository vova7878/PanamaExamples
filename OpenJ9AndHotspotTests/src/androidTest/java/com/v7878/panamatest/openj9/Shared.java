package com.v7878.panamatest.openj9;

import static com.v7878.foreign.ValueLayout.ADDRESS;

import com.v7878.foreign.Linker;
import com.v7878.foreign.ValueLayout;
import com.v7878.foreign.ValueLayout.OfDouble;
import com.v7878.foreign.ValueLayout.OfLong;

public class Shared {
    public static final OfDouble C_DOUBLE = (OfDouble) Linker.nativeLinker().canonicalLayouts().get("double");
    public static final OfLong C_LONG_LONG = (OfLong) Linker.nativeLinker().canonicalLayouts().get("long long");
    public static final ValueLayout C_SIZE_T = (ValueLayout) Linker.nativeLinker().canonicalLayouts().get("size_t");
    public static final boolean IS32BIT = ADDRESS.byteSize() == 4;
    public static final boolean IS_DOUBLE_SMALL_ALIGNMENT = C_DOUBLE.byteAlignment() == 4;
    public static final boolean IS_LONG_SMALL_ALIGNMENT = C_LONG_LONG.byteAlignment() == 4;
}
