package com.cwuom.ouo.hooks;

import static com.cwuom.ouo.Initiator.loadClass;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;

import com.cwuom.ouo.creator.ElementSender;
import com.cwuom.ouo.reflex.XMethod;
import com.cwuom.ouo.startup.HookBase;
import com.cwuom.ouo.util.Logger;
import com.cwuom.ouo.util.Utils;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import lombok.Getter;

@Getter
@SuppressLint("DiscouragedApi")
public class HookBtnMore implements HookBase {
    public static String method_name = "劫持\"更多功能\"";

    private void hookTargetActivity(Activity activity) {
        try {
            XposedBridge.hookMethod(XMethod.clz("com.tencent.qqnt.aio.shortcutbar.PanelIconLinearLayout").ret(ImageView.class).ignoreParam().get(), new XC_MethodHook(50) {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    ImageView imageView = (ImageView) param.getResult();
                    if ("更多功能".contentEquals(imageView.getContentDescription())){
                        imageView.setOnLongClickListener(view -> {
                            Utils.runOnUiThread(() -> ElementSender.createView(activity, view.getContext(), ""));
                            return true;
                        });
                    }
                }
            });
        } catch (NoSuchMethodException e) {
            Logger.e(e, true);
        }
    }

    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> clazz = loadClass("com.tencent.mobileqq.activity.SplashActivity");
            XposedHelpers.findAndHookMethod(clazz, "doOnCreate", Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            hookTargetActivity((Activity) param.thisObject);
                        }
                    });
        } catch (ClassNotFoundException e) {
            Logger.e("Failed to hook target method: " + e, true);
        }
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