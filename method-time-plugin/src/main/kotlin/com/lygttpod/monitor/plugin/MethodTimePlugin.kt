package com.lygttpod.monitor.plugin

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.lygttpod.monitor.plugin.time.MethodTimeTransform
import org.gradle.api.Plugin
import org.gradle.api.Project

class MethodTimePlugin : Plugin<Project> {
    //带配置参数的
    override fun apply(project: Project) {
//        var enableMonitorPlugin = true
//        val properties = Properties()
//        val file = project.rootProject.file("local.properties")
//        if (file.exists()) {
//            properties.load(file.inputStream())
//            enableMonitorPlugin =
//                properties.getProperty("monitor.enablePlugin", "true")?.toBoolean() ?: true
//        }
//        println("MonitorPlugin---->enableMonitorPlugin = $enableMonitorPlugin")
//        if (!enableMonitorPlugin) return

        //这里appExtension获取方式与原transform api不同，可自行对比
        val appExtension = project.extensions.getByType(AndroidComponentsExtension::class.java)
        //这里通过transformClassesWith替换了原registerTransform来注册字节码转换操作
        appExtension.onVariants { variant ->
            // 只在debug构建中启用监控插桩
            if ("debug".equals(variant.buildType, ignoreCase = true)) {
                println("MonitorPlugin---->enableMonitorPlugin2 = ${variant.buildType}")
                variant.instrumentation.apply {
                    //方法耗时
                    transformClassesWith(
                        MethodTimeTransform::class.java,
                        InstrumentationScope.PROJECT
                    ) {}
                    //InstrumentationScope.ALL 配合 FramesComputationMode.COPY_FRAMES指定该字节码转换器在全局生效，包括第三方lib
//                    setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
                    // PROJECT 搭配 COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS 指定该字节码转换器只在当前项目生效，不会对第三方依赖lib生效
                    setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)
                }
            }
        }
    }
}