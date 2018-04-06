package com.lkdont.sound.player

/**
 * 播放状态
 *
 * Created by kidonliang on 2018/4/6.
 */
enum class Status {

    /**
     * 最初的默认状态
     */
    UNDEFINED,

    /**
     * 播放中
     */
    PLAYING,

    /**
     * 暂停中
     */
    PAUSING,

    /**
     * 已停止
     */
    STOPPED

}