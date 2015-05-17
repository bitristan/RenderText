package xyz.sunting.rendertext;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Iterator;
import java.util.Vector;

public class TextManager {

    private static final float RI_TEXT_UV_BOX_WIDTH = 0.125f;
    private static final float RI_TEXT_WIDTH = 32.0f;
    private static final float RI_TEXT_SPACESIZE = 20f;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTexCoordBuffer;
    private FloatBuffer mColorBuffer;
    private ShortBuffer mDrawListBuffer;

    private float[] mVertices;
    private float[] mTexCoords;
    private short[] mIndices;
    private float[] mColors;

    private int mVertexIndex;
    private int mIndiceIndex;
    private int mTexCoordIndex;
    private int mColorIndex;

    private int mTextureId;
    private int mProgram;

    private float mScale;

    public static int[] ALPHABET = {36, 29, 30, 34, 25, 25, 34, 33,
            11, 20, 31, 24, 48, 35, 39, 29,
            42, 31, 27, 31, 34, 35, 46, 35,
            31, 27, 30, 26, 28, 26, 31, 28,
            28, 28, 29, 29, 14, 24, 30, 18,
            26, 14, 14, 14, 25, 28, 31, 0,
            0, 38, 39, 12, 36, 34, 0, 0,
            0, 38, 0, 0, 0, 0, 0, 0};

    public Vector<TextObject> mTextObjects;

    public TextManager(int textureId) {
        mTextObjects = new Vector<TextObject>();

        mVertices = new float[3 * 10];
        mColors = new float[4 * 10];
        mTexCoords = new float[2 * 10];
        mIndices = new short[10];

        mTextureId = textureId;
        mProgram = ShaderUtil.createProgramFromName("text.vshader", "text.fshader");
    }

    public void addText(TextObject obj) {
        mTextObjects.add(obj);
    }

    public void addCharRenderInfo(float[] vec, float[] cs, float[] uv, short[] indi) {
        // We need a base value because the object has mIndices related to
        // that object and not to this collection so basicly we need to
        // translate the mIndices to align with the vertexlocation in ou
        // mVertices array of vectors.
        short base = (short) (mVertexIndex / 3);

        // We should add the vec, translating the mIndices to our saved vector
        for (int i = 0; i < vec.length; i++) {
            mVertices[mVertexIndex] = vec[i];
            mVertexIndex++;
        }

        // We should add the mColors, so we can use the same texture for multiple effects.
        for (int i = 0; i < cs.length; i++) {
            mColors[mColorIndex] = cs[i];
            mColorIndex++;
        }

        // We should add the mTexCoords
        for (int i = 0; i < uv.length; i++) {
            mTexCoords[mTexCoordIndex] = uv[i];
            mTexCoordIndex++;
        }

        // We handle the mIndices
        for (int j = 0; j < indi.length; j++) {
            mIndices[mIndiceIndex] = (short) (base + indi[j]);
            mIndiceIndex++;
        }
    }

    public void prepareDrawInfo() {
        mVertexIndex = 0;
        mIndiceIndex = 0;
        mTexCoordIndex = 0;
        mColorIndex = 0;

        // Get the total amount of characters
        int charcount = 0;
        for (TextObject txt : mTextObjects) {
            if (txt != null) {
                if (!(txt.text == null)) {
                    charcount += txt.text.length();
                }
            }
        }

        // Create the arrays we need with the correct size.
        mVertices = null;
        mColors = null;
        mTexCoords = null;
        mIndices = null;

        mVertices = new float[charcount * 12];
        mColors = new float[charcount * 16];
        mTexCoords = new float[charcount * 8];
        mIndices = new short[charcount * 6];

    }

    public void prepareDraw() {
        // Setup all the arrays
        prepareDrawInfo();

        // Using the iterator protects for problems with concurrency
        for (Iterator<TextObject> it = mTextObjects.iterator(); it.hasNext(); ) {
            TextObject txt = it.next();
            if (txt != null && txt.text != null) {
                convertTextToTriangleInfo(txt);
            }
        }
    }

    public void drawSelf(float[] m) {
        // Set the correct shader for our grid object.
        GLES20.glUseProgram(mProgram);

        // The vertex buffer.
        ByteBuffer bb = ByteBuffer.allocateDirect(mVertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(mVertices);
        mVertexBuffer.position(0);

        // The vertex buffer.
        ByteBuffer bb3 = ByteBuffer.allocateDirect(mColors.length * 4);
        bb3.order(ByteOrder.nativeOrder());
        mColorBuffer = bb3.asFloatBuffer();
        mColorBuffer.put(mColors);
        mColorBuffer.position(0);

        // The texture buffer
        ByteBuffer bb2 = ByteBuffer.allocateDirect(mTexCoords.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        mTexCoordBuffer = bb2.asFloatBuffer();
        mTexCoordBuffer.put(mTexCoords);
        mTexCoordBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(mIndices.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        mDrawListBuffer = dlb.asShortBuffer();
        mDrawListBuffer.put(mIndices);
        mDrawListBuffer.position(0);

        // get handle to vertex shader's vPosition member
        int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the background coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, 3,
                GLES20.GL_FLOAT, false,
                0, mVertexBuffer);

        int mTexCoordLoc = GLES20.glGetAttribLocation(mProgram, "a_texCoord");

        // Prepare the texturecoordinates
        GLES20.glVertexAttribPointer(mTexCoordLoc, 2, GLES20.GL_FLOAT,
                false,
                0, mTexCoordBuffer);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mTexCoordLoc);

        int mColorHandle = GLES20.glGetAttribLocation(mProgram, "a_Color");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mColorHandle);

        // Prepare the background coordinate data
        GLES20.glVertexAttribPointer(mColorHandle, 4,
                GLES20.GL_FLOAT, false,
                0, mColorBuffer);

        // get handle to shape's transformation matrix
        int mtrxhandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        GLES20.glUniformMatrix4fv(mtrxhandle, 1, false, m, 0);

        int textureHandle = GLES20.glGetUniformLocation(mProgram, "s_texture");
        GLES20.glUniform1i(textureHandle, mTextureId);

        // Draw the triangle
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndices.length, GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordLoc);
        GLES20.glDisableVertexAttribArray(mColorHandle);

    }

    private int convertCharToIndex(int val) {
        int indx = -1;

        if (val > 64 && val < 91) { // A-Z
            indx = val - 65;
        } else if (val > 96 && val < 123) { // a-z
            indx = val - 97;
        } else if (val > 47 && val < 58) { // 0-9
            indx = val - 48 + 26;
        } else if (val == 43) { // +
            indx = 38;
        } else if (val == 45) { // -
            indx = 39;
        } else if (val == 33) { // !
            indx = 36;
        } else if (val == 63) { // ?
            indx = 37;
        } else if (val == 61) { // =
            indx = 40;
        } else if (val == 58) { // :
            indx = 41;
        } else if (val == 46) { // .
            indx = 42;
        } else if (val == 44) { // ,
            indx = 43;
        } else if (val == 42) { // *
            indx = 44;
        } else if (val == 36) { // $
            indx = 45;
        }

        return indx;
    }

    private void convertTextToTriangleInfo(TextObject val) {
        float x = val.x;
        float y = val.y;
        String text = val.text;

        for (int j = 0; j < text.length(); j++) {
            int index = convertCharToIndex((int) text.charAt(j));

            if (index == -1) {
                x += (RI_TEXT_SPACESIZE * mScale);
                continue;
            }

            int row = index / 8;
            int col = index % 8;

            float v = row * RI_TEXT_UV_BOX_WIDTH;
            float v2 = v + RI_TEXT_UV_BOX_WIDTH;
            float u = col * RI_TEXT_UV_BOX_WIDTH;
            float u2 = u + RI_TEXT_UV_BOX_WIDTH;

            // Creating the triangle information
            float[] vec = new float[12];
            float[] uv = new float[8];

            vec[0] = x;
            vec[1] = y + (RI_TEXT_WIDTH * mScale);
            vec[2] = 0.99f;
            vec[3] = x;
            vec[4] = y;
            vec[5] = 0.99f;
            vec[6] = x + (RI_TEXT_WIDTH * mScale);
            vec[7] = y;
            vec[8] = 0.99f;
            vec[9] = x + (RI_TEXT_WIDTH * mScale);
            vec[10] = y + (RI_TEXT_WIDTH * mScale);
            vec[11] = 0.99f;

            float[] colors = new float[] {
                    val.color[0], val.color[1], val.color[2], val.color[3],
                    val.color[0], val.color[1], val.color[2], val.color[3],
                    val.color[0], val.color[1], val.color[2], val.color[3],
                    val.color[0], val.color[1], val.color[2], val.color[3]
            };

            uv[0] = u + 0.001f;
            uv[1] = v + 0.001f;
            uv[2] = u + 0.001f;
            uv[3] = v2 - 0.001f;
            uv[4] = u2 - 0.001f;
            uv[5] = v2 - 0.001f;
            uv[6] = u2 - 0.001f;
            uv[7] = v + 0.001f;

            short[] inds = {0, 1, 2, 0, 2, 3};

            // Add our triangle information to our collection for 1 render call.
            addCharRenderInfo(vec, colors, uv, inds);

            // Calculate the new position
            x += ((ALPHABET[index] / 2) * mScale);
        }
    }

    public float getScale() {
        return mScale;
    }

    public void setScale(float mScale) {
        this.mScale = mScale;
    }
}
