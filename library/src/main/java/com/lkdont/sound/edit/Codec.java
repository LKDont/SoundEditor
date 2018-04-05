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

}
