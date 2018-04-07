package com.lkdont.sound.edit;

/**
 * 声音转换器
 * <p>
 * Created by kidonliang on 2018/4/7.
 */

public class Convertor {

    private long nativeConvertorId = 0L;

    private native int _init(int src_channels, int src_sp_format_id, int src_sp_rate,
                             int dst_channels, int dst_sp_format_id, int dst_sp_rate);

    private native int _feed_data(long convertor_id, byte[] data, int len);

    private native int _get_converted_size(long convertor_id);

    private native int _receive_converted_data(long convertor_id, byte[] data);

    private native void _flush(long convertor_id);

    private native void _close(long convertor_id);

    public int init(Codec.ChannelLayout srcChannel, Codec.SampleFormat srcSpFormat, int srcSpRate,
                    Codec.ChannelLayout dstChannel, Codec.SampleFormat dstSpFormat, int dstSpRate) {
        return _init(srcChannel.getChannels(), srcSpFormat.ordinal(), srcSpRate,
                dstChannel.getChannels(), dstSpFormat.ordinal(), dstSpRate);
    }

    public int feedData(byte[] data, int len) {
        return _feed_data(nativeConvertorId, data, len);
    }

    public int getConvertedSize() {
        return _get_converted_size(nativeConvertorId);
    }

    public int receiveConvertedData(byte[] data) {
        return _receive_converted_data(nativeConvertorId, data);
    }

    public void flush() {
        _flush(nativeConvertorId);
    }

    public void close() {
        _close(nativeConvertorId);
    }
}
