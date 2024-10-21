package com.cwuom.ouo.util;

import static com.cwuom.ouo.HookInit.hostApp;
import static com.cwuom.ouo.HookInit.sModulePath;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.cwuom.ouo.Initiator;
import com.tencent.qqnt.kernel.nativeinterface.MsgAttributeInfo;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgAvatarPendant;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgBubble;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgElement;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgFont;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgIceBreak;
import com.tencent.qqnt.kernel.nativeinterface.VASMsgNamePlate;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;

public class Utils {
    private static Handler sHandler;

    public static List<View> getAllViews(Activity act) {
        return getAllChildViews(act.getWindow().getDecorView());
    }

    private static List<View> getAllChildViews(View view) {
        List<View> allChildren = new ArrayList<>();
        if (view instanceof ViewGroup) {
            ViewGroup vp = (ViewGroup) view;
            for (int i = 0; i < vp.getChildCount(); i++) {
                View viewChild = vp.getChildAt(i);
                allChildren.add(viewChild);
                allChildren.addAll(getAllChildViews(viewChild));
            }
        }
        return allChildren;
    }

    public static View getViewByDesc(Activity act, String desc, int limit) throws InterruptedException {
        for (int x = 0; x < limit; x++){
            for (View view : getAllViews(act)) {
                try {
                    if (view.getContentDescription().equals(desc)) {
                        return view;
                    }
                } catch (Exception ignored) {}

            }
            Thread.sleep(200);
        }



        return null;
    }

    public static View getViewByDesc(Activity act, String desc) {
        try {
            for (View view : getAllViews(act)) {
                try {
                    if (view.getContentDescription().equals(desc)) {
                        return view;
                    }
                } catch (Exception ignored) {}

            }
        } catch (Exception e) {
            Logger.e(e);
        }


        return null;
    }

    public static void printIntentExtras(String TAG, Intent intent) {
        if (intent == null) {
            Logger.e("Intent is null or has no extras.");
            return;
        }

        Logger.e("*-------------------- " + TAG + " --------------------*");
        Bundle extras = intent.getExtras();
        if (extras != null) {
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Logger.d(key + " = " + Objects.requireNonNull(value) + "(" + value.getClass() + ")");
            }
        } else {
            Logger.d("No extras found in the Intent.");
        }

        Logger.e("^-------------------- " + "OVER~" + " --------------------^");
    }

    @SuppressLint("LambdaLast")
    public static void postDelayed(@NonNull Runnable r, long ms) {
        if (sHandler == null) {
            sHandler = new Handler(Looper.getMainLooper());
        }
        sHandler.postDelayed(r, ms);
    }

    public static void post(@NonNull Runnable r) {
        postDelayed(r, 0L);
    }

    public static void post(@NonNull Runnable r, long t) {
        postDelayed(r, t);
    }

    public static void runOnUiThread(@NonNull Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            post(r);
        }
    }

    public static void runOnUiThreadDelayed(@NonNull Runnable r, long t) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            post(r, t);
        }
    }

    public interface BeforeHookedMethod {

        void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable;
    }

    public interface AfterHookedMethod {

        void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable;
    }

    public interface BeforeAndAfterHookedMethod {

        void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable;

        void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable;
    }

    public static XC_MethodHook afterAlways(int priority,
                                            final @NonNull AfterHookedMethod afterHookedMethod) {
        return new XC_MethodHook(priority) {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                afterHookedMethod.afterHookedMethod(param);
            }
        };
    }

    public static String getModulePath() {
        if (sModulePath == null) {
            throw new IllegalStateException("Module path is null");
        }
        return sModulePath;
    }

    public static Application getHostApp() {
        if (hostApp == null) {
            throw new IllegalStateException("hostApp is null");
        }
        return hostApp;
    }

    public static MsgAttributeInfo getDefaultAttributeInfo() {

        VASMsgNamePlate plate = new VASMsgNamePlate(258, 64, 0, 0, 0, 0, 258, 0, new ArrayList<>(), 0, 0);
        VASMsgBubble bubble = new VASMsgBubble(0, 0, 0, 0);
        VASMsgFont font = new VASMsgFont(65536, 0L, 0, 0, 0);
        VASMsgAvatarPendant pendant = new VASMsgAvatarPendant();
        VASMsgIceBreak iceBreak = new VASMsgIceBreak(null, null);
        VASMsgElement element = new VASMsgElement(plate, bubble, pendant, font, iceBreak);
        try {
            Class<?> msgAttributeInfoClazz = Initiator.load("com.tencent.qqnt.kernel.nativeinterface.MsgAttributeInfo");
            if (msgAttributeInfoClazz != null) {
                for (Constructor<?> constructor : msgAttributeInfoClazz.getDeclaredConstructors()) {
                    if (constructor.getParameterTypes().length < 1) continue;
                    List<Object> args = new ArrayList<>();
                    args.add(0);
                    args.add(0);
                    args.add(element);
                    for (int i = 0; i < constructor.getParameterTypes().length - 3; i++) {
                        args.add(null);
                    }
                    return (MsgAttributeInfo) constructor.newInstance(args.toArray());
                }
            }
        } catch (Throwable e) {
            Logger.e(e);
        }
        return null;
    }

    public static String[] replaceInvalidLinks(String message) {
        String regex = "!\\[.*?\\]\\((https?://.*?)\\)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);

        StringBuffer result = new StringBuffer();
        boolean isReplaced = false;

        String fullUrl = "";
        while (matcher.find()) {
            fullUrl = matcher.group(1);
            if (!fullUrl.startsWith("https://qqbot.ugcimg.cn/")) {
                matcher.appendReplacement(result, matcher.group(0).replace(fullUrl, "[非法链接]"));
                isReplaced = true;
            } else {
                matcher.appendReplacement(result, matcher.group(0));
            }
        }
        matcher.appendTail(result);
        return new String[]{result.toString(), String.valueOf(isReplaced), fullUrl};
    }

    public static Activity getActivityFromView(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }


    public static Activity getActivityFromContext(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    public static void jump(View v, int hashCode, String webUrl) throws ClassNotFoundException {
        Class<?> browserClass = Initiator.loadClass("com.tencent.mobileqq.activity.QQBrowserDelegationActivity");
        Intent browserIntent = new Intent(v.getContext(), browserClass);

        browserIntent.putExtra("fling_action_key", 2);
        browserIntent.putExtra("fling_code_key", hashCode);
        browserIntent.putExtra("useDefBackText", true);
        browserIntent.putExtra("param_force_internal_browser", true);
        browserIntent.putExtra("url", webUrl);

        v.getContext().startActivity(browserIntent);
    }

}
