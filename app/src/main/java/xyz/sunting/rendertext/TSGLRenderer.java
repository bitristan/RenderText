package xyz.sunting.rendertext;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TSGLRenderer implements Renderer {

    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mMVPMatrix = new float[16];
    public TextManager tm;

    public TSGLRenderer() {
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (tm != null) {
            tm.drawSelf(mMVPMatrix);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        for (int i = 0; i < 16; i++) {
            mProjectionMatrix[i] = 0.0f;
            mViewMatrix[i] = 0.0f;
            mMVPMatrix[i] = 0.0f;
        }
        Matrix.orthoM(mProjectionMatrix, 0, 0f, width, 0.0f, height, 0, 50);
        Matrix.setLookAtM(mViewMatrix, 0, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initFontTexture();
        initFontObject();

        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void initFontObject() {
        tm = new TextManager(0);
        TextObject txt = new TextObject("hello world", 10f, 10f);
        tm.addText(txt);
        tm.addText(new TextObject("OpenGLES", 0f, 100f));
        tm.setScale(2.0f);
        tm.prepareDraw();
    }

    public void initFontTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        int id = R.mipmap.font;
        Bitmap bmp = BitmapFactory.decodeResource(TSApplication.getInstance().getResources(), id);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();
    }

}