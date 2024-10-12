package com.v7878.panamatest.hotspot;

import static com.v7878.foreign.ValueLayout.ADDRESS;
import static com.v7878.foreign.ValueLayout.JAVA_INT;
import static org.junit.Assert.assertTrue;

import com.v7878.foreign.FunctionDescriptor;
import com.v7878.foreign.Linker;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SymbolLookup;

import org.junit.Ignore;
import org.junit.Test;

import java.lang.invoke.MethodHandle;

public class TestNULLAddress {

    static {
        System.loadLibrary("Null");
    }

    static final Linker LINKER = Linker.nativeLinker();

    @Test(expected = IllegalArgumentException.class)
    public void testNULLLinking() {
        LINKER.downcallHandle(
                MemorySegment.NULL,
                FunctionDescriptor.ofVoid());
    }

    //TODO
    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void testNULLVirtual() throws Throwable {
        MethodHandle mh = LINKER.downcallHandle(
                FunctionDescriptor.ofVoid());
        mh.invokeExact(MemorySegment.NULL);
    }

    @Test
    public void testNULLReturn_target() throws Throwable {
        MethodHandle mh = LINKER.downcallHandle(SymbolLookup.loaderLookup().find("get_null").get(),
                FunctionDescriptor.of(ADDRESS.withTargetLayout(JAVA_INT)));
        MemorySegment ret = (MemorySegment) mh.invokeExact();
        assertTrue(ret.equals(MemorySegment.NULL));
    }

    @Test
    public void testNULLReturn_plain() throws Throwable {
        MethodHandle mh = LINKER.downcallHandle(SymbolLookup.loaderLookup().find("get_null").get(),
                FunctionDescriptor.of(ADDRESS));
        MemorySegment ret = (MemorySegment) mh.invokeExact();
        assertTrue(ret.equals(MemorySegment.NULL));
    }
}
