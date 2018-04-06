package com.lkdont.sound.player

/**
 * 播放事件
 *
 * Created by kidonliang on 2018/4/6.
 */
interface PlayEventListener {

    fun onUpdatingTime(curTime: Long, totalTime: Long)

    fun onUpdatingStatus(status: Status)
}
