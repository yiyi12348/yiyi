package com.cwuom.ouo.hooks;

import static com.cwuom.ouo.util.Check.isValidInteger;
import static com.cwuom.ouo.util.Utils.replaceInvalidLinks;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.cwuom.ouo.startup.HookBase;
import com.cwuom.ouo.util.Logger;
import com.tencent.qqnt.kernel.nativeinterface.FaceBubbleElement;
import com.tencent.qqnt.kernel.nativeinterface.MarkdownElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;
import com.tencent.qqnt.kernel.nativeinterface.SmallYellowFaceInfo;
import com.tencent.qqnt.kernel.nativeinterface.TextElement;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import lombok.Getter;


@SuppressLint("DiscouragedApi")
@Getter
public class HookUncaughtExceptionHandler implements HookBase {
    public static String method_name = "拦截部分闪退";

    public void doHook(ClassLoader classLoader) {
        findAndHookMethod(
                "java.lang.Thread",
                classLoader,
                "setDefaultUncaughtExceptionHandler",
                Thread.UncaughtExceptionHandler.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Thread.UncaughtExceptionHandler originalHandler = (Thread.UncaughtExceptionHandler) param.args[0];
                        param.args[0] = (Thread.UncaughtExceptionHandler) (t, e) -> {
                        };
                    }
                }
        );


        XposedHelpers.findAndHookMethod("com.tencent.ecommerce.biz.router.ECScheme", classLoader, "l", android.net.Uri.class,"com.tencent.ecommerce.base.router.api.IECSchemeCallback", boolean.class, java.util.Map.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                String uri = String.valueOf(param.args[0]);
                if (!isValidInteger(uri.replace("mqqapi://ecommerce/open?target=", ""))){
                    param.setResult(0);
                }
                super.beforeHookedMethod(param);
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
            }
        });



        findAndHookMethod(
                "com.tencent.mobileqq.emoticon.QQSysFaceUtil",
                classLoader,
                "getFaceDrawableFromLocal",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int index = (int) param.args[0];
                        if (index < 0 || index >= 360) {
                            param.setResult(null);
                        }
                    }
                }
        );

        findAndHookMethod("com.tencent.mobileqq.aio.msg.FaceBubbleMsgItem$msgElement$2", classLoader, "invoke", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.getResult() != null){
                    super.afterHookedMethod(param);
                    return;
                }
                MsgElement msgElement = new MsgElement();
                FaceBubbleElement faceBubbleElement = new FaceBubbleElement();
                SmallYellowFaceInfo smallYellowFaceInfo = new SmallYellowFaceInfo();
                smallYellowFaceInfo.setIndex(187);
                smallYellowFaceInfo.setText("幽灵");
                smallYellowFaceInfo.setCompatibleText("幽灵");
                faceBubbleElement.setFaceType(13);
                faceBubbleElement.setFaceCount(0);
                faceBubbleElement.setFaceFlag(0);
                faceBubbleElement.setFaceSummary("幽灵");
                faceBubbleElement.setContent("[幽灵]x0");
                faceBubbleElement.setYellowFaceInfo(smallYellowFaceInfo);
                msgElement.setElementType(27);
                msgElement.setFaceBubbleElement(faceBubbleElement);
                param.setResult(msgElement);

            }
        });




    }

    public static void findMarkdownAIO(DexKitBridge bridge, ClassLoader classLoader) {
        MethodData methodData = bridge.findMethod(FindMethod.create()
                .matcher(MethodMatcher.create()
                        .usingStrings("AIOMarkdownContentComponent")
                        .usingStrings("bind status=")
                        .paramCount(2)
                )
        ).single();


        try {
            Method method = methodData.getMethodInstance(classLoader);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    String content = ((MarkdownElement)param.args[0]).getContent();
                    String[] replacedMessage = replaceInvalidLinks(content);
                    Logger.d("replacedMessage\n" + replacedMessage[0]);
                    String content_replaced = replacedMessage[0];
                    if (Boolean.parseBoolean(replacedMessage[1])){
                        content_replaced = content_replaced.replace("`", "^");
                        content_replaced += "\n***\n# 以下消息来自ouo!\n- 提示: 此markdown不合法！已阻止资源加载\n- 原因：此消息内包含了一个或多个非官方的图片资源链接。\ncontent:\n```markdown\n"+content+"```";
                    }
                    MarkdownElement markdownElement = new MarkdownElement(content_replaced);
                    param.args[0] = markdownElement;
                }
            });
        } catch (Exception e) {
            Log.e("ouom","err:"+e);
        }
    }
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        String apkPath = lpparam.appInfo.sourceDir;
        try (DexKitBridge bridge = DexKitBridge.create(apkPath)) {
            findMarkdownAIO(bridge, lpparam.classLoader);
        }

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