package com.sk.hotfixflutter;

/**
 * |                   quu..__
 * |                    $$  `---.__
 * |                     "$        `--.                          ___.---uuudP
 * |                      `$           `.__.------.__     __.---'      $$$$"              .
 * |                        "          -'            `-.-'            $$$"              .'|
 * |                          ".                                       d$"             _.'  |
 * |                            `.   /                              ..."             .'     |
 * |                              `./                           ..::-'            _.'       |
 * |                               /                         .:::-'            .-'         .'
 * |                              :                          ::''\          _.'            |
 * |                             .' .-.             .-.           `.      .'               |
 * |                             : /'$$|           .@"$\           `.   .'              _.-'
 * |                            .'|$$|          |$$,$$|           |  <            _.-'
 * |                            | `:$$:'          :$$$$$:           `.  `.       .-'
 * |                            :                  `"--'             |    `-.     \
 * |                           :$$.       ==             .$$$.       `.      `.    `\
 * |                           |$$:                      :$$$:        |        >     >
 * |                           |$'     `..'`..'          `$$$'        x:      /     /
 * |                            \                                   xXX|     /    ./
 * |                             \                                xXXX'|    /   ./
 * |                             /`-.                                  `.  /   /
 * |                            :    `-  ...........,                   | /  .'
 * |                            |         ``:::::::'       .            |<    `.
 * |                            |             ```          |           x| \ `.:``.
 * |                            |                         .'    /'   xXX|  `:`M`M':.
 * |                            |    |                    ;    /:' xXXX'|  -'MMMMM:'
 * |                            `.  .'                   :    /:'       |-'MMMM.-'
 * |                             |  |                   .'   /'        .'MMM.-'
 * |                             `'`'                   :  ,'          |MMM<
 * |                               |                     `'            |tbap\
 * |                                \                                  :MM.-'
 * |                                 \                 |              .''
 * |                                  \.               `.            /
 * |                                   /     .:::::::.. :           /
 * |                                  |     .:::::::::::`.         /
 * |                                  |   .:::------------\       /
 * |                                 /   .''               >::'  /
 * |                                 `',:                 :    .'
 * |
 * |                                                      `:.:'
 * |
 * |
 * |
 *
 * @author SK on 2020/8/7
 */


import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.multidex.MultiDex;

import com.taobao.sophix.PatchStatus;
import com.taobao.sophix.SophixApplication;
import com.taobao.sophix.SophixEntry;
import com.taobao.sophix.SophixManager;
import com.taobao.sophix.core.dex.SophixNative;
import com.taobao.sophix.listener.PatchLoadStatusListener;
import com.tencent.bugly.beta.Beta;

/**
 * Sophix入口类，专门用于初始化Sophix，不应包含任何业务逻辑。
 * 此类必须继承自SophixApplication，onCreate方法不需要实现。
 * 此类不应与项目中的其他类有任何互相调用的逻辑，必须完全做到隔离。
 * AndroidManifest中设置application为此类，而SophixEntry中设为原先Application类。
 * 注意原先Application里不需要再重复初始化Sophix，并且需要避免混淆原先Application类。
 * 如有其它自定义改造，请咨询官方后妥善处理。
 */
public class SophixStubApplication extends SophixApplication {
    private final String TAG = "SophixStubApplication";
    // 此处SophixEntry应指定真正的Application，并且保证RealApplicationStub类名不被混淆。
    @Keep
    @SophixEntry(AppApplication.class)
    static class RealApplicationStub {}
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
//         如果需要使用MultiDex，需要在此处调用。
         MultiDex.install(this);

        initSophix();
    }
    private void initSophix() {
        String appVersion = "0.0.0";
        try {
            appVersion = this.getPackageManager()
                    .getPackageInfo(this.getPackageName(), 0)
                    .versionName;
        } catch (Exception e) {
        }
        final SophixManager instance = SophixManager.getInstance();
        instance.setContext(this)
                .setAppVersion(appVersion)
                .setEnableDebug(true)
                .setEnableFullLog()
                .setSecretMetaData("你的 id","你的 key","RSA密钥")
                .setPatchLoadStatusStub(new PatchLoadStatusListener() {
                    @Override
                    public void onLoad(final int mode, final int code, final String info, final int handlePatchVersion) {
                        Log.e(TAG, "sophix load patch! mode:" + mode + " code:" + code + " info:" + info + " handlePatchVersion:" + handlePatchVersion);
                        if (code == PatchStatus.CODE_LOAD_SUCCESS) {
                            Log.i(TAG, "sophix load patch success!");
                        } else if (code == PatchStatus.CODE_LOAD_RELAUNCH) {
                            // 如果需要在后台重启，建议此处用SharePreference保存状态。
                            Log.i(TAG, "sophix preload patch success. restart app to make effect.");
                        }
                    }
                }).initialize();
    }



}
