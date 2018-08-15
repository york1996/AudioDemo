package com.york1996.audiodemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "AudioDemo";

    public static final int SAMPLE_RATE_IN_HZ = 44100;//采样率
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;//声道数
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;//音频数据格式

    private Button mBtnRecord;
    private Button mBtnStop;
    private Button mBtnPlay;
    private Button mBtnStopPlay;

    private AudioRecord mAudioRecord;
    private AudioTrack mAudioTrack;
    private boolean mIsRecording = false;

    private String[] permissions = new String[]{
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private List<String> mPermissionList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermissions();

        mBtnRecord = findViewById(R.id.btn_record);
        mBtnStop = findViewById(R.id.btn_stop);
        mBtnPlay = findViewById(R.id.btn_play);
        mBtnStopPlay = findViewById(R.id.btn_stop_play);

        mBtnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecord();
            }
        });

        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecord();
            }
        });

        mBtnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                play();
            }
        });

        mBtnStopPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopPlay();
            }
        });
    }

    private void startRecord() {
        final int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
        final byte data[] = new byte[minBufferSize];
        final File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        if (file.exists()) {
            file.delete();
        }

        mAudioRecord.startRecording();
        mIsRecording = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                if (fos != null) {
                    while (mIsRecording) {
                        int read = mAudioRecord.read(data, 0, data.length);
                        if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                            try {
                                fos.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void stopRecord() {
        mIsRecording = false;
        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
        }
    }

    private void play() {
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        final int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_IN_HZ, channelConfig, AUDIO_FORMAT);
        mAudioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                new AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE_IN_HZ)
                        .setEncoding(AUDIO_FORMAT)
                        .setChannelMask(channelConfig)
                        .build(),
                minBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
//        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE_IN_HZ, AUDIO_FORMAT, minBufferSize, AudioTrack.MODE_STREAM);
        mAudioTrack.play();


        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "test.pcm");
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                byte[] buffer = new byte[minBufferSize];
                while (fis != null) {
                    try {
                        int readCount = fis.read(buffer);
                        if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                            continue;
                        }
                        if (readCount != 0 && readCount != -1) {
                            mAudioTrack.write(buffer, 0, readCount);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void stopPlay() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }

    private void checkPermissions() {
        // Marshmallow开始才用申请运行时权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (ContextCompat.checkSelfPermission(this, permissions[i]) !=
                        PackageManager.PERMISSION_GRANTED) {
                    mPermissionList.add(permissions[i]);
                }
            }
            if (!mPermissionList.isEmpty()) {
                String[] permissions = mPermissionList.toArray(new String[mPermissionList.size()]);
                ActivityCompat.requestPermissions(this, permissions, 1001);
            }
        }
    }
}
