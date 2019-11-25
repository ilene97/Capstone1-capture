package com.example.user.airbutton_capture;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static final int REQ_CODE_OVERLAY_PERMISSION = 1;

    private MediaProjectionManager mpManager;
    public static final int REQUEST_MEDIA_PROJECTION = 1001;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mpManager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE); //미디어프로젝션 시작?

        // permission을 확인하는 intent를 던져 사용자의 허가 · 불허가를받을
        if(mpManager != null){
            startActivityForResult(mpManager.createScreenCaptureIntent(),
                    REQUEST_MEDIA_PROJECTION);
        }
        //openView();
        //finish();
    }

    public void openView() {
        if(Settings.canDrawOverlays(this))
            startService(new Intent(this, FloatingService.class));
        else
            onObtainingPermissionOverlayWindow();
    }


    public void onObtainingPermissionOverlayWindow() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQ_CODE_OVERLAY_PERMISSION);
    }

    // 사용자의 허가를받을
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_MEDIA_PROJECTION == requestCode) {
            if (resultCode != RESULT_OK) {
                // 거부 된
                Toast.makeText(this,
                        "User cancelled", Toast.LENGTH_LONG).show();
                return;
            }
            Intent i = new Intent(this, FloatingService.class)
                    .putExtra(FloatingService.EXTRA_RESULT_CODE, resultCode)
                    .putExtra(FloatingService.EXTRA_RESULT_INTENT,data);
            if(Settings.canDrawOverlays(this))
                startService(i);
            else onObtainingPermissionOverlayWindow();
            // 허가 된 결과를받을
            //setUpMediaProjection(resultCode, data);
        }
        finish();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
