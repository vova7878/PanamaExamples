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

import static com.v7878.foreign.ValueLayout.JAVA_INT;

import com.v7878.foreign.FunctionDescriptor;
import com.v7878.foreign.Linker;
import com.v7878.foreign.MemorySegment;
import com.v7878.foreign.SymbolLookup;

import org.junit.Assert;
import org.junit.Test;

import java.lang.invoke.MethodHandle;

/**
 * Test cases for JEP 454: Foreign Linker API for primitive types in downcall,
 * which verifies the downcalls with the diffrent layouts and arguments/return types in multithreading.
 */
public class MultiThreadingTests3 implements Thread.UncaughtExceptionHandler {
    private volatile Throwable initException;
    private static Linker linker = Linker.nativeLinker();

    static {
        System.loadLibrary("clinkerffitests");
    }

    private static final SymbolLookup nativeLibLookup = SymbolLookup.loaderLookup();

    @Override
    public void uncaughtException(Thread thr, Throwable t) {
        initException = t;
    }

    @Test
    public void test_twoThreadsWithDiffFuncDescriptor() throws Throwable {
        Thread thr1 = new Thread() {
            @Override
            public void run() {
                try {
                    FunctionDescriptor fd = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT);
                    MemorySegment functionSymbol = nativeLibLookup.find("add2Ints").get();
                    MethodHandle mh = linker.downcallHandle(functionSymbol, fd);
                    int result = (int) mh.invokeExact(112, 123);
                    Assert.assertEquals(result, 235);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        };

        Thread thr2 = new Thread() {
            @Override
            public void run() {
                try {
                    FunctionDescriptor fd = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT);
                    MemorySegment functionSymbol = nativeLibLookup.find("add3Ints").get();
                    MethodHandle mh = linker.downcallHandle(functionSymbol, fd);
                    int result = (int) mh.invokeExact(112, 123, 235);
                    Assert.assertEquals(result, 470);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
        };

        thr1.setUncaughtExceptionHandler(this);
        thr2.setUncaughtExceptionHandler(this);
        initException = null;

        thr1.start();
        thr2.start();

        thr1.join();
        thr2.join();

        if (initException != null) {
            throw new RuntimeException(initException);
        }
    }
}
