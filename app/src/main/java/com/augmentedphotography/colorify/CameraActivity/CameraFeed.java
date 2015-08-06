package com.augmentedphotography.colorify.CameraActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    private static final String TAG = "CameraFeed";
    private SurfaceTexture surface;

    CameraFeed(Context context) {
        this.context = context;
    }

    void start(SurfaceTexture surface) {
        this.surface = surface;
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

    void resume() {
        this.start(surface);
    }

    void capture() {
        byte[] buffer = new byte[10000000];
        mainCamera.addCallbackBuffer(buffer);
        mainCamera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
            }
        },
           rawCallback,jpegCallback
           /*new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                int w = camera.getParameters().getPictureSize().width;
                int h = camera.getParameters().getPictureSize().height;

                Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bm.copyPixelsFromBuffer(ByteBuffer.wrap(data));
                MediaStore.Images.Media.insertImage(context.getContentResolver(), bm, "myPicture", "none");

                //preview stops after taking a picture
                mainCamera.startPreview();
            }
        }*/
        );
    }
    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            //			 Log.d(TAG, "onPictureTaken - raw");
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask().execute(data);
            mainCamera.startPreview();
            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };
    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        context.sendBroadcast(mediaScanIntent);
    }


    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream = null;

            // Write to SD Card
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (Environment.getExternalStorageDirectory() + "/" + android.os.Environment.DIRECTORY_DCIM + "/camtest");
                dir.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);

                outStream = new FileOutputStream(outFile);
                outStream.write(data[0]);
                outStream.flush();
                outStream.close();
                refreshGallery(outFile);
                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }

    }
    public Camera getCamera() {
        return mainCamera;
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
