package com.augmentedphotography.colorify.CameraActivity;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;

/**
 * Created by Alex on 5/21/2015.
 */

@SuppressWarnings("deprecation")
public class CameraFeed {
    private static String LOG_TAG = "CameraManager";
    private boolean running = false;
    private Camera mainCamera;
    private Context context;

    CameraFeed(Context context) {
        this.context = context;
    }

    void start(SurfaceTexture surface) {
        Log.v(LOG_TAG, "Starting Camera");

        try {
            mainCamera = Camera.open();
        } catch (Exception e) {
            Log.e(LOG_TAG, "no camera");
        }
        Camera.Parameters cameraParameters = mainCamera.getParameters();
        Log.v(LOG_TAG, cameraParameters.getPreviewSize().width + " x " + cameraParameters.getPreviewSize().height);

        try {
            mainCamera.setPreviewTexture(surface);
            mainCamera.startPreview();
            running = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void capture() {
        mainCamera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                /*int w = camera.getParameters().getPictureSize().width;
                int h = camera.getParameters().getPictureSize().height;
                Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bm.copyPixelsFromBuffer(ByteBuffer.wrap(data));
                MediaStore.Images.Media.insertImage(context.getContentResolver(), bm, "myPicture", "none");
                */
            }
        }, null
        );
    }

    void stop() {
        if (running) {
            Log.v(LOG_TAG, "Stopping Camera");
            mainCamera.stopPreview();
            mainCamera.release();
            running = false;
        }
    }
}
