package com.cwuom.ouo.hooks;

import static com.cwuom.ouo.Initiator.loadClass;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.cwuom.ouo.creator.ElementSender;
import com.cwuom.ouo.startup.HookBase;
import com.cwuom.ouo.util.Logger;
import com.cwuom.ouo.util.Utils;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import lombok.Getter;

@Getter
@SuppressLint("DiscouragedApi")
public class HookInputRoot implements HookBase {
    public static String method_name = "劫持发送按钮";

    public void findInputRoot(DexKitBridge bridge, ClassLoader hostClassLoader) {
        MethodData methodData = bridge.findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                        .usingStrings("inputRoot.findViewById(R.id.send_btn)")
                )
        ).single();


        try {
            Method method = methodData.getMethodInstance(hostClassLoader);
            XposedBridge.hookMethod(method, new XC_MethodHook(40) {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);

                    Button sendBtn = null;
                    EditText editText = null;
                    ViewGroup inputRoot = null;
                    Field[] fs = param.thisObject.getClass().getDeclaredFields();
                    for (Field f : fs) {
                        Class<?> type = f.getType();
//                        Logger.d("type::"+type);
                        if (type.equals(android.widget.Button.class)) {
                            f.setAccessible(true);
                            sendBtn = (Button) f.get(param.thisObject);
                            Logger.d(String.valueOf(f.get(param.thisObject)));
                            Logger.d("found input send btn");
                        } else if (type.equals(android.widget.EditText.class)) {
                            f.setAccessible(true);
                            editText = (EditText) f.get(param.thisObject);
                            Logger.d("found input editText");
                        } else if (type.equals(android.view.ViewGroup.class)) {
                            f.setAccessible(true);
                            Logger.d("found input ViewGroup");
                            inputRoot = (ViewGroup) f.get(param.thisObject);
                        }
                    }


                    if (sendBtn != null && editText != null && inputRoot != null){
                        EditText finalEditText = editText;
                        sendBtn.setOnLongClickListener(v -> {
                            Utils.runOnUiThread(() -> ElementSender.createView(Objects.requireNonNull(Utils.getActivityFromView(v)), v.getContext(), finalEditText.getText().toString()));

                            return true;
                        });
                        EditText finalEditText1 = editText;
                        editText.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                            }

                            @Override
                            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                                finalEditText1.setFilters( new InputFilter[]{ new InputFilter.LengthFilter(Integer.MAX_VALUE)});
                            }

                            @Override
                            public void afterTextChanged(Editable editable) {

                            }
                        });

                    }
                }
            });
        } catch (Exception e) {
            Logger.e("err:"+e);
        }
    }

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        String apkPath = lpparam.appInfo.sourceDir;
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            findInputRoot(bridge, lpparam.classLoader);
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