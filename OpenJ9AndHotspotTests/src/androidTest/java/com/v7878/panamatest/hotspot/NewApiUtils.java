package com.v7878.panamatest.hotspot;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class NewApiUtils {
    @SuppressWarnings("unchecked")
    public static <T> List<T> toList(Stream<T> stream) {
        return (List<T>) List.of(stream.toArray());
    }

    public static MethodHandle empty(MethodType type) {
        var zero = zero(type.returnType());
        return MethodHandles.dropArguments(zero, 0, type.parameterArray());
    }

    private static void dummy() { /* nop */ }

    public static MethodHandle zero(Class<?> type) {
        Objects.requireNonNull(type);
        if (!type.isPrimitive()) {
            return MethodHandles.constant(type, null);
        }
        if (type == void.class) {
            try {
                return MethodHandles.lookup().findStatic(NewApiUtils.class,
                        "dummy", MethodType.methodType(void.class));
            } catch (IllegalAccessException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        if (type == byte.class) {
            return MethodHandles.constant(type, (byte) 0);
        }
        if (type == char.class) {
            return MethodHandles.constant(type, (char) 0);
        }
        if (type == short.class) {
            return MethodHandles.constant(type, (short) 0);
        }
        if (type == int.class) {
            return MethodHandles.constant(type, 0);
        }
        if (type == float.class) {
            return MethodHandles.constant(type, 0F);
        }
        if (type == long.class) {
            return MethodHandles.constant(type, 0L);
        }
        if (type == double.class) {
            return MethodHandles.constant(type, 0D);
        }
        throw new IllegalArgumentException("Illegal type: " + type);
    }

    public static ByteBuffer slice(ByteBuffer buffer, int index, int length) {
        buffer = buffer.duplicate();
        buffer.position(index);
        buffer.limit(index + length);
        return buffer.slice();
    }
}
