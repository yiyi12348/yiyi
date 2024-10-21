package com.cwuom.ouo.creator;

import static com.cwuom.ouo.builder.MsgBuilder.nt_build_text;
import static com.cwuom.ouo.util.Session.getContact;
import static com.cwuom.ouo.util.Session.getCurrentChatType;
import static com.cwuom.ouo.util.Session.getCurrentPeerID;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cwuom.ouo.R;
import com.cwuom.ouo.bridge.Nt_kernel_bridge;
import com.cwuom.ouo.bridge.kernelcompat.ContactCompat;
import com.cwuom.ouo.helper.ElemHelperKt;
import com.cwuom.ouo.hooks.Toasts;
import com.cwuom.ouo.util.CommonContextWrapper;
import com.cwuom.ouo.util.Logger;
import com.cwuom.ouo.util.Utils;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.core.BottomPopupView;
import com.lxj.xpopup.util.XPopupUtils;
import com.tencent.qqnt.kernel.nativeinterface.ArkElement;
import com.tencent.qqnt.kernel.nativeinterface.MsgElement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@SuppressLint("ResourceType")
public class OUOSetting extends BottomPopupView {
    private static View decorView;

    public OUOSetting(@NonNull Context context) {
        super(context);
    }

    public static void createView(Activity activity, Context context) {
        Context fixContext = CommonContextWrapper.createAppCompatContext(context);
        XPopup.Builder NewPop = new XPopup.Builder(fixContext).moveUpToKeyboard(false).isDestroyOnDismiss(true);
        NewPop.maxHeight((int) (XPopupUtils.getScreenHeight(context) * .7f));
        NewPop.popupHeight((int) (XPopupUtils.getScreenHeight(context) * .65f));


        decorView = activity.getWindow().getDecorView();
        animateBlurEffect(decorView);

        BasePopupView popupView = NewPop.asCustom(new OUOSetting(fixContext));
        popupView.show();
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate() {
        super.onCreate();

        Button btnQQGroup = findViewById(R.id.btn_qq_group);
        Button btnTgGroup = findViewById(R.id.btn_tg_group);
        Button btnGitHub = findViewById(R.id.btn_github);

        btnQQGroup.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Utils.jump(v, this.hashCode(), "https://qm.qq.com/q/INFnke6R6a");
                } catch (ClassNotFoundException e) {
                    Toasts.error(v.getContext(), "无法跳转内置浏览器");
                }
            }
        });

        btnTgGroup.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ouom_pub"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            v.getContext().startActivity(intent);
        });

        btnGitHub.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/ouom_pub"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            v.getContext().startActivity(intent);
        });
    }

    private static void animateBlurEffect(View decorView) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            return;
        }

        ValueAnimator blurAnimator = ValueAnimator.ofFloat(1f, 25f);
        blurAnimator.setDuration(200);
        blurAnimator.setInterpolator(new AccelerateInterpolator());
        blurAnimator.addUpdateListener(animation -> {
            float blurRadius = (float) animation.getAnimatedValue();
            decorView.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP));
        });
        blurAnimator.start();
    }

    private static void fadeOutAndClearBlur(View decorView) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) {
            return;
        }

        ValueAnimator fadeOutAnimator = ValueAnimator.ofFloat(25f, 1f);
        fadeOutAnimator.setDuration(100);
        fadeOutAnimator.setInterpolator(new DecelerateInterpolator());
        fadeOutAnimator.addUpdateListener(animation -> {
            float blurRadius = (float) animation.getAnimatedValue();
            decorView.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP));
        });

        fadeOutAnimator.addListener(new AnimatorListenerAdapter() {
            @RequiresApi(api = Build.VERSION_CODES.S)
            @Override
            public void onAnimationEnd(Animator animation) {
                decorView.setRenderEffect(null);
            }
        });

        fadeOutAnimator.start();
    }

    @Override
    protected void beforeDismiss() {
        fadeOutAndClearBlur(decorView);
        super.beforeDismiss();
    }

    @Override
    protected void onDismiss() {
        fadeOutAndClearBlur(decorView);
        super.onDismiss();
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.layout_setting;
    }
}


