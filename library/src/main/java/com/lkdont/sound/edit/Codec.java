package com.lkdont.sound.edit;

/**
 * 声音编码解码器
 * <p>
 * Created by kidonliang on 2018/4/4.
 */

public class Codec {

    public static final String DECODER_MP3 = "mp3";
    public static final String DECODER_AAC = "libfdk_aac";

    public static final String ENCODER_MP3 = "libmp3lame";
    public static final String ENCODER_AAC = "libfdk_aac";

    /**
     * 声道
     */
    public enum ChannelLayout {

        /**
         * 单声道
         */
        MONO(1),

        /**
         * 立体声
         */
        STEREO(2);

        private final int channels;

        ChannelLayout(int channels) {
            this.channels = channels;
        }

        public int getChannels() {
            return channels;
        }
    }

    /**
     * 采样格式
     */
    public enum SampleFormat {

        AV_SAMPLE_FMT_U8,          ///< unsigned 8 bits
        AV_SAMPLE_FMT_S16,         ///< signed 16 bits
        AV_SAMPLE_FMT_S32,         ///< signed 32 bits
        AV_SAMPLE_FMT_FLT,         ///< float
        AV_SAMPLE_FMT_DBL,         ///< double

        AV_SAMPLE_FMT_U8P,         ///< unsigned 8 bits, planar
        AV_SAMPLE_FMT_S16P,        ///< signed 16 bits, planar
        AV_SAMPLE_FMT_S32P,        ///< signed 32 bits, planar
        AV_SAMPLE_FMT_FLTP,        ///< float, planar
        AV_SAMPLE_FMT_DBLP,        ///< double, planar
        AV_SAMPLE_FMT_S64,         ///< signed 64 bits
        AV_SAMPLE_FMT_S64P,        ///< signed 64 bits, planar
    }
}
