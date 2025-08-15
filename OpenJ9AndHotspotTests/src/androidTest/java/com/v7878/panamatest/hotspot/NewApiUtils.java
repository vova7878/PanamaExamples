package com.v7878.panamatest.hotspot;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Stream;

public class NewApiUtils {
    @SuppressWarnings("unchecked")
    public static <T> List<T> toList(Stream<T> stream) {
        return (List<T>) List.of(stream.toArray());
    }

    public static ByteBuffer slice(ByteBuffer buffer, int index, int length) {
        buffer = buffer.duplicate();
        buffer.position(index);
        buffer.limit(index + length);
        return buffer.slice();
    }
}
