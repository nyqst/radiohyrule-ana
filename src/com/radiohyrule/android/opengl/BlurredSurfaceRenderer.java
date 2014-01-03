package com.radiohyrule.android.opengl;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by lysann on 1/3/14.
 */
public class BlurredSurfaceRenderer implements GLSurfaceView.Renderer {
    Bitmap nextBitmap; // must be flipped before usage

    BlurredImage blurredImage;
    float[] mProjectionMatrix = new float[16];
    float[] mViewMatrix = new float[16];
    float[] mMVPMatrix = new float[16];
    float imageAspectRatio = 1.0f;
    int[] viewport;

    public BlurredSurfaceRenderer(Resources resources, int resourceId) {
        setNextBitmap(Util.loadBitmapForTexturing(resources, resourceId, 0, 0, false));
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        updateBitmap();

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        viewport = new int[]{0, 0, width, height};
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        float screenAspectRatio = ((float)width) / ((float)height);
        float left, right, bottom, top;
        if (screenAspectRatio < imageAspectRatio) {
            // aspect-fit image width
            left = -screenAspectRatio/imageAspectRatio; right = screenAspectRatio/imageAspectRatio;
            bottom = -1; top = 1;
        } else {
            // aspect-fit image height
            left = -1; right = 1;
            bottom = -imageAspectRatio/screenAspectRatio; top = imageAspectRatio/screenAspectRatio;
        }
        float zoom = 0.95f;
        Matrix.frustumM(mProjectionMatrix, 0, zoom * left, zoom * right, zoom * bottom, zoom * top, /* near */ 1, /* far */ 5);
        Matrix.setLookAtM(mViewMatrix, 0, /* eye */ 0, 0, 1, /* center */ 0, 0, 0, /* up */ 0, 1, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        updateBitmap();

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        blurredImage.draw(mMVPMatrix, viewport);
    }


    public void setNextBitmap(Bitmap bitmap) {
        this.nextBitmap = bitmap;
    }
    protected void updateBitmap() {
        if (nextBitmap != null) {
            this.setBitmap(Util.flipBitmap(nextBitmap));
            nextBitmap = null;
        }
    }
    protected void setBitmap(Bitmap bitmap) {
        if (blurredImage != null) blurredImage.close();
        blurredImage = new BlurredImage(bitmap, 5, 3);
        imageAspectRatio = ((float)bitmap.getWidth()) / ((float)bitmap.getHeight());
    }


    public class BlurredImage {
        public void close() {
            if (tempFrameBufferObject > 0) {
                GLES20.glDeleteFramebuffers(1, new int[]{tempFrameBufferObject}, 0);
                tempFrameBufferObject = -1;
            }
            if (tempTextureHandle >= 0) {
                GLES20.glDeleteTextures(1, new int[]{tempTextureHandle}, 0);
                tempTextureHandle = -1;
            }
            vertexBuffer = null;
            textureBuffer = null;
            if (mInputTextureDataHandle >= 0) {
                GLES20.glDeleteTextures(1, new int[]{mInputTextureDataHandle}, 0);
                mInputTextureDataHandle = -1;
            }
            if (mProgramHandle >= 0) {
                GLES20.glDeleteProgram(mProgramHandle);
                mProgramHandle = 0;
            }
        }
        protected void finalize() {
            try {
                close();
            } catch (Throwable e) {
                Log.e("", "close in finalize threw exception: " + e.getMessage());
            }
        }

        private final String vertexShaderString =
                "uniform mat4 u_MVPMatrix;\n" +
                        "attribute vec2 a_TexCoordinate;\n" +
                        "attribute vec4 a_Position;\n" +
                        "varying vec2 v_TexCoordinate;\n" +
                        "void main() {\n" +
                        "  gl_Position = u_MVPMatrix * a_Position;\n" +
                        "  v_TexCoordinate = a_TexCoordinate;\n" +
                        "}";
        private int numRuns = 1;

        private int tempFrameBufferObject = -1, tempTextureHandle = -1;

        private FloatBuffer vertexBuffer = null;
        private final float vertexCoords[] = {
                // triangle 1
                -1, -1, 0, // lower left
                1, -1, 0, // lower right
                -1,  1, 0, // upper left
                // triangle 2
                1,  1, 0, // upper right
        };
        private final int POSITION_COORDS_PER_VERTEX = 3;

        private FloatBuffer textureBuffer = null;
        private final float textureCoords[] = {
                // triangle 1
                0, 0, // lower left
                1, 0, // lower right
                0, 1, // upper left
                // triangle 2
                1, 1, // upper right
        };
        private final int TEXTURE_COORDS_PER_VERTEX = 2;
        private int mInputTextureDataHandle = -1;

        private int mProgramHandle = -1;

        private final int sizeofFloat = 4;

        int imageWidth, imageHeight;
        public BlurredImage(Bitmap bitmap, double sigma, int numRuns) {
            imageWidth = bitmap.getWidth(); imageHeight = bitmap.getHeight();

            final int[] intArr = new int[1];
            GLES20.glGenTextures(1, intArr, 0);
            tempTextureHandle = intArr[0];
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tempTextureHandle);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, imageWidth, imageHeight, 0, GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glGenFramebuffers(1, intArr, 0);
            tempFrameBufferObject = intArr[0];
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, tempFrameBufferObject);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, tempTextureHandle, 0);

            ByteBuffer bb = ByteBuffer.allocateDirect(vertexCoords.length * sizeofFloat);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(vertexCoords);
            vertexBuffer.position(0);

            bb = ByteBuffer.allocateDirect(textureCoords.length * sizeofFloat);
            bb.order(ByteOrder.nativeOrder());
            textureBuffer = bb.asFloatBuffer();
            textureBuffer.put(textureCoords);
            textureBuffer.position(0);

            GLES20.glGenTextures(1, intArr, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, intArr[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            mInputTextureDataHandle = intArr[0];

            this.numRuns = numRuns;
            int kernelRadius = calculateGaussianKernelRadius(sigma);
            float[] normalWeights = new float[kernelRadius+1];
            calculateGaussianWeights(normalWeights, kernelRadius, sigma);
            int numOffsetsAndWeights = kernelRadius/2;
            float[] offsets = new float[numOffsetsAndWeights], weights = new float[numOffsetsAndWeights];
            calculateGaussianOffsetsAndWeightsOptimizedForLinearSampling(offsets, weights, normalWeights, kernelRadius);
            final String fragmentShaderString = createFragmentShaderString(offsets, weights, normalWeights);

            mProgramHandle = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgramHandle, Util.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderString));
            GLES20.glAttachShader(mProgramHandle, Util.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderString));
            GLES20.glLinkProgram(mProgramHandle);
            GLES20.glUseProgram(mProgramHandle);
            //
            int positionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
            GLES20.glEnableVertexAttribArray(positionHandle);
            GLES20.glVertexAttribPointer(positionHandle, POSITION_COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, vertexBuffer);
            //
            int textureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mInputTextureDataHandle);
            GLES20.glUniform1i(textureUniformHandle, 0);
            int textureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
            GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
            GLES20.glVertexAttribPointer(textureCoordinateHandle, TEXTURE_COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, textureBuffer);
        }

        private int calculateGaussianKernelRadius(double sigma) {
            double minimumWeightThreshold = 1.0 / 256.0; // bottom limit for the contribution of the outmost pixel
            int radius = (int) Math.sqrt( -2.0*sigma*sigma * Math.log( minimumWeightThreshold*Math.sqrt(2*Math.PI*sigma*sigma) ) );
            // due to the use of biliear interpolation there is no gain in using odd radii
            return radius + radius%2;
        }
        private void calculateGaussianWeights(float[] output, int kernelRadius, double sigma) {
            if (output.length < kernelRadius+1) throw new IllegalArgumentException("output must hold at least " + (kernelRadius+1) + "elements");

            float sum = 0.0f;
            for (int i = 0; i <= kernelRadius; ++i) {
                float weight = (float) Math.exp(-i*i / (2*sigma*sigma));
                output[i] = weight;
                sum += weight * (i == 0 ? 1 : 2);
            }
            // normalization
            for (int i = 0; i <= kernelRadius; ++i) {
                output[i] = output[i] / sum;
            }
        }
        private void calculateGaussianOffsetsAndWeightsOptimizedForLinearSampling(float[] offsets, float[] weights, float[] normalWeights, int kernelRadius) {
            int numOffsetsAndWeights = kernelRadius/2;
            if (offsets.length < numOffsetsAndWeights) throw new IllegalArgumentException("offsets must hold at least " + numOffsetsAndWeights + "elements");
            if (weights.length < numOffsetsAndWeights) throw new IllegalArgumentException("weights must hold at least " + numOffsetsAndWeights + "elements");
            if (normalWeights.length < kernelRadius+1) throw new IllegalArgumentException("normalWeights must hold at least " + (kernelRadius+1) + "elements");

            for (int i = 0; i < numOffsetsAndWeights; ++i) {
                int index1 = 2*i+1; float weight1 = normalWeights[index1];
                int index2 = 2*i+2; float weight2 = normalWeights[index2];
                float optimizedWeight = weight1 + weight2;
                weights[i] = optimizedWeight;
                offsets[i] = (weight1*index1 + weight2*index2) / optimizedWeight;
            }
        }
        private String createFragmentShaderString(float[] offsets, float[] weights, float[] normalWeights) {
            StringBuilder sb = new StringBuilder();
            sb.append("precision mediump float;\n")
                    .append("uniform sampler2D u_Texture;\n")
                    .append("uniform int u_IsHorizontal;\n")
                    .append("varying vec2 v_TexCoordinate;\n");
            sb.append("void main() {\n");
            sb.append("  vec2 singleStepOffset = (u_IsHorizontal == 0) ? vec2(").append(1.0f/imageWidth).append(", 0.0) : vec2(0.0, ").append(1.0f/imageHeight).append(");\n")
                    .append("  gl_FragColor = texture2D(u_Texture, v_TexCoordinate) * ").append(normalWeights[0]).append(";\n");
            for (int i = 0; i < weights.length; ++i) {
                sb.append("  gl_FragColor += texture2D(u_Texture, v_TexCoordinate + singleStepOffset*").append(offsets[i]).append(") * ").append(weights[i]).append(";\n")
                        .append("  gl_FragColor += texture2D(u_Texture, v_TexCoordinate - singleStepOffset*").append(offsets[i]).append(") * ").append(weights[i]).append(";\n");
            }
            sb.append("}");
            return sb.toString();
        }

        public void draw(float[] mvpMatrix, int[] viewport) {
            float[] identity = new float[16]; Matrix.setIdentityM(identity, 0);
            GLES20.glUseProgram(mProgramHandle);

            GLES20.glViewport(0, 0, imageWidth, imageHeight);
            GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix"), 1, false, identity, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, tempFrameBufferObject);
            int inputTexture = mInputTextureDataHandle, outputTexture = tempTextureHandle;
            for (int i = 1; i <= numRuns; ++i) {
                GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgramHandle, "u_IsHorizontal"), 0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTexture);
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, outputTexture, 0);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCoords.length / POSITION_COORDS_PER_VERTEX);

                if (i == numRuns) {
                    GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
                    GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix"), 1, false, mvpMatrix, 0);
                    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                } else {
                    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, inputTexture, 0);
                }
                GLES20.glUniform1i(GLES20.glGetUniformLocation(mProgramHandle, "u_IsHorizontal"), 1);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, outputTexture);
                GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCoords.length / POSITION_COORDS_PER_VERTEX);
            }
        }
    }
}
