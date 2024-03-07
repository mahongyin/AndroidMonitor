package com.lygttpod.monitor.plugin

import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input

/**
 * Created By Mahongyin
 * Date    2024/3/7 12:29
 * 新api的配置参数方式
 * Plugin中添加配置文件相关逻辑
 */
interface ConfigOkHttp : InstrumentationParameters {

    @get:Input
    val packageNames: ListProperty<String>
//    @get:Input
//    val packageNames: Property<String>
}