package com.daoxuehao.dxhaudiorecordinglib;

import android.media.MediaRecorder;
import android.os.Handler;

/**
 * Created by Yale on 2016/6/12.
 * http://blog.csdn.net/greatpresident/article/details/38402147 分贝计算
 * http://www.cnblogs.com/Amandaliu/archive/2013/02/04/2891604.html 录音api及demo
 */
public enum SimpleMediaAudioRecorder {

    INSTANCE;
    private int BASE = 1;
    private int SPACE = 100;// 间隔取样时间
    private MediaRecorder mMediaRecorder ;

    private DecibelListener mDecibelListener;
    private Runnable mUpdateMicStatusTimer;
    private final Handler mHandler = new Handler();
    private void initRecorder(){



        mMediaRecorder  = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
        /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    }

    private void release(){
        if (mMediaRecorder !=null){
            mMediaRecorder .stop();
            mMediaRecorder .release();
            mMediaRecorder = null;

        }
        if (mUpdateMicStatusTimer!=null){
            mHandler.removeCallbacks(mUpdateMicStatusTimer);
        }
    }

    public void setDecibelListener(DecibelListener decibelListener){
        mDecibelListener = decibelListener;
    }
    private void updateMicStatus() {
        if (mMediaRecorder != null) {
            double ratio = (double)mMediaRecorder.getMaxAmplitude() /BASE;
            double db = 0;// 分贝
            if (ratio > 1)
                db = 20 * Math.log10(ratio);

            if (mDecibelListener!=null){
                mDecibelListener.decibel(db);
            }
            mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
        }
    }
    public void start(String path,ErrorListener errorListener){

        try {
            release();
            initRecorder();

            mMediaRecorder.setOutputFile(path);
            mMediaRecorder.prepare();
            mMediaRecorder.start();

            mUpdateMicStatusTimer  = new Runnable() {
                public void run() {
                    updateMicStatus();
                }
            };
            updateMicStatus();
        }catch (Exception e){
            e.printStackTrace();
            if(errorListener!=null){
                errorListener.error(e);
            }
        }
    }
    public void stop(){
        release();
    }

  public   interface ErrorListener {
        void error(Exception e);
    }
    public   interface DecibelListener{
        void decibel(double decibel);
    }
}
