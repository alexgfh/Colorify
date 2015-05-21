package com.augmentedphotography.colorify.CameraActivity;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Alex on 5/21/2015.
 */
public class DirectVideo {
    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 2;
    static float squareCoords[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f,
    };
    static float textureVertices[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
    };
    private static String LOG_TAG = "DirectVideo";
    private final String vertexShaderCode =
            "attribute vec4 vPosition;\n" +
                    "attribute vec2 inputTextureCoordinate;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "void main() {\n" +
                    "  gl_Position = vPosition;\n" +
                    "  textureCoordinate = inputTextureCoordinate;\n" +
                    "}\n";
    private final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "uniform float threshold;\n" +
                    "uniform float reference_hue;\n" +
                    "uniform samplerExternalOES s_texture;\n" +
                    "vec3 rgb2hsv(vec3 c)\n" +
                    "{\n" +
                    "  vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n" +
                    "  vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n" +
                    "  vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n" +
                    "\n" +
                    "    float d = q.x - min(q.w, q.y);\n" +
                    "    float e = 1.0e-10;\n" +
                    "  return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n" +
                    "}\n" +
                    "\n" +
                    "vec3 hsv2rgb(vec3 c)\n" +
                    "{\n" +
                    "  vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n" +
                    "  vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n" +
                    "  return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n" +
                    "}\n" +
                    "void main() {\n" +
                    "  vec3 frag = texture2D(s_texture, textureCoordinate).xyz;\n" +
                    "  vec3 hsv = rgb2hsv(frag);\n" +
                    "  float difference = abs(hsv.x - reference_hue);\n" +
                    "  difference = min(difference, -difference+1.0);\n" +
                    "  if (difference > threshold) {\n" +
                    "    gl_FragColor = vec4(vec3(hsv.z), 1.0);\n" +
                    "  }\n" +
                    "  else {\n" +
                    "    gl_FragColor = vec4(frag, 1.0);\n" +
                    "  }\n" +
                    "}\n";
    private final int mProgram;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    private FloatBuffer vertexBuffer, textureVerticesBuffer;
    private ShortBuffer drawListBuffer;
    private short drawOrder[] = {0, 1, 2, 0, 2, 3}; // order to draw vertices
    private int texture;

    public DirectVideo(int texture) {
        this.texture = texture;

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(textureVertices);
        textureVerticesBuffer.position(0);

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
    }

    private static int loadShader(int type, String shaderCode) {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        //String msg = GLES20.glGetShaderInfoLog(shader);
        //Log.v(LOG_TAG, msg);
        return shader;
    }

    public void draw(float referenceHue, float threshold, float[] transform) {
        GLES20.glUseProgram(mProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);

        // get handle to vertex shader's vPosition member
        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Prepare the <insert shape here> coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

        int mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textureVerticesBuffer);

        int mReferenceHueHandle = GLES20.glGetUniformLocation(mProgram, "reference_hue");
        GLES20.glUniform1f(mReferenceHueHandle, referenceHue);

        int mThresholdHandle = GLES20.glGetUniformLocation(mProgram, "threshold");
        GLES20.glUniform1f(mThresholdHandle, threshold);


        //TODO: use transform matrix (pass to shader)
        /*int mTransformHandle = GLES20.glGetUniformLocation(mProgram, "transform");
        ByteBuffer bb = ByteBuffer.allocateDirect(64); //16*4
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer transformBuffer = bb.asFloatBuffer();
        transformBuffer.put(transform);
        transformBuffer.position(0);
        GLES20.glUniformMatrix4fv(mThresholdHandle, 1, false, transformBuffer);*/

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
    }
}
