package com.example.user.airbutton_capture;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CaptureService extends Service {
    private WindowManager windowManager2;
    public static View squareFrame;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Toast.makeText(getApplicationContext(),"캡쳐 onCreate",Toast.LENGTH_LONG).show();
        // 서비스에서 가장 먼저 호출됨(최초에 한번만)
        Log.d("test", "capture service onCreate");
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        squareFrame = inflater.inflate(R.layout.capture_layout, null);
        Log.d("test", "set inflater");
        //squareFrame.setOnTouchListener(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.RIGHT;
        windowManager2 = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager2.addView(squareFrame, params);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (squareFrame != null) {
            windowManager2.removeView(squareFrame);
            squareFrame = null;
        }
    }

}
