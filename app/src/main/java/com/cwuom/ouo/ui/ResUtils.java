package com.cwuom.ouo.ui;

import static com.cwuom.ouo.Initiator._ThemeUtil;
import static com.cwuom.ouo.Initiator.load;
import static com.cwuom.ouo.util.AppRuntimeHelper.getAppRuntime;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import com.cwuom.ouo.reflex.Reflex;
import com.cwuom.ouo.util.Logger;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XposedHelpers;

public class ResUtils {

    private ResUtils() {
    }

    private static final Map<String, Drawable> sCachedDrawable = new HashMap<>();

    public static InputStream openAsset(String name) {
        return Objects.requireNonNull(ResUtils.class.getClassLoader()).getResourceAsStream("assets/" + name);
    }

    public static Drawable loadDrawableFromAsset(String name, Context mContext) {
        if (mContext != null) {
            return loadDrawableFromAsset(name, mContext.getResources(), mContext);
        } else {
            return loadDrawableFromAsset(name, null, null);
        }
    }

    public static Drawable loadDrawableFromStream(InputStream in, String name, @Nullable Resources res) {
        Drawable ret;
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            // qq has xhdpi
            bitmap.setDensity(320);
            byte[] chunk = bitmap.getNinePatchChunk();
            if (NinePatch.isNinePatchChunk(chunk)) {
                Class<?> clz = load("com/tencent/theme/SkinnableNinePatchDrawable");
                ret = (Drawable) XposedHelpers.findConstructorBestMatch(clz, Resources.class, Bitmap.class,
                        byte[].class, Rect.class, String.class)
                    .newInstance(res, bitmap, chunk, new Rect(), name);
            } else {
                ret = new BitmapDrawable(res, bitmap);
            }
            return ret.mutate();
        } catch (Exception e) {
            Logger.e(e);
        }
        return null;
    }

    public static Drawable loadDrawableFromAsset(String name, @Nullable Resources res, Context mContext) {
        Drawable ret;
        if ((ret = sCachedDrawable.get(name)) != null) {
            return ret;
        }
        try {
            if (res == null && mContext != null) {
                res = mContext.getResources();
            }
            InputStream fin = openAsset(name);
            ret = loadDrawableFromStream(fin, name, res);
            sCachedDrawable.put(name, ret);
            return ret;
        } catch (Exception e) {
            Logger.e(e);
        }
        return null;
    }

    public static boolean isInNightMode() {
        try {
            String themeId = (String) Reflex.invokeStatic(_ThemeUtil(),
                "getUserCurrentThemeId", getAppRuntime(), load("mqq/app/AppRuntime"));
            return "1103".endsWith(themeId) || "2920".endsWith(themeId) || "2963".endsWith(themeId);
        } catch (Exception e) {
            Logger.e(e);
            return false;
        }
    }

    public static int getNightModeMasked() {
        return isInNightMode() ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
    }
}
