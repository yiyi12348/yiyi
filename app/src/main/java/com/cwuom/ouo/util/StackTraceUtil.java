package com.cwuom.ouo.util;

import android.content.Context;
import android.os.Bundle;

import java.util.Arrays;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class StackTraceUtil {
    public static void printStackTrace(XC_MethodHook.MethodHookParam param, String loadClazz, String method) throws ClassNotFoundException {
        Context context = (Context) param.args[0];
        ClassLoader cl = context.getClassLoader();
        Class<?> clazz = cl.loadClass(loadClazz);

        XposedHelpers.findAndHookMethod(
            clazz,
            method,
            Bundle.class,
            new XC_MethodHook(){
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Logger.d("beforeHookedMethod" + Arrays.toString(param.args));
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    for (Map.Entry<Thread, StackTraceElement[]> stackTrace:Thread.getAllStackTraces().entrySet())
                    {
                        Thread thread = stackTrace.getKey();
                        StackTraceElement[] stack = stackTrace.getValue();

                        if (thread.equals(Thread.currentThread())) {
                            continue;
                        }

                        Logger.e("[Dump Stack]"+"**********Thread name：" + thread.getName()+"**********");
                        int index = 0;
                        for (StackTraceElement stackTraceElement : stack) {

                            Logger.d("[Dump Stack]"+index+": "+ stackTraceElement.getClassName()
                                    +"----"+stackTraceElement.getFileName()
                                    +"----" + stackTraceElement.getLineNumber()
                                    +"----" +stackTraceElement.getMethodName());

                            index++;
                        }

                        XposedBridge.log("afterHookedMethod result:" + param.getResult());

                    }
                    Logger.e("[Dump Stack]"+"********************* over **********************");
                }
            });
    }
}
