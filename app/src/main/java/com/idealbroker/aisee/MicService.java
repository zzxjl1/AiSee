package com.idealbroker.aisee;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MicService extends Service {
    public static final String MIMETYPE_AUDIO_AAC = "audio/mp4a-latm";
    // 输入源 麦克风
    private final static int AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    // 采样率 44.1kHz，所有设备都支持
    private final static int SAMPLE_RATE = 16000;
    // 通道 单声道，所有设备都支持
    private final static int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    // 精度 16 位，所有设备都支持
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    // 通道数 单声道
    private static final int CHANNEL_COUNT = 1;
    // 比特率
    private static final int BIT_RATE = 64000;
    private static final String TAG = "AudioEncoder";
    private static boolean paused;
    // 缓冲区字节大小
    private int mBufferSizeInBytes;
    private static AudioRecord mAudioRecord;
    private MediaCodec mMediaCodec;
    private Executor mExecutor = Executors.newSingleThreadExecutor();
    private FileOutputStream fos;

    public MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    public void createAudio() {
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (mBufferSizeInBytes <= 0) {
            throw new RuntimeException("AudioRecord is not available, minBufferSize: " + mBufferSizeInBytes);
        }
        Log.i(TAG, "createAudioRecord minBufferSize: " + mBufferSizeInBytes);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mAudioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, mBufferSizeInBytes);
        int state = mAudioRecord.getState();
        Log.i(TAG, "createAudio state: " + state + ", initialized: " + (state == AudioRecord.STATE_INITIALIZED));
    }

    public void createMediaCodec() throws IOException {
        MediaCodecInfo mediaCodecInfo = selectCodec(MIMETYPE_AUDIO_AAC);
        if (mediaCodecInfo == null) {
            throw new RuntimeException(MIMETYPE_AUDIO_AAC + " encoder is not available");
        }
        Log.i(TAG, "createMediaCodec: mediaCodecInfo " + mediaCodecInfo.getName());

        MediaFormat format = MediaFormat.createAudioFormat(MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);

        mMediaCodec = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    public void start() throws IOException {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                startAsync();
            }
        });
    }

    public static void pause() {
        paused = true;
    }

    public static void recontinue() {
        paused = false;
    }


    private void startAsync() {
        mMediaCodec.start();
        mAudioRecord.startRecording();
        byte[] buffer = new byte[mBufferSizeInBytes];
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
        try {
            while (true) {
                int readSize = mAudioRecord.read(buffer, 0, mBufferSizeInBytes);
                if (paused) continue;
                // MyApplication.baseWebSocket.send(ByteString.of(buffer));//DEBUG ONLY
                ////Transcriber.send(buffer);
                if (readSize > 0) {
                    int inputBufferIndex = mMediaCodec.dequeueInputBuffer(0);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                        inputBuffer.clear();
                        inputBuffer.put(buffer);
                        inputBuffer.limit(buffer.length);
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, readSize, 0, 0);
                    }

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                    while (outputBufferIndex >= 0) {
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        byte[] chunkAudio = new byte[bufferInfo.size + 7];// 7 is ADTS size
                        outputBuffer.get(chunkAudio, 7, bufferInfo.size);
                        outputBuffer.position(bufferInfo.offset);
                        fos.write(chunkAudio);
                        mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                        //MyApplication.baseWebSocket.send(ByteString.of(chunkAudio));

                    }
                } else {
                    Log.w(TAG, "read audio buffer error:" + readSize);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Log.i(TAG, "released");
            mAudioRecord.stop();
            mAudioRecord.release();
            mMediaCodec.stop();
            mMediaCodec.release();
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
