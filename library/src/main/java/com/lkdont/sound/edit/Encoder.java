package com.lkdont.sound.edit;

/**
 * 声音编码器
 */
public class Encoder {

    private long nativeEncoderId = 0L;

    private native int _init(String codecName,
                             int channels, int sample_rate, int sample_fmt);

    private native int _feed_data(long encoder_id, byte[] data, int len);

    private native int _get_encoded_size(long encoder_id);

    private native int _receive_encoded_data(long encoder_id, byte[] data);

    private native int _flush(long encoder_id);

    private native void _close(long encoder_id);

    /**
     * 初始化编码器
     *
     * @param codecName    编码器名字
     * @param channel      声道
     * @param sampleRate   采样率
     * @param sampleFormat 采样格式
     * @return 0：成功；其它：错误
     */
    public int init(String codecName,
                    Codec.ChannelLayout channel, int sampleRate, Codec.SampleFormat sampleFormat) {
        return _init(codecName, channel.getChannels(), sampleRate, sampleFormat.ordinal());
    }

    public int feedData(byte[] data, int len) {
        return _feed_data(nativeEncoderId, data, len);
    }

    public int getEncodedSize() {
        return _get_encoded_size(nativeEncoderId);
    }

    public int receiveEncodedData(byte[] data) {
        return _receive_encoded_data(nativeEncoderId, data);
    }

    public int flush() {
        return _flush(nativeEncoderId);
    }

    public void close() {
        _close(nativeEncoderId);
    }
}
