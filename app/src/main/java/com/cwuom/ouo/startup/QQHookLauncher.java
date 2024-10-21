package com.cwuom.ouo.startup;

import com.cwuom.ouo.hooks.HookAvatar;
import com.cwuom.ouo.hooks.HookBtnMore;
import com.cwuom.ouo.hooks.HookInputRoot;
import com.cwuom.ouo.hooks.HookLengthFilter;
import com.cwuom.ouo.hooks.HookProfileCard;
import com.cwuom.ouo.hooks.HookUncaughtExceptionHandler;
import com.cwuom.ouo.hooks.MainSettingEntranceInjector;
import com.cwuom.ouo.util.Logger;
import com.cwuom.ouo.hooks.Toasts;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class QQHookLauncher {
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        List<HookBase> hooks = new ArrayList<>();

        hooks.add(new Toasts());
        hooks.add(new HookInputRoot());
        hooks.add(new HookBtnMore());
//        hooks.add(new HookTextView());
        hooks.add(new HookUncaughtExceptionHandler());
        hooks.add(new HookAvatar());
        hooks.add(new HookProfileCard());
        hooks.add(new MainSettingEntranceInjector());
        hooks.add(new HookLengthFilter());


        for (HookBase hook : hooks) {
            try {
                Logger.i(String.format("Initializing %s...", hook.getName()));
                hook.init(lpparam);
            } catch (Exception e) {
                Logger.e("cannot load " + hook.getName(), e);
            }
        }

    }
}

