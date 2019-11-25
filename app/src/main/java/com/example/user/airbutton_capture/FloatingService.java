package com.example.user.airbutton_capture;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.example.user.airbutton_capture.CaptureService.squareFrame;

public class FloatingService extends Service implements View.OnTouchListener{
    private String TAG = "FloatingService";
    static final String EXTRA_RESULT_CODE="resultCode";
    static final String EXTRA_RESULT_INTENT="resultIntent";
    private int resultCode;
    private Intent resultData;

    private WindowManager windowManager;
    private View floatingView;
    public int screenDensity;
    public int displayWidth, displayHeight;
    public ImageReader imageReader;
    public VirtualDisplay virtualDisplay;
    private MediaProjectionManager mpManager;
    private MediaProjection mProjection;

    private LinearLayout first_overlay;
    private LinearLayout second_overlay;
    private LinearLayout third_overlay;
    private ImageButton airButton;
    private ImageButton captureButton;
    private ImageButton appButton;
    private ImageButton backButton;
    private ImageButton captureOrBackButton;

    float xpos = 0;
    float ypos = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.activity_floating, null);
        floatingView.setOnTouchListener(this);

        //Displaymetrics로 화면의 가로 세로 크기와 dp를 얻을
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);  //getWindowManager는 액티비티에서만 되고 서비스에서는 안됨
        screenDensity = displayMetrics.densityDpi;  //이것도.. 서비스에서 따로 하면 안되는듯
        displayWidth = displayMetrics.widthPixels;
        displayHeight = displayMetrics.heightPixels;
        mpManager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE); //미디어프로젝션 시작?
        setUpMediaProjection(resultCode,resultData);

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
                Toast.makeText(getApplicationContext(),"capture!",Toast.LENGTH_SHORT).show();

                getScreenshot();
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
    public void onDestroy() {
        if (virtualDisplay != null) {
            Log.d("debug","release VirtualDisplay");
            virtualDisplay.release();
        }
        if (floatingView != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }

        super.onDestroy();
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


    @Override
    public int onStartCommand(Intent i, int flags, int startId){
        if (i.getAction()==null) {
            resultCode = i.getIntExtra(EXTRA_RESULT_CODE, 1337);
            resultData = i.getParcelableExtra(EXTRA_RESULT_INTENT);
        }
        return(START_NOT_STICKY);
    }

    private void setUpMediaProjection(int code, Intent intent) {
        mProjection = mpManager.getMediaProjection(code, intent);
        setUpVirtualDisplay();
    }

    private void setUpVirtualDisplay() {
        imageReader = ImageReader.newInstance(
                displayWidth, displayHeight, PixelFormat.RGBA_8888, 2);

        virtualDisplay = mProjection.createVirtualDisplay("ScreenCapture",
                displayWidth, displayHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);
    }


    public void getScreenshot() {
        // ImageReader에서 화면을 검색
        Log.d("debug", "getScreenshot");

        Image image = imageReader.acquireLatestImage();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();

        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * displayWidth;

        // 버퍼에서 Bitmap을 생성
        Bitmap bitmap = Bitmap.createBitmap(
                displayWidth + rowPadding / pixelStride, displayHeight,
                Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        image.close();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "Screenshot" + timeStamp;

        saveBitmaptoJPEG(bitmap, fileName);

    }

    public static void saveBitmaptoJPEG(Bitmap bitmap, String name){
        String ex_storage = Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/Camera/";
        //String folder_name = "/"+"ScreenShot"+"/";
        String file_name = name+".jpg";
        String string_path = ex_storage;

        File file_path;
        try{
            Log.d("debug", "트라이는 들어왔따");
            file_path = new File(string_path);
            if(!file_path.isDirectory()){
                Log.d("debug", "디렉토리 없어서 만들려고 한다");
                file_path.mkdirs();
            }
            FileOutputStream out = new FileOutputStream(string_path+file_name);

            bitmap = cropBitmap(bitmap);
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,out);
            Log.d("debug", "@@@@@@@@2bitmap to jpeg");
            out.close();
        }catch (FileNotFoundException e){
            Log.e("FileNotFoundException",e.getMessage());
        }catch (IOException e){
            Log.e("IOException",e.getMessage());
        }
    }

    public static Bitmap cropBitmap(Bitmap original){
        Bitmap result = Bitmap.createBitmap(original
                , 0, 0
                ,original.getWidth(), original.getHeight()/4); //시작x,시작y,넓이,높이
        return result;

    }


}
