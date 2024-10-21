package com.cwuom.ouo.util;

import android.util.Log;

import androidx.annotation.Nullable;

import com.cwuom.ouo.Initiator;
import com.cwuom.ouo.reflex.Reflex;

import org.jetbrains.annotations.Contract;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import mqq.app.AppRuntime;
import mqq.app.MobileQQ;

public class AppRuntimeHelper {

    private static Field f_mAppRuntime = null;

    private AppRuntimeHelper() {
    }

    public static long getLongAccountUin() {
        try {
            AppRuntime rt = getAppRuntime();
            if (rt == null) {
                // getLongAccountUin/E getAppRuntime == null
                return -1;
            }
            return (long) Reflex.invokeVirtual(rt, "getLongAccountUin");
        } catch (ReflectiveOperationException e) {
            Logger.e(e);
        }
        return -1;
    }

    public static AppRuntime getQQAppInterface() {
        AppRuntime art = getAppRuntime();
        if (art == null) {
            return null;
        }
        if (Initiator._QQAppInterface().isAssignableFrom(art.getClass())) {
            return art;
        } else {
            throw new IllegalStateException("QQAppInterface is not available in current process");
        }
    }

    /**
     * Peek the AppRuntime instance.
     * @return AppRuntime instance, or null if not ready.
     */
    @Nullable
    public static AppRuntime getAppRuntime() {
        Object sMobileQQ = MobileQQ.sMobileQQ;
        if (sMobileQQ == null) {
            return null;
        }
        try {
            if (f_mAppRuntime == null) {
                f_mAppRuntime = MobileQQ.class.getDeclaredField("mAppRuntime");
                f_mAppRuntime.setAccessible(true);
            }
            return (AppRuntime) f_mAppRuntime.get(sMobileQQ);
        } catch (ReflectiveOperationException e) {
            Logger.e(e);
            // unreachable
            return null;
        }
    }

    public static String getAccount() {
        Object rt = getAppRuntime();
        try {
            return (String) Reflex.invokeVirtual(rt, "getAccount");
        } catch (ReflectiveOperationException e) {
            Logger.e(e);
            // unreachable
            return null;
        }
    }

    @Contract("null -> fail")
    public static void checkUinValid(String uin) {
        if (uin == null || uin.length() == 0) {
            throw new IllegalArgumentException("uin is empty");
        }
        try {
            // allow cases like 9915...
            if (Long.parseLong(uin) < 1000) {
                throw new IllegalArgumentException("uin is invalid: " + uin);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("uin is invalid: " + uin);
        }
    }

    private static Method sMethodGetServerTime = null;

    public static long getServerTime() throws ReflectiveOperationException {
        if (sMethodGetServerTime == null) {
            sMethodGetServerTime = Initiator.loadClass("com.tencent.mobileqq.msf.core.NetConnInfoCenter").getDeclaredMethod("getServerTime");
        }
        return (Long) sMethodGetServerTime.invoke(null);
    }
}