package com.cwuom.ouo.hooks;

import static com.cwuom.ouo.HookInit.hostApp;
import static com.cwuom.ouo.Initiator.loadClass;

import android.content.Context;

import androidx.annotation.NonNull;

import com.cwuom.ouo.Initiator;
import com.cwuom.ouo.R;
import com.cwuom.ouo.creator.OUOSetting;
import com.cwuom.ouo.startup.HookBase;
import com.cwuom.ouo.util.Logger;
import com.cwuom.ouo.util.Parasitics;
import com.cwuom.ouo.util.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import kotlin.collections.ArraysKt;
import kotlin.jvm.functions.Function0;
import lombok.Getter;

@Getter
public class MainSettingEntranceInjector implements HookBase {
    public static String method_name = "设置模块入口";

    public void init(LoadPackageParam lpparam) {
        try {
            Class<?> mainSettingConfigProviderClass = XposedHelpers.findClass("com.tencent.mobileqq.setting.main.MainSettingConfigProvider", lpparam.classLoader);
            Method[] methods = mainSettingConfigProviderClass.getDeclaredMethods();

            Method buildMethod = Arrays.stream(methods)
                    .filter(method -> List.class.isAssignableFrom(method.getReturnType()))
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);


            buildMethod.setAccessible(true);
            XposedBridge.hookMethod(buildMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    List<Object> resultList = (List<Object>) param.getResult();

                    Parasitics.injectModuleResources(context.getResources());
                    int resId = hostApp.getResources().getIdentifier("qui_tuning", "drawable", hostApp.getPackageName());


                    Class<?> simpleItemProcessorClass = loadClass("com.tencent.mobileqq.setting.processor.h");
                    Constructor<?> simpleItemProcessorConstructor = simpleItemProcessorClass.getDeclaredConstructor(Context.class, int.class, CharSequence.class, int.class);
                    Method setOnClickListener;
                    {
                        List<Method> candidates = ArraysKt.filter(simpleItemProcessorClass.getDeclaredMethods(), m -> {
                            Class<?>[] argt = m.getParameterTypes();
                            return m.getReturnType() == void.class && argt.length == 1 && Function0.class.getName().equals(argt[0].getName());
                        });
                        candidates.sort(Comparator.comparing(Method::getName));
                        if (candidates.size() != 2) {
                            throw new IllegalStateException("com.tencent.mobileqq.setting.processor.g.?(Function0)V candidates.size() != 2");
                        }
                        setOnClickListener = candidates.get(0);
                    }

                    Object simpleItemProcessor = simpleItemProcessorConstructor.newInstance(context, R.id.setting2Activity_settingEntryItem, "ouo", resId);

                    Class<?> thatFunction0 = setOnClickListener.getParameterTypes()[0];
                    Object theUnit = thatFunction0.getClassLoader().loadClass("kotlin.Unit").getField("INSTANCE").get(null);
                    ClassLoader hostClassLoader = Initiator.getHostClassLoader();
                    Context ctx = (Context) param.args[0];
                    Object func0 = Proxy.newProxyInstance(hostClassLoader, new Class<?>[]{thatFunction0}, (proxy, method, args) -> {
                        if (method.getName().equals("invoke")) {
                            onSettingEntryClick(ctx);
                            return theUnit;
                        }
                        // must be sth from Object
                        return method.invoke(this, args);
                    });
                    setOnClickListener.invoke(simpleItemProcessor, func0);

                    Class<?> groupClass = resultList.get(0).getClass();
                    Constructor<?> groupConstructor = groupClass.getConstructor(List.class, CharSequence.class, CharSequence.class, Integer.TYPE, Class.forName("kotlin.jvm.internal.DefaultConstructorMarker", true, lpparam.classLoader));
                    Object group = groupConstructor.newInstance(Collections.singletonList(simpleItemProcessor), null, null, 6, null);

                    resultList.add(0, group);
                }
            });
        } catch (Throwable e) {
            Logger.e(e + "设置创建入口失败");
        }
    }

    private void onSettingEntryClick(@NonNull Context context) {
        Utils.runOnUiThread(() -> OUOSetting.createView(Objects.requireNonNull(Utils.getActivityFromContext(context)), context));
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