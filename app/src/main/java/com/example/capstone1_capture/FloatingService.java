package com.example.capstone1_capture;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.ImageReader;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;


public class FloatingService extends Service implements View.OnTouchListener{
    private String TAG = "FloatingService";

    private WindowManager windowManager;
    private View floatingView;


    private LinearLayout first_overlay;
    private LinearLayout second_overlay;
    private LinearLayout third_overlay;
    private ImageButton airButton;
    private ImageButton captureButton;
    private ImageButton appButton;
    private ImageButton backButton;
    private ImageButton captureOrBackButton;

    public int screenDensity;
    public int displayWidth, displayHeight;

    float xpos = 0;
    float ypos = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.activity_floating, null);
        floatingView.setOnTouchListener(this);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.RIGHT|Gravity.TOP;

        first_overlay = floatingView.findViewById(R.id.first_overlay);
        second_overlay = floatingView.findViewById(R.id.second_overlay);
        third_overlay = floatingView.findViewById(R.id.third_overlay);

        airButton = floatingView.findViewById(R.id.floating_button);
        backButton = floatingView.findViewById(R.id.overlay_back_button);
        captureButton = floatingView.findViewById(R.id.capture_button);
        appButton = floatingView.findViewById(R.id.app_button);
        captureOrBackButton = floatingView.findViewById(R.id.capture_or_back_button);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        screenDensity = displayMetrics.densityDpi;
        displayWidth = displayMetrics.widthPixels;
        displayHeight = displayMetrics.heightPixels;


        airButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("onClick: ", "first overlay");

                first_overlay.setClickable(false);
                first_overlay.setVisibility(View.GONE);
                second_overlay.setClickable(true);
                second_overlay.setVisibility(View.VISIBLE);
            }
        });

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                second_overlay.setClickable(false);
                second_overlay.setVisibility(View.GONE);
                third_overlay.setClickable(true);
                third_overlay.setVisibility(View.VISIBLE);
                startService(new Intent(FloatingService.this, CaptureService.class));
                //stopSelf();
            }
        });

        captureOrBackButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(getApplicationContext(), CaptureActivity.class );
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1001, intent, 0);
                try {
                    pendingIntent.send();
                }
                catch(PendingIntent.CanceledException e) {
                    Log.d("test", "-------------------------pending exception ");
                    e.printStackTrace();
                }

            }
        });
        captureOrBackButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(),"Back to second",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), CaptureService.class);
                stopService(intent);

                third_overlay.setClickable(false);
                third_overlay.setVisibility(View.GONE);
                second_overlay.setClickable(true);
                second_overlay.setVisibility(View.VISIBLE);

                return true;
            }
        });

        appButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                windowManager.removeView(floatingView);
                floatingView = null;

                //TAB 액티비티로 이동
//                Intent intent = new Intent(getApplicationContext(), DetailsActivity.class);
//                startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
//                stopSelf();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                first_overlay.setClickable(true);
                first_overlay.setVisibility(View.VISIBLE);
                second_overlay.setClickable(false);
                second_overlay.setVisibility(View.GONE);
            }
        });

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);
    }




    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int action = motionEvent.getAction();
        int pointerCount = motionEvent.getPointerCount();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (pointerCount == 1) {
                    xpos = motionEvent.getRawX();
                    ypos = motionEvent.getRawY();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pointerCount == 1) {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view.getLayoutParams();
                    float dx = xpos - motionEvent.getRawX();
                    float dy = ypos - motionEvent.getRawY();
                    xpos = motionEvent.getRawX();
                    ypos = motionEvent.getRawY();

                    Log.d(TAG, "lp.x : " + lp.x + ", dx : " + dx + "lp.y : " + lp.y + ", dy : " + dy);

                    lp.x = (int) (lp.x - dx);
                    lp.y = (int) (lp.y - dy);

                    windowManager.updateViewLayout(view,lp);
                    return true;
                }
                break;
        }
        return false;
    }

/*
    @Override
    public int onStartCommand(Intent i, int flags, int startId){
        if (i.getAction()==null) {
            resultCode = i.getIntExtra(EXTRA_RESULT_CODE, 1337);
            resultData = i.getParcelableExtra(EXTRA_RESULT_INTENT);
        }
        return(START_NOT_STICKY);
    }
*/

    @Override
    public void onDestroy() {

        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }

        super.onDestroy();
    }


}
