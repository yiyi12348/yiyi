package com.cwuom.ouo.hooks;

import static com.cwuom.ouo.util.Utils.replaceInvalidLinks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.cwuom.ouo.startup.HookBase;
import com.cwuom.ouo.util.Logger;
import com.tencent.qqnt.kernel.nativeinterface.MarkdownElement;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import lombok.Getter;


@SuppressLint("DiscouragedApi")
@Getter
public class HookLengthFilter implements HookBase {
    public static String method_name = "屏蔽字数限制";

    public void doHook(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("com.tencent.mobileqq.aio.input.sendmsg.AIOSendMsgVMDelegate", classLoader, "v", java.util.List.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(false);
                super.beforeHookedMethod(param);
            }
        });

        XposedHelpers.findAndHookMethod("com.tencent.mobileqq.aio.input.sendmsg.AIOSendMsgVMDelegate", classLoader, "w", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                param.setResult(false);
                super.beforeHookedMethod(param);
            }
        });
    }

    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        doHook(lpparam.classLoader);
    }

    @Override
    public String getName() {
        return method_name;
    }

    @Override
    public Boolean isEnable() {
        return null;
    }
}