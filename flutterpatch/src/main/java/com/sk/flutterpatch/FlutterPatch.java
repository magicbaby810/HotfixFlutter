package com.sk.flutterpatch;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.tinker.lib.tinker.Tinker;
import com.tencent.tinker.lib.tinker.TinkerLoadResult;
import com.tencent.tinker.lib.util.TinkerLog;
import com.tencent.tinker.loader.shareutil.ShareConstants;
import com.tencent.tinker.loader.shareutil.SharePatchFileUtil;

import java.io.File;
import java.lang.reflect.Field;

import io.flutter.FlutterInjector;
import io.flutter.embedding.engine.loader.FlutterApplicationInfo;
import io.flutter.embedding.engine.loader.FlutterLoader;


/**
 * 利用反射把tinker和sophix生成的flutter补丁，打进flutter的so加载流程中
 *
 */
public class FlutterPatch {

    private static final String TAG = "Tinker";
    private static String libPathFromSophix = "";

    private static boolean isUseSophix = false;
    private static boolean isUseTinker = false;


    private FlutterPatch() {
    }

    public static String getLibPath(Context context) {

        String libPath = "";

        if (Build.VERSION.SDK_INT >= 21) {

            for (String cpuABI : Build.SUPPORTED_ABIS) {
                if (!TextUtils.isEmpty(cpuABI)) {

                    libPath = findLibraryFromTinker(context, "lib" + File.separator + cpuABI, "libapp.so");
                    if (!TextUtils.isEmpty(libPath) && !libPath.equals("libapp.so")) {
                        TinkerLog.i(TAG, "cpu abi is:" + cpuABI);
                        break;
                    }
                }
            }
        } else {

            libPath = findLibraryFromTinker(context, "lib" + File.separator + Build.CPU_ABI, "libapp.so");
            if (!TextUtils.isEmpty(libPath) && !libPath.equals("libapp.so")) {
                TinkerLog.i(TAG, "cpu abi is:" + Build.CPU_ABI);
            }
        }
        return libPath;
    }


    public static void reflect(String libPath) {
        try {
            FlutterLoader flutterLoader = FlutterInjector.instance().flutterLoader();

            Field field = FlutterLoader.class.getDeclaredField("flutterApplicationInfo");
            field.setAccessible(true);

            FlutterApplicationInfo flutterApplicationInfo = (FlutterApplicationInfo) field.get(flutterLoader);

            Field aotSharedLibraryNameField = FlutterApplicationInfo.class.getDeclaredField("aotSharedLibraryName");
            aotSharedLibraryNameField.setAccessible(true);
            aotSharedLibraryNameField.set(flutterApplicationInfo, libPath);

            field.set(flutterLoader, flutterApplicationInfo);

            TinkerLog.i(TAG, "flutter patch is loaded successfully");

        } catch (Exception e) {
            TinkerLog.i(TAG, "flutter reflect is failed");
            e.printStackTrace();
        }
    }

    /**
     *
     * 插桩方法
     * 此方法不可修改，否则不会成功
     *
     * Tinker和Sophix都集成这种情况是不可能发生的吧？
     *
     * @param obj
     */
    public static void hook(Object obj) {
        if (obj instanceof Context) {

            Context context = (Context) obj;
            TinkerLog.i(TAG, "find FlutterMain");

            if (isUseTinker) {

                String libPathFromTinker = getLibPath(context);
                if (!TextUtils.isEmpty(libPathFromTinker)) {
                    reflect(libPathFromTinker);
                }
            } else if (isUseSophix) {

                if (!TextUtils.isEmpty(libPathFromSophix)) {
                    reflect(libPathFromSophix);
                }
            } else {
                TinkerLog.i(TAG, "lib path is null");
            }

        } else {

            TinkerLog.i(TAG, "Object: " + obj.getClass().getName());
        }

    }

    /**
     * Sophix 插桩方法，获取sophix so包路径
     *
     * 此方法不可修改，否则不会成功
     * @param obj
     */
    public static void hookSophix(Object obj) {

        if (null != obj) {
            File file = new File(obj.toString()+"/libs/libapp.so");
            if (file.exists() && !file.isDirectory()) {
                libPathFromSophix = file.getAbsolutePath();
                TinkerLog.i(TAG, "path is " + libPathFromSophix);
            } else {
                TinkerLog.i(TAG, "path file is not exist");
            }
        }
    }

    /**
     * Sophix 插桩方法，获取项目是否使用sophix
     *
     * 此方法不可修改，否则不会成功
     */
    public static void hookIsUseSophix() {
        isUseSophix = true;
        TinkerLog.i(TAG, "is use sophix");
    }

    /**
     * Sophix 插桩方法，获取项目是否使用Tinker
     *
     * 此方法不可修改，否则不会成功
     */
    public static void hookIsUseTinker() {
        isUseTinker = true;
        TinkerLog.i(TAG, "is use tinker");
    }


    public static String findLibraryFromTinker(Context context, String relativePath, String libName) throws UnsatisfiedLinkError {
        final Tinker tinker = Tinker.with(context);

        libName = libName.startsWith("lib") ? libName : "lib" + libName;
        libName = libName.endsWith(".so") ? libName : libName + ".so";
        String relativeLibPath = relativePath + File.separator + libName;

        TinkerLog.i(TAG, "flutterPatchInit() called   " + tinker.isTinkerLoaded() + " " + tinker.isEnabledForNativeLib());

        if (tinker.isEnabledForNativeLib() && tinker.isTinkerLoaded()) {
            TinkerLoadResult loadResult = tinker.getTinkerLoadResultIfPresent();
            if (loadResult.libs == null) {
                return libName;
            }
            for (String name : loadResult.libs.keySet()) {
                if (!name.equals(relativeLibPath)) {
                    continue;
                }
                String patchLibraryPath = loadResult.libraryDirectory + "/" + name;
                File library = new File(patchLibraryPath);
                if (!library.exists()) {
                    continue;
                }

                boolean verifyMd5 = tinker.isTinkerLoadVerify();
                if (verifyMd5 && !SharePatchFileUtil.verifyFileMd5(library, loadResult.libs.get(name))) {
                    tinker.getLoadReporter().onLoadFileMd5Mismatch(library, ShareConstants.TYPE_LIBRARY);
                } else {
                    TinkerLog.i(TAG, "findLibraryFromTinker success:" + patchLibraryPath);
                    return patchLibraryPath;
                }
            }
        }

        return libName;
    }

}