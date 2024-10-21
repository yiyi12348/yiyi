package com.cwuom.ouo.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.widget.TextView;

import com.cwuom.ouo.startup.HookBase;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import lombok.Getter;

@Getter
public class HookTextView implements HookBase {
    public static String method_name = "拦截卡屏消息";

    public void init(XC_LoadPackage.LoadPackageParam lpparam){
        XC_MethodHook hodorHodor = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                CharSequence text = (CharSequence) param.args[0];
                if (text != null) {
                    if (text.length() > 5000) {
                        String truncatedText = "ouo: *疑似卡屏，已阻止此文字消息的加载";
                        param.args[0] = truncatedText;
                    }
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        };


        findAndHookMethod(TextView.class, "setText", CharSequence.class, TextView.BufferType.class,
                boolean.class, int.class, hodorHodor);
        findAndHookMethod(TextView.class, "setText", CharSequence.class, hodorHodor);
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
