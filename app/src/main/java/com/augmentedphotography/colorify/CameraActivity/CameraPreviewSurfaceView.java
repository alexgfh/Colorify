package com.augmentedphotography.colorify.CameraActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Alex on 5/21/2015.
 */
public class CameraPreviewSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private final static String LOG_TAG = "CameraPreviewSurfacView";
    private CameraFeed cameraFeed;
    private SurfaceTexture mSurface;
    private DirectVideo directVideo;
    private int selectedColor;
    private boolean reselectColor = false;
    private float selectedHue = 240.0f; //blue: uncommon color
    private float threshold = 360.0f; //unreachable threshold
    private Point selectedPosition = new Point();
    private boolean capture = false;
    private ByteBuffer frame;
    private Bitmap picture;
    private int width, height;
    private Context context;

    public CameraPreviewSurfaceView(Context context) {
        this(context, null);

    }

    public CameraPreviewSurfaceView(Context context, CameraFeed camera) {
        super(context);

        this.context = context;
        this.cameraFeed = camera;
        setEGLContextClientVersion(2);

        setRenderer(this);
    }


    @Override
    public void onDrawFrame(GL10 gl) {
        float[] mtx = new float[16];
        mSurface.updateTexImage();
        mSurface.getTransformMatrix(mtx);

        directVideo.draw(selectedHue / 360.0f, threshold / 360.0f, mtx);
        if (reselectColor) {
            selectedColor = getGLPixel(selectedPosition.x, selectedPosition.y);
            float[] hsv = new float[3];
            Color.RGBToHSV((selectedColor & 0xFF), ((selectedColor>>8) & 0xFF), ((selectedColor>>16) & 0xFF), hsv);
            selectedHue = hsv[0];
            reselectColor = false;
            String msg = "(" + (selectedColor & 0xFF) + ',' + ((selectedColor>>8) & 0xFF) + ',' + ((selectedColor>>16) & 0xFF) + ')';
            Log.d(LOG_TAG,msg);
        }
        if (capture) {
            frame = ByteBuffer.allocateDirect(width*height*4);
            frame.order(ByteOrder.nativeOrder());
            picture = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            GLES20.glReadPixels(0, 0, getWidth(), getHeight(), GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, frame);
            picture.copyPixelsFromBuffer(frame);
            String savedImageUrl = MediaStore.Images.Media.insertImage(getContext().getContentResolver(), picture, "myPicture", "none");
            Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.parse(savedImageUrl));
            context.sendBroadcast(mediaScanIntent);
            Log.e(LOG_TAG, "File path:" + savedImageUrl);
            capture = false;
        }
    }

    private int getGLPixel(int x, int y) {
        ByteBuffer output = ByteBuffer.allocateDirect(4);
        output.order(ByteOrder.nativeOrder());

        int error = GLES20.glGetError();
        GLES20.glReadPixels(x, y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, output);
        int error2 = GLES20.glGetError();
        return output.asIntBuffer().get(0);
    }

    public void scheduleGetPixels() {
        capture = true;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.v(LOG_TAG, "Surface Changed");
        GLES20.glViewport(0, 0, width, height);
        this.width = width;
        this.height = height;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.v(LOG_TAG, "Surface Created");
        int texture = createTexture();
        directVideo = new DirectVideo(texture);
        //TODO: try single-buffer mode
        mSurface = new SurfaceTexture(texture /*,true*/);
        //TODO: create MediaRecorder here

        cameraFeed.start(mSurface);
    }

    private int createTexture() {
        int[] textures = new int[1];

        // generate one texture pointer and bind it as an external texture.
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        // No mip-mapping with camera source.
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // Clamp to edge is only option.
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        return textures[0];
    }

    void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            return true;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        selectedPosition.x = x;
        selectedPosition.y = getHeight() - y;
        reselectColor = true;
        return true;
    }

}
