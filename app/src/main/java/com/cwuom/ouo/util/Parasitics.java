package com.cwuom.ouo.util;

import static com.cwuom.ouo.Initiator.getHostClassLoader;
import static com.cwuom.ouo.util.Utils.getModulePath;
import static com.cwuom.ouo.util.Utils.runOnUiThread;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.loader.ResourcesLoader;
import android.content.res.loader.ResourcesProvider;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.cwuom.ouo.R;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

public class Parasitics {
    private static long sResInjectEndTime = 0;

    public static void injectModuleResources(Resources res) {
        if (res == null) {
            return;
        }
        // FIXME: 去除资源注入成功检测，每次重复注入资源，可以修复一些内置 Hook 框架注入虽然成功但是依然找不到资源 ID 的问题
        //        复现：梦境框架、应用转生、LSPatch，QQ 版本 8.3.9、8.4.1
        // TODO: 2024-03-25 测试 Android 11 上是否存在小方说的问题
        try {
            res.getString(R.string.res_inject_success);
            return;
        } catch (Resources.NotFoundException ignored) {
        }
        String sModulePath = getModulePath();
        if (sModulePath == null) {
            throw new RuntimeException("get module path failed, loader=" + getHostClassLoader());
        }
        // AssetsManager.addAssetPath starts to break on Android 12.
        // ResourcesLoader is added since Android 11.
        if (Build.VERSION.SDK_INT >= 30) {
            injectResourcesAboveApi30(res, sModulePath);
        } else {
            injectResourcesBelowApi30(res, sModulePath);
        }
    }

    @RequiresApi(30)
    private static void injectResourcesAboveApi30(@NonNull Resources res, @NonNull String path) {
        if (ResourcesLoaderHolderApi30.sResourcesLoader == null) {
            try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(new File(path),
                    ParcelFileDescriptor.MODE_READ_ONLY)) {
                ResourcesProvider provider = ResourcesProvider.loadFromApk(pfd);
                ResourcesLoader loader = new ResourcesLoader();
                loader.addProvider(provider);
                ResourcesLoaderHolderApi30.sResourcesLoader = loader;
            } catch (IOException e) {
                logForResourceInjectFaulure(path, e, 0);
                return;
            }
        }
        runOnUiThread(() -> {
            res.addLoaders(ResourcesLoaderHolderApi30.sResourcesLoader);
            try {
                res.getString(R.string.res_inject_success);
                if (sResInjectEndTime == 0) {
                    sResInjectEndTime = System.currentTimeMillis();
                }
            } catch (Resources.NotFoundException e) {
                logForResourceInjectFaulure(path, e, 0);
            }
        });


    }

    @RequiresApi(30)
    private static class ResourcesLoaderHolderApi30 {

        private ResourcesLoaderHolderApi30() {
        }

        public static ResourcesLoader sResourcesLoader = null;

    }

    @SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
    private static void injectResourcesBelowApi30(@NonNull Resources res, @NonNull String path) {
        try {
            AssetManager assets = res.getAssets();
            Method addAssetPath = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
            addAssetPath.setAccessible(true);
            int cookie = (int) addAssetPath.invoke(assets, path);
            try {
                res.getString(R.string.res_inject_success);
                if (sResInjectEndTime == 0) {
                    sResInjectEndTime = System.currentTimeMillis();
                }
            } catch (Resources.NotFoundException e) {
                logForResourceInjectFaulure(path, e, 0);
            }
        } catch (Exception e) {
            Logger.e(e);
        }
    }

    private static void logForResourceInjectFaulure(@NonNull String path, @NonNull Throwable e, int cookie) {
        Logger.e("Fatal: injectModuleResources: test injection failure!");
        Logger.e("injectModuleResources: path=" + path + ", cookie=" + cookie +
                ", loader=" + getHostClassLoader());
        long length;
        boolean read;
        boolean exist;
        boolean isDir;
        File f = new File(path);
        exist = f.exists();
        isDir = f.isDirectory();
        length = f.length();
        read = f.canRead();
        Logger.e("sModulePath: exists = " + exist + ", isDirectory = " + isDir + ", canRead = " + read + ", fileLength = " + length);
    }

}
