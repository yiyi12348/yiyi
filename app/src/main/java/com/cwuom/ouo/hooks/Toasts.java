package com.cwuom.ouo.hooks;

import static com.cwuom.ouo.Initiator.loadClass;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.cwuom.ouo.startup.HookBase;
import com.cwuom.ouo.util.Logger;
import com.cwuom.ouo.util.Utils;

import java.lang.reflect.Method;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Toasts implements HookBase {
    public static String method_name = "Toasts";

    private static Method showQQToastInUiThreadMethod;

    public static final int TYPE_INFO = 0;
    public static final int TYPE_ERROR = 1;
    public static final int TYPE_SUCCESS = 2;

    public static void show(Context ctx, int type, @NonNull CharSequence text) {
        Utils.runOnUiThread(() -> {
            try {
                showQQToastInUiThreadMethod.invoke(null, type, text);
            } catch (Throwable e) {
                Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show();
            }
        });

    }

    public static void info(Context ctx, @NonNull CharSequence text) {
        show(ctx, TYPE_INFO, text);
    }

    public static void success(Context ctx, @NonNull CharSequence text) {
        show(ctx, TYPE_SUCCESS, text);
    }

    public static void error(Context ctx, @NonNull CharSequence text) {
        show(ctx, TYPE_ERROR, text);
    }

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> QQToastUtilClass = loadClass("com.tencent.util.QQToastUtil");
            showQQToastInUiThreadMethod = QQToastUtilClass.getDeclaredMethod("showQQToastInUiThread", int.class, String.class);
        } catch (Exception e) {
            Logger.e("QQToastUtil Not Found : " + e);
        }
    }

    @Override
    public String getName() {
        return method_name;
    }

    @Override
    public Boolean isEnable() {
        return true;
    }
}