package com.augmentedphotography.colorify.CameraActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.augmentedphotography.colorify.R;

import java.io.File;


public class CameraActivity extends Activity  implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "CameraActivity";
    private CameraFeed cameraFeed;
    private CameraPreviewSurfaceView renderedView;
    private Button resetButton;
    private SeekBar thresholdBar;
    private RelativeLayout layout;

    private final static int MY_PERMISSIONS_REQUEST_CAMERA = 1;
    private final static int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 2;


    //TODO: Create hue selector (get color from hue bitmap itself?)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int permissionCameraCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);

        int permissionWriteCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCameraCheck != PackageManager.PERMISSION_GRANTED && permissionWriteCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
        else if (permissionCameraCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }
        else if (permissionWriteCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        }
        if (permissionCameraCheck == PackageManager.PERMISSION_GRANTED) {
            permittedCamera = true;
            setupView();
        }
        if (permissionWriteCheck == PackageManager.PERMISSION_GRANTED) {
            permittedWrite = true;
        }


    }

    private void setupView() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        cameraFeed = new CameraFeed(this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        layout = (RelativeLayout) inflater.inflate(R.layout.activity_camera, null);
        renderedView = new CameraPreviewSurfaceView(this, cameraFeed);
        layout.addView(renderedView, 0);
        setContentView(layout);

        /* TODO: Add ads
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/

        resetButton = (Button) findViewById(R.id.reset_button);
        thresholdBar = (SeekBar) findViewById(R.id.threshold_bar);
        thresholdBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                renderedView.setThreshold((float) progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        thresholdBar.setMax(100);
        thresholdBar.setProgress(100);
        if (resetButton == null) {
            throw new RuntimeException("resetButton is null");
        }
        if (thresholdBar == null) {
            throw new RuntimeException("thresholdBar is null");
        }
    }

    private Bitmap loadBitmapFromView(View v) {
        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);

        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            Log.e(TAG, "failed getViewBitmap(" + v + ")", new RuntimeException());
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(cacheBitmap);

        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        return bitmap;
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    public void captureFrame(View view) {
        //Bitmap bm = loadBitmapFromView(renderedView);
        renderedView.scheduleGetPixels();

        //cameraFeed.capture();
    }

    public void reset(View view) {
        thresholdBar.setProgress(100);
        renderedView.setThreshold(360.0f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(permittedCamera && !cameraFeed.running)
            cameraFeed.resume();
    }
    private boolean permittedCamera = false;
    private boolean permittedWrite = false;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            permittedCamera = true;
            setupView();
            if(!cameraFeed.running)
                cameraFeed.resume();
        }
        else if (requestCode == MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
            permittedWrite = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.e("CameraFeed", "resuming camera");
        if(cameraFeed!=null && permittedCamera && !cameraFeed.running)
            cameraFeed.resume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(cameraFeed!=null && cameraFeed.running)
            cameraFeed.stop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraFeed!=null && cameraFeed.running)
            cameraFeed.stop();
    }

    /*@Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            Log.d(LOG_TAG, "Down");
        }
        else  if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            Log.d(LOG_TAG,"Up");
        }
        //npTODO: draw view on bitmap to get one pixel
        int x = (int)event.getX();
        int y = (int)event.getY();
        //layout.layout(0,0, 1080, 1920);
        if(layout.willNotCacheDrawing()) {
            throw new RuntimeException("not caching");
        }
        View screen = findViewById(android.R.id.content).getRootView();
        screen.setDrawingCacheEnabled(true);
        Bitmap bmScreen = screen.getDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(layout.getWidth(), layout.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        layout.draw(canvas);
        int colorSelected = bmScreen.getPixel(x, y);
        String msg = "(" + (colorSelected>>24&0xff) + ',' + (colorSelected>>16&0xff) + ',' + (colorSelected>>8&0xff) + ',' + (colorSelected&0xff) +  ')';
        Log.d(LOG_TAG,msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        return true;
    }*/
}
