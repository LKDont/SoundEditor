package com.lkdont.sound.edit;

import android.util.Log;

import java.util.Locale;

/**
 * 声音信息
 * <p>
 * Created by kidonliang on 2018/4/5.
 */

public class Info {

    private final String url;
    private final boolean network;

    public Info(String url) {
        this.url = url;
        network = url != null && url.startsWith("http");
    }

    private String codec = null;   // 解码器
    private long duration = 0;  // 时长
    private int sampleRate = 0; // 采样率
    private int channels = 0;   // 声道
    private long bitRate = 0;    // 比特率

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "Info={url:%s, codec:%s, duration:%d, sampleRate:%d, channels:%d, bitRate:%d}",
                url, codec, duration, sampleRate, channels, bitRate);
    }

    public String getUrl() {
        return url;
    }

    public boolean isNetwork() {
        return network;
    }

    public String getCodec() {
        return codec;
    }

    public long getDuration() {
        return duration;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public long getBitRate() {
        return bitRate;
    }

    //******************************** 本地方法 ********************************//

    private native int _load(String url, int network);

    public int load() {
        if (url == null) {
            Log.e("Info", "load audio info fail : url == null");
            return -1;
        }
        if (network) {
            return _load(url, 1);
        } else {
            return _load(url, 0);
        }
    }
}
