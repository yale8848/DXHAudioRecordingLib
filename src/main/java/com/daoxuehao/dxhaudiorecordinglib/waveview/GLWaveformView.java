package com.daoxuehao.dxhaudiorecordinglib.waveview;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GLWaveformView.
 *
 * @author kang
 * @since 14/9/25.
 */
public class GLWaveformView extends GLSurfaceView {

    private static final int MSG_NOTIFY_REFRESH = 0x1001;

    private float mPrimaryWidth = 5.0f;
    private float mSecondaryWidth = 1.0f;
    private float mAmplitude = 0.075f;
    private float mDensity = 2f;
    private int mWaveCount = 5;
    private float mFrequency = 0.1875f;
    private float mPhaseShift = -0.15f;
    private float mPhase = mPhaseShift;
    private GLRender mGLRender;
    private Handler mHandler;
    private List<Wave> mWaves;
    private CalThread mCalThread;

    public GLWaveformView(Context context) {
        this(context, null);
    }

    public GLWaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHandler = new Handler(new Handler.Callback() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean handleMessage(Message msg) {
                if (msg.what == MSG_NOTIFY_REFRESH) {
                    List<Wave> waves = (List<Wave>) msg.obj;
                    if (waves != null) {
                        mWaves = waves;
                    }
                }

                return true;
            }
        });

/*
        setEGLConfigChooser(
                new EGLConfigChooser() {

                    @Override
                    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
                        int[] attrList = new int[]{ //
                                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_WINDOW_BIT, //
                                EGL10.EGL_RED_SIZE, 8, //
                                EGL10.EGL_GREEN_SIZE, 8, //
                                EGL10.EGL_BLUE_SIZE, 8, //
                                EGL10.EGL_DEPTH_SIZE, 16, //
                                EGL10.EGL_SAMPLE_BUFFERS, 1,
                                EGL10.EGL_SAMPLES, 2,
                                EGL10.EGL_NONE //
                        };
                        EGLConfig[] configOut = new EGLConfig[1];
                        int[] configNumOut = new int[1];
                        egl.eglChooseConfig(display, attrList, configOut, 1, configNumOut);
                        return configOut[0];
                    }
                }
        );
*/

        mGLRender = new GLRender();
        setRenderer(mGLRender);
    }

    public void onPause() {
        if (mCalThread != null) {
            mCalThread.finish();
        }
    }

    public void onResume() {
        if (mCalThread != null) {
            mCalThread.finish();
        }

        mCalThread = new CalThread(mHandler);
        mCalThread.start();
    }

    private class Wave {
        List<Float> vertexes;
        float alpha;
        boolean isPrimary;
        FloatBuffer buffer;

        public Wave() {
            vertexes = new ArrayList<Float>();
        }

        void updateBuffer() {
            if (vertexes == null || vertexes.isEmpty()) {
                buffer = null;
                return;
            }

            int size = vertexes.size();
            float[] arrays = new float[size];

            for (int i = 0; i < size; ++i) {
                arrays[i] = vertexes.get(i);
            }

            ByteBuffer vbb = ByteBuffer.allocateDirect(arrays.length * 4);
            vbb.order(ByteOrder.nativeOrder());
            buffer = vbb.asFloatBuffer();
            buffer.put(arrays);
            buffer.position(0);
        }
    }

    private class CalThread extends Thread {
        boolean stop = false;
        Handler mHandler;

        public CalThread(Handler handler) {
            mHandler = handler;
        }

        @Override
        public void run() {
            super.run();
            while (!isStop()) {
                List<Wave> waves = generateVertex(getWidth(), getHeight());

                mPhase += mPhaseShift;

                Message message = mHandler.obtainMessage();
                message.what = MSG_NOTIFY_REFRESH;
                message.obj = waves;
                mHandler.sendMessageDelayed(message, 100);
            }
        }

        public synchronized void finish() {
            stop = true;
        }

        public synchronized boolean isStop() {
            return stop;
        }
    }

    private List<Wave> generateVertex(int width, int height) {
        List<Wave> waves = new ArrayList<Wave>();

        for (int l = 0; l < mWaveCount; ++l) {
            Wave wave = new Wave();

            float midH = height / 2.0f;
            float midW = width / 2.0f;

            float maxAmplitude = midH - 4.0f;
            float progress = 1.0f - l * 1.0f / mWaveCount;
            float normalAmplitude = (1.5f * progress - 0.5f) * mAmplitude;

            wave.isPrimary = l == 0;
            wave.alpha = wave.isPrimary ? 1f : (float) Math.min(1.0, (progress / 3.0f * 2.0f) + (1.0f / 3.0f));

            for (float x = 0; x < width + mDensity; x += mDensity) {
                float scaling = 1f - (float) Math.pow(1 / midW * (x - midW), 2);
                float y = scaling * maxAmplitude * normalAmplitude * (float) Math.sin(
                        180 * x * mFrequency / (width * Math.PI) + mPhase);
                wave.vertexes.add(x * 2f / width);
                wave.vertexes.add(y * 1f / height);
            }

            wave.updateBuffer();
            waves.add(wave);
        }

        return waves;
    }

    class GLRender implements Renderer {
        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            gl.glClearColor(0f, 0f, 0f, 1f);

            gl.glEnable(GL10.GL_BLEND);
            gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);


        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            gl.glViewport(0, 0, width, height);
            /*float ratio = (float) width / height;
            // 设置投影矩阵
            gl.glMatrixMode(GL10.GL_PROJECTION);

            // 重置投影矩阵
            gl.glLoadIdentity();
            // 设置视口的大小
            GLU.gluPerspective(gl, 45.0f, ratio, 0.1f, 100.0f);
            //以下两句告诉opengl es，以后所有的变换都将影响这个模型(即我们绘制的图形)
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();*/
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (mWaves != null) {
                gl.glClearColor(0f, 0f, 0f, 1f);
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
                gl.glLoadIdentity();

                // 开启顶点缓存写入功能
                gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

                gl.glTranslatef(-1f, 0f, 0f);
                for (Wave wave : mWaves) {
                    if (wave.buffer == null) {
                        continue;
                    }

                    gl.glColor4f(0.5f, 0.5f, 0.5f, 1f);

                    if (wave.isPrimary) {
                        gl.glLineWidth(mPrimaryWidth);
                    } else {
                        gl.glLineWidth(mSecondaryWidth);
                    }

                    gl.glVertexPointer(2, GL10.GL_FLOAT, 0, wave.buffer);
                    gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, wave.vertexes.size() / 2);
                }

                gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            }
        }
    }
}
