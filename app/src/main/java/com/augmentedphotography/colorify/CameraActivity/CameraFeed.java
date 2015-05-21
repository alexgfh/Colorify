package com.augmentedphotography.colorify.CameraActivity;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import java.io.IOException;

/**
 * Created by Alex on 5/21/2015.
 */

@SuppressWarnings("deprecation")
public class CameraFeed
{
    private static String LOG_TAG = "CameraManager";
    private boolean running = false;
    private Camera mainCamera;

    void start(SurfaceTexture surface)
    {
        Log.v(LOG_TAG, "Starting Camera");

        try {
            mainCamera = Camera.open();
        }
        catch(Exception e) {
            Log.e(LOG_TAG, "no camera");
        }
        Camera.Parameters cameraParameters = mainCamera.getParameters();
        Log.v(LOG_TAG,cameraParameters.getPreviewSize().width + " x " + cameraParameters.getPreviewSize().height);

        try {
            mainCamera.setPreviewTexture(surface);
            mainCamera.startPreview();
            running = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void stop()
    {
        if (running) {
            Log.v(LOG_TAG, "Stopping Camera");
            mainCamera.stopPreview();
            mainCamera.release();
            running = false;
        }
    }
}
