package com.cwuom.ouo.hooks;

import static com.cwuom.ouo.util.Check.isValidInteger;
import static com.cwuom.ouo.util.Utils.replaceInvalidLinks;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

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
import com.tencent.qqnt.kernel.nativeinterface.FaceBubbleElement;
import com.tencent.qqnt.kernel.nativeinterface.MarkdownElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.SmallYellowFaceInfo;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import lombok.Getter;


@SuppressLint("DiscouragedApi")
@Getter
public class HookAvatar implements HookBase {
    public static String method_name = "Telegram模式";

    public void doHook(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod("com.tencent.mobileqq.aio.msglist.holder.component.avatar.AIOAvatarContentComponent$avatarContainer$2", classLoader, "invoke", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                RelativeLayout relativeLayout = (RelativeLayout) param.getResult();
                ImageView view = (ImageView) relativeLayout.getChildAt(0);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager windowManager = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);

                view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int screenWidth = displayMetrics.widthPixels;
                        int[] location = new int[2];
                        view.getLocationOnScreen(location);
                        int absoluteX = location[0];

                        if (absoluteX < screenWidth / 2) {
                            // nothing to do
                        } else {
                            relativeLayout.setVisibility(View.GONE);
                        }

                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

                relativeLayout.post(() -> {
                    int screenWidth = displayMetrics.widthPixels;
                    int[] location = new int[2];
                    view.getLocationOnScreen(location);
                    int absoluteX = location[0];

                    if (absoluteX < screenWidth / 2) {
                        Logger.d("ImageView is on the left side");
                    } else {
                        Logger.d("ImageView is on the right side");
                        relativeLayout.setVisibility(View.GONE);
                    }
                });
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