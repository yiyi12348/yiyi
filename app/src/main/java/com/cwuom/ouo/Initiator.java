package com.cwuom.ouo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;

import mqq.app.AppRuntime;

public class Initiator {
    private static ClassLoader sHostClassLoader;
    private static ClassLoader sPluginParentClassLoader;
    private static Class<?> kQQAppInterface = null;

    private Initiator() {
        throw new AssertionError("No instance for you!");
    }

    public static void init(ClassLoader classLoader) {
        sHostClassLoader = classLoader;
        sPluginParentClassLoader = Initiator.class.getClassLoader();
    }

    public static ClassLoader getPluginClassLoader() {
        return Initiator.class.getClassLoader();
    }

    public static ClassLoader getHostClassLoader() {
        return sHostClassLoader;
    }

    /**
     * Load a class, if the class is not found, null will be returned.
     *
     * @param className The class name.
     * @return The class, or null if not found.
     */
    @Nullable
    public static Class<?> load(String className) {
        if (sPluginParentClassLoader == null || className == null || className.isEmpty()) {
            return null;
        }
        if (className.endsWith(";") || className.contains("/")) {
            className = className.replace('/', '.');
            if (className.endsWith(";")) {
                if (className.charAt(0) == 'L') {
                    className = className.substring(1, className.length() - 1);
                } else {
                    className = className.substring(0, className.length() - 1);
                }
            }
        }
        try {
            return sHostClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Nullable
    public static Class<?> findClassWithSynthetics(@NonNull String className1, @NonNull String className2) {
        Class<?> clazz = load(className1);
        if (clazz != null) {
            return clazz;
        }
        return load(className2);
    }

    public static Class<?> _ThemeUtil() {
        return findClassWithSynthetics("com/tencent/mobileqq/theme/ThemeUtil", "com.tencent.mobileqq.vas.theme.api.ThemeUtil");
    }

    /**
     * Load a class, if the class is not found, a ClassNotFoundException will be thrown.
     *
     * @param className The class name.
     * @return The class.
     * @throws ClassNotFoundException If the class is not found.
     */
    @NonNull
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        Class<?> ret = load(className);
        if (ret == null) {
            throw new ClassNotFoundException(className);
        }
        return ret;
    }

    @NonNull
    public static Class<?> loadClassEither(@NonNull String... classNames) throws ClassNotFoundException {
        for (String className : classNames) {
            Class<?> ret = load(className);
            if (ret != null) {
                return ret;
            }
        }
        throw new ClassNotFoundException("Class not found for names: " + Arrays.toString(classNames));
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends AppRuntime> _QQAppInterface() {
        if (kQQAppInterface == null) {
            kQQAppInterface = load("com/tencent/mobileqq/app/QQAppInterface");
            if (kQQAppInterface == null) {
                Class<?> ref = load("com/tencent/mobileqq/app/QQAppInterface$1");
                if (ref != null) {
                    try {
                        kQQAppInterface = ref.getDeclaredField("this$0").getType();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return (Class<? extends AppRuntime>) kQQAppInterface;
    }

}
