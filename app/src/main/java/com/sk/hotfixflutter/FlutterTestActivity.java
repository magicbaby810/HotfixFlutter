package com.sk.hotfixflutter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import io.flutter.app.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.StandardMessageCodec;



public class FlutterTestActivity extends FlutterActivity {


    private FlutterEngine flutterEngine;
    private BasicMessageChannel basicMessageChannel;
    private Handler handler;

    private static final String NATIVE_ARGS_CHANNEL = "ole.flutter/nativetoflutter";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();
        flutterEngine = new FlutterEngine(this);

        new MethodChannel(getFlutterView(), NATIVE_ARGS_CHANNEL).setMethodCallHandler(new MethodChannel.MethodCallHandler() {
            @Override
            public void onMethodCall(@NonNull MethodCall methodCall, @NonNull MethodChannel.Result result) {

                if (methodCall.method.equals("gotoMainActivity")) {
                    startActivity(new Intent(FlutterTestActivity.this, MainActivity.class));
                } else {
                    result.notImplemented();
                }

            }
        });

        new EventChannel(getFlutterView(), NATIVE_ARGS_CHANNEL).setStreamHandler(new EventChannel.StreamHandler() {

            Timer timer = new Timer();

            @Override
            //注册成功后的回调
            public void onListen(Object o, EventChannel.EventSink eventSink) {
                //定时器自增数值然后传参
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                repeatCount(eventSink);
                            }
                        });


                    }
                }, 0, 1000);

                repeatCount(eventSink);
            }

            @Override
            public void onCancel(Object o) {
                //timer = null;
            }
        });


        basicMessageChannel = new BasicMessageChannel<Object>(flutterEngine.getDartExecutor().getBinaryMessenger(), NATIVE_ARGS_CHANNEL, StandardMessageCodec.INSTANCE);
        basicMessageChannel.setMessageHandler(new BasicMessageChannel.MessageHandler<Object>() {
            @Override
            public void onMessage(Object message, BasicMessageChannel.Reply<Object> reply) {
                Log.d("Android", "Received message = $message");
                reply.reply("Reply from Android");
            }
        });


        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                basicMessageChannel.send("Hello World from Android", new BasicMessageChannel.Reply() {
                    @Override
                    public void reply(Object reply) {
                        Log.d("Android", String.valueOf(reply));
                    }
                });
            }
        }, 500);
    }


    int _nativeCount = 0;
    /**
     * 安卓原生方法传参，每1s就会被执行一次实现自增
     */
    private void repeatCount(EventChannel.EventSink eventSink){
        _nativeCount ++;
    }

    /**
     * 跳转该页面的时候可以传要跳转的页面,参数名固定为route
     */
    private static final String ROUTE_PAGE = "route";

    public static Intent makeIntent(Context context, String routePage) {
        if (TextUtils.isEmpty(routePage)) {
            routePage = "/";
        }
        Intent intent = new Intent(context, FlutterTestActivity.class);
        intent.setAction(Intent.ACTION_RUN);
        intent.putExtra(ROUTE_PAGE, routePage);
        return intent;
    }

}
