package com.cwuom.ouo.hooks;

import static com.cwuom.ouo.Initiator.loadClass;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import com.cwuom.ouo.Initiator;
import com.cwuom.ouo.startup.HookBase;
import com.cwuom.ouo.util.CommonContextWrapper;
import com.cwuom.ouo.util.Logger;
import com.cwuom.ouo.util.Utils;
import com.lxj.xpopup.XPopup;

import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import lombok.Getter;

@Getter
@SuppressLint("DiscouragedApi")
public class HookProfileCard implements HookBase {
    public static String method_name = "劫持资料卡";
    private static String QQ;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());


    protected void hookMethod(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> clazz = loadClass("com.tencent.mobileqq.profilecard.activity.FriendProfileCardActivity");
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

    private void hookTargetActivity(Activity activity) {
        Utils.postDelayed(() -> {
            new Thread(() -> {
                String qqNumber = null;
                try {
                    TextView tv = activity.findViewById(activity.getResources().getIdentifier("gmx", "id", activity.getPackageName()));
                    String text = tv.getText().toString();
                    qqNumber = text.replace("QQ号：", "").replaceAll("[^0-9]", "");
                } catch (Exception e) {
                    try {
                        TextView tv = activity.findViewById(activity.getResources().getIdentifier("info", "id", activity.getPackageName()));
                        String text = tv.getText().toString();
                        qqNumber = text.replace("QQ号：", "").replaceAll("[^0-9]", "");
                    } catch (Exception ex) {
                        Logger.e("无法获取QQ号: " + ex, true);
                    }
                }

                if (qqNumber != null) {
                    QQ = qqNumber;
                }

                View setting = null;
                try {
                    setting = Utils.getViewByDesc(activity, "设置", 200);
                } catch (InterruptedException e) {
                    Logger.e("查找设置按钮时出错: " + e, true);
                }

                if (setting != null) {
                    View finalSetting = setting;
                    mainHandler.post(() -> {
                        try {
                            finalSetting.setOnLongClickListener(new onSettingLongClickListener());
                        } catch (Exception ignored) {}
                    });
                } else {
                    Logger.e("设置按钮未找到", true);
                }
            }).start();
        }, 100);
    }


    private static class onSettingLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            Context fixContext = CommonContextWrapper.createAppCompatContext(v.getContext());
            new XPopup.Builder(fixContext)
                    .asCenterList("你要干啥？", new String[]{"看TA的小世界！", "看看TA的等级！", "不干啥"},
                            (position, text) -> {
                                switch (position) {
                                    case 0:
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mqqapi://qcircle/openmainpage?uin=" + QQ));
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        v.getContext().startActivity(intent);
                                        break;
                                    case 1:
                                        try {
                                            Utils.jump(v, this.hashCode(), "https://club.vip.qq.com/card/friend?qq=" + QQ);
                                        } catch (Exception e) {
                                            Toasts.error(v.getContext(), "无法打开内置浏览器");
                                        }
                                        break;
                                }
                            })
                    .show();
            return true;
        }
    }

    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookMethod(lpparam);
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