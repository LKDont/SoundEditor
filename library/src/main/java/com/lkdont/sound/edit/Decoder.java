package com.lkdont.sound.edit;

/**
 * 声音解码器
 * <p>
 * Created by kidonliang on 2018/4/4.
 */
public class Decoder {

    // 解码器初始化后，都会与native端的一个解码器结构体绑定，
    // 这个id就是对应的结构体地址
    private long nativeDecoderId = 0L;

    public long getNativeDecoderId() {
        return nativeDecoderId;
    }

    private native int _init(String codecName);

    private native int _feed_data(long decoder_id, byte[] data, int len);

    private native int _get_decoded_size(long decoder_id);

    private native int _receive_decoded_data(long decoder_id, byte[] data);

    private native void _flush(long decoder_id);

    private native void _close(long decoder_id);

    /**
     * 准备解码器
     *
     * @param codecName 解码器名字
     * @return 0：成功；其它：错误码。
     */
    public int init(String codecName) {
        return _init(codecName);
    }

    public int feedData(byte[] data, int len) {
        return _feed_data(nativeDecoderId, data, len);
    }

    public int getDecodedSize() {
        return _get_decoded_size(nativeDecoderId);
    }

    public int receiveDecodedData(byte[] data) {
        return _receive_decoded_data(nativeDecoderId, data);
    }

    public void flush() {
        _flush(nativeDecoderId);
    }

    public void close() {
        _close(nativeDecoderId);
    }
}
