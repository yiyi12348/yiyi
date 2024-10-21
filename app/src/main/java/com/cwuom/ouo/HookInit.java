package com.cwuom.ouo;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import com.cwuom.ouo.startup.QQHookLauncher;
import com.cwuom.ouo.util.Logger;
import com.cwuom.ouo.util.ModuleUtils;
import com.cwuom.ouo.reflex.XField;
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit;

import org.lsposed.hiddenapibypass.HiddenApiBypass;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookInit implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static boolean injectedQQ = false;
    public static String sModulePath = null;
    public static Application hostApp = null;
    public static Object cAIOParam;
    private static ClassLoader hostClassLoader;
    public static String cPeerUID;
    public static int cChatType = -1;
    private static boolean sSecondStageInit = false;
    private static IXposedHookZygoteInit.StartupParam sInitZygoteStartupParam = null;
    private static XC_LoadPackage.LoadPackageParam sLoadPackageParam = null;
    public static boolean isDarkMode = false;

    static {
        System.loadLibrary("dexkit");
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws ClassNotFoundException {
        sLoadPackageParam = lpparam;

        if (BuildConfig.APPLICATION_ID.equals(lpparam.packageName)) {
            try {
                Logger.d("Hooking current Xposed module status...");
                hookModuleUtils(lpparam);
            } catch (Throwable e) {
                Logger.e("Failed to hook current Xposed module status.");
            }
        }


        if (lpparam.packageName.equals("com.tencent.mobileqq") && !injectedQQ) {
            Logger.i("Successfully hooked " + lpparam.processName, true);
            hostClassLoader = lpparam.classLoader;


            String apkPath = lpparam.appInfo.sourceDir;
            try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
                findAIOParam(bridge);
            }


            XposedHelpers.findAndHookMethod(
                    "com.tencent.common.app.BaseApplicationImpl",
                    lpparam.classLoader,
                    "onCreate",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            hostApp = (Application) param.thisObject;
                        }
                    });


            XposedHelpers.findAndHookMethod(
                    Application.class,
                    "attach",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Context context = (Context) param.args[0];
                            Initiator.init(context.getClassLoader());
                        }
                    });

            XC_MethodHook startup = new XC_MethodHook(51) {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        Context app;
                        Class<?> clz = param.thisObject.getClass().getClassLoader()
                                .loadClass("com.tencent.common.app.BaseApplicationImpl");
                        Field fsApp = null;
                        for (Field f : clz.getDeclaredFields()) {
                            if (f.getType() == clz) {
                                fsApp = f;
                                break;
                            }
                        }
                        if (fsApp == null) {
                            throw new NoSuchFieldException("field BaseApplicationImpl.sApplication not found");
                        }
                        app = (Context) fsApp.get(null);
                        execStartupInit(app, param.thisObject, null, false);
                    } catch (Throwable e) {
                        Logger.e(e);
                        throw e;
                    }
                }
            };


            Class<?> loadDex = findLoadDexTaskClass(lpparam.classLoader);
            Method[] ms = loadDex.getDeclaredMethods();
            Method m = null;
            for (Method method : ms) {
                if (method.getReturnType().equals(boolean.class) && method.getParameterTypes().length == 0) {
                    m = method;
                    break;
                }
                // QQ NT: 8.9.58.11040 (4054)+
                // public void run(Context)
                if (method.getReturnType() == void.class && method.getParameterTypes().length == 1 &&
                        method.getParameterTypes()[0] == Context.class) {
                    m = method;
                    break;
                }
            }

            XposedBridge.hookMethod(m, startup);

            injectedQQ = true;
        }



    }

    private void findAIOParam(DexKitBridge bridge) {
        MethodData methodData = bridge.findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                        .usingStrings("rootVMBuild")
                )
        ).single();


        try {
            Method method = methodData.getMethodInstance(hostClassLoader);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Bundle bundle = (Bundle) param.args[0];
                    cAIOParam = bundle.getParcelable("aio_param");

                    Object AIOSession = XField.obj(cAIOParam).type(Initiator.loadClass("com.tencent.aio.data.AIOSession")).get();
                    Object AIOContact = XField.obj(AIOSession).type(Initiator.loadClass("com.tencent.aio.data.AIOContact")).get();
                    cPeerUID = XField.obj(AIOContact).name("f").type(String.class).get();
                    cChatType = XField.obj(AIOContact).name("e").type(int.class).get();
                }
            });
        } catch (Exception e) {
            Logger.e("err:"+e);
        }
    }

    private void hookModuleUtils(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String className = ModuleUtils.class.getName();

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader,
                "getModuleVersion",
                XC_MethodReplacement.returnConstant(BuildConfig.VERSION_CODE));
    }

    @Override
    public void initZygote(StartupParam startupParam) {
        sInitZygoteStartupParam = startupParam;
        sModulePath = startupParam.modulePath;
    }

    private static Class<?> findLoadDexTaskClass(ClassLoader cl) throws ClassNotFoundException {
        try {
            return cl.loadClass("com.tencent.mobileqq.startup.step.LoadDex");
        } catch (ClassNotFoundException ignored) {
            // ignore
        }
        // for NT QQ
        // TODO: 2023-04-19 'com.tencent.mobileqq.startup.task.config.a' is not a good way to find the class
        Class<?> kTaskFactory = cl.loadClass("com.tencent.mobileqq.startup.task.config.a");
        Class<?> kITaskFactory = cl.loadClass("com.tencent.qqnt.startup.task.d");
        // check cast so that we can sure that we have found the right class
        if (!kITaskFactory.isAssignableFrom(kTaskFactory)) {
            kTaskFactory = cl.loadClass("com.tencent.mobileqq.startup.task.config.b");
            if (!kITaskFactory.isAssignableFrom(kTaskFactory)) {
                throw new AssertionError(kITaskFactory + " is not assignable from " + kTaskFactory);
            }
        }
        Field taskClassMapField = null;
        for (Field field : kTaskFactory.getDeclaredFields()) {
            if (field.getType() == HashMap.class && Modifier.isStatic(field.getModifiers())) {
                taskClassMapField = field;
                break;
            }
        }
        if (taskClassMapField == null) {
            throw new AssertionError("taskClassMapField not found");
        }
        taskClassMapField.setAccessible(true);
        HashMap<String, Class<?>> taskClassMap;
        try {
            // XXX: this will cause <clinit>() to be called, check whether it will cause any problem
            taskClassMap = (HashMap<String, Class<?>>) taskClassMapField.get(null);
        } catch (IllegalAccessException e) {
            // should not happen
            throw new AssertionError(e);
        }
        assert taskClassMap != null;
        Class<?> loadDexTaskClass = taskClassMap.get("LoadDexTask");
        if (loadDexTaskClass == null) {
            throw new AssertionError("loadDexTaskClass not found");
        }
        return loadDexTaskClass;
    }

    public static void execStartupInit(Context ctx, Object step, String lpwReserved, boolean bReserved) {
        if (sSecondStageInit) {
            return;
        }
        ClassLoader classLoader = ctx.getClassLoader();
        if (classLoader == null) {
            throw new AssertionError("ERROR: classLoader == null");
        }
        if ("true".equals(System.getProperty(HookInit.class.getName()))) {  // reload??
            return;
        }
        System.setProperty(HookInit.class.getName(), "true");
        injectClassLoader(classLoader);
        execPostStartupInit(ctx, step, lpwReserved, bReserved);
        sSecondStageInit = true;
        Logger.i("Startup initialization executed successfully in <com.tencent.mobileqq>!");
    }

    public static void execPostStartupInit(Context ctx, Object step, String lpwReserved, boolean bReserved) {
        ensureHiddenApiAccess();
        // init all kotlin utils here
        EzXHelperInit.INSTANCE.initZygote(getInitZygoteStartupParam());
        EzXHelperInit.INSTANCE.initHandleLoadPackage(getLoadPackageParam());
        // resource injection is done somewhere else, do not init it here
        EzXHelperInit.INSTANCE.initAppContext(ctx, false, false);
        EzXHelperInit.INSTANCE.setLogTag("ouom!");
        HostInfo.init((Application) ctx);
        Initiator.init(ctx.getClassLoader());

        QQHookLauncher launcher = new QQHookLauncher();
        launcher.init(sLoadPackageParam);
    }

    private static void ensureHiddenApiAccess() {
        if (!isHiddenApiAccessible()) {
            android.util.Log.w("ouom!", "Hidden API access not accessible, SDK_INT is " + Build.VERSION.SDK_INT);
            HiddenApiBypass.setHiddenApiExemptions("L");
        }
    }



    @SuppressLint({"BlockedPrivateApi", "PrivateApi"})
    public static boolean isHiddenApiAccessible() {
        Class<?> kContextImpl;
        try {
            kContextImpl = Class.forName("android.app.ContextImpl");
        } catch (ClassNotFoundException e) {
            return false;
        }
        Field mActivityToken = null;
        Field mToken = null;
        try {
            mActivityToken = kContextImpl.getDeclaredField("mActivityToken");
        } catch (NoSuchFieldException ignored) {
        }
        try {
            mToken = kContextImpl.getDeclaredField("mToken");
        } catch (NoSuchFieldException ignored) {
        }
        return mActivityToken != null || mToken != null;
    }



    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("DiscouragedPrivateApi")
    private static void injectClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new NullPointerException("classLoader == null");
        }
        try {
            Field fParent = ClassLoader.class.getDeclaredField("parent");
            fParent.setAccessible(true);
            ClassLoader mine = HookInit.class.getClassLoader();
            ClassLoader curr = (ClassLoader) fParent.get(mine);
            if (curr == null) {
                curr = XposedBridge.class.getClassLoader();
            }
            if (!curr.getClass().getName().equals(HybridClassLoader.class.getName())) {
                fParent.set(mine, new HybridClassLoader(curr, classLoader));
            }
        } catch (Exception e) {
            Logger.e(e);
        }
    }

    /**
     * Get the {@link IXposedHookZygoteInit.StartupParam} of the current module.
     * <p>
     * Do NOT add @NonNull annotation to this method. *** No kotlin code should be invoked here.*** May cause a crash.
     *
     * @return the initZygote param
     */
    public static IXposedHookZygoteInit.StartupParam getInitZygoteStartupParam() {
        if (sInitZygoteStartupParam == null) {
            throw new IllegalStateException("InitZygoteStartupParam is null");
        }
        return sInitZygoteStartupParam;
    }

    public static XC_LoadPackage.LoadPackageParam getLoadPackageParam() {
        if (sLoadPackageParam == null) {
            throw new IllegalStateException("LoadPackageParam is null");
        }
        return sLoadPackageParam;
    }

}