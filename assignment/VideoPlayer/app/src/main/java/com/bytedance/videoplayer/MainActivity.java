package com.bytedance.videoplayer;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.VideoView;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Homework";
    private String videoUri = "https://lf6-hscdn-tos.pstatp.com/obj/developer-baas/baas/tt7217xbo2wz3cem41/d76174be59f40433_1563285205499.mp4";
    private VideoView videoView;
    private Button playOrPause;
    private Button fullOrSmall;
    private SeekBarHandler seekBarHandler;
    private SeekBar seekBar;
    private Configuration configuration;
    private SeekBarChangeThread seekBarChangeThread;
    private boolean isFirstCreate = true;
    //Handler
    private static class SeekBarHandler extends Handler{
        private WeakReference<MainActivity> mainActivityWeakReference;
        private SeekBar seekBar;
        private VideoView videoView;

        SeekBarHandler(MainActivity activity, SeekBar seekBar, VideoView videoView){
            mainActivityWeakReference = new WeakReference<>(activity);
            this.seekBar = seekBar;
            this.videoView = videoView;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity activity = mainActivityWeakReference.get();
            if(activity != null){
                if (msg.what == 1){
                    float progress = (float)(videoView.getCurrentPosition()) / (float)(videoView.getDuration()) * seekBar.getMax();
                    Log.d(TAG, "handleMessage: " + progress);
                    seekBar.setProgress((int)progress);
                }
                if(msg.what == 2){
                    int progress = seekBar.getProgress();
                    videoView.seekTo(videoView.getDuration() * progress / seekBar.getMax());
                }
            }
        }
    }

    private void PlayVideo(){
        videoView.start();
        playOrPause.setText(getString(R.string.pause));
        if(seekBarChangeThread == null){
            seekBarChangeThread = new SeekBarChangeThread(seekBarHandler);
        }
        seekBarChangeThread.start();
    }

    private void PauseVideo(){
        videoView.pause();
        playOrPause.setText(getString(R.string.play));
        seekBarChangeThread.exit = true;
        seekBarChangeThread = null;
    }

    private void initAll(){
        videoView = findViewById(R.id.video_view);
        playOrPause = findViewById(R.id.play_pause);
        fullOrSmall = findViewById(R.id.full_small);
        seekBar = findViewById(R.id.seekBar);
        //设置视频URI
        videoView.setVideoURI(Uri.parse(videoUri));
        configuration = getResources().getConfiguration();
        int hOrv = configuration.orientation;
        if(hOrv == configuration.ORIENTATION_PORTRAIT){
            fullOrSmall.setText(getString(R.string.full));
        }
        else {
            fullOrSmall.setText(getString(R.string.small));
        }

        seekBarHandler = new SeekBarHandler(this, seekBar, videoView);
        seekBarChangeThread = new SeekBarChangeThread(seekBarHandler);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initAll();

        playOrPause.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(playOrPause.getText().toString().equals(getString(R.string.play))){
                    PlayVideo();
                }
                else {
                    PauseVideo();
                }
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean mChange = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mChange){
                    int targetTime = (int)((float)progress / (float)seekBar.getMax() * videoView.getDuration());
                    videoView.seekTo(targetTime);
                    Log.d(TAG, "onProgressChanged: ");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mChange = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mChange = false;
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                seekBarChangeThread.exit = true;
                seekBarChangeThread = null;

                seekBar.setProgress(100);
                playOrPause.setText(getString(R.string.play));
            }
        });

        fullOrSmall.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                configuration = getResources().getConfiguration();
                int hOrv = configuration.orientation;
                if(hOrv == configuration.ORIENTATION_PORTRAIT){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
                else{
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        });
        Log.d(TAG, "onCreate: " + videoView);
    }

    public class SeekBarChangeThread extends Thread{
        private Handler seekBarHandler;
        public boolean exit = false;

        SeekBarChangeThread(Handler seekBarHandler){
            this.seekBarHandler = seekBarHandler;
        }

        @Override
        public void run() {
            while(!exit){
                try{
                    if(isFirstCreate){
                        sleep(200);
                        seekBarHandler.sendEmptyMessage(2);
                        isFirstCreate = false;
                    }
                    else{
                        seekBarHandler.sendEmptyMessage(1);
                    }
                    sleep(300);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

}
