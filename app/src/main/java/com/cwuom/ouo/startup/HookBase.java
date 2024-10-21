package com.cwuom.ouo.startup;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

public interface HookBase {
    void init(XC_LoadPackage.LoadPackageParam lpparam);
    String getName();
    Boolean isEnable();
}
