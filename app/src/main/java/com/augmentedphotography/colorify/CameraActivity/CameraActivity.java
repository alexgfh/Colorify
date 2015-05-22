package com.augmentedphotography.colorify.CameraActivity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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


public class CameraActivity extends Activity {
    private static final String LOG_TAG = "CameraActivity";
    private CameraFeed cameraFeed;
    private CameraPreviewSurfaceView renderedView;
    private Button resetButton;
    private SeekBar thresholdBar;
    private RelativeLayout layout;

    //TODO: Create hue selector (get color from hue bitmap itself?)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    public void captureFrame(View view) {
        cameraFeed.capture();
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
    protected void onPause() {
        super.onPause();
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
        //TODO: draw view on bitmap to get one pixel
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
