/*
 * Copyright © 2020 rwsbillyang@qq.com.  All Rights Reserved.
 *
 * Written by rwsbillyang@qq.com at Beijing Time: 2020-10-10 23:18
 *
 */

package com.github.rwsbillyang.wxOA.stats


import org.koin.dsl.module

/**
 * 将接收到的事件event写入统计表
 * */

internal val statsModule = module {
    //single { FanController(get()) }
    single { StatsService(get()) }
}
