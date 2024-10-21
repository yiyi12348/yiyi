package com.cwuom.ouo.bridge.kernelcompat;

import org.jetbrains.annotations.NotNull;

public class KernelObjectHelper {

    private KernelObjectHelper() {
    }

    public static void throwKernelObjectNotSupported(@NotNull Class<?> clazz) {
        String shortName = clazz.getSimpleName();
        throw new UnsupportedOperationException("Kernel object " + shortName + " is not supported");
    }

    public static void throwKernelObjectNotSupported(@NotNull String name) {
        throw new UnsupportedOperationException("Kernel object " + name + " is not supported");
    }

}
