package com.lygttpod.monitor.plugin

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
//import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

/** 配置包名
 * MonitorPlugin {
 *     includePackages = ['com.xx.xxx']
 * }
 */
class MonitorPlugin : Plugin<Project> {

//     fun apply7(project: Project) {
//        var enableMonitorPlugin = true
//        val properties = Properties()
//        val file = project.rootProject.file("local.properties")
//        if (file.exists()) {
//            properties.load(file.inputStream())
//            enableMonitorPlugin = properties.getProperty("monitor.enablePlugin", "true")?.toBoolean() ?: true
//        }
//        println("MonitorPlugin---->enableMonitorPlugin = $enableMonitorPlugin")
//        if (!enableMonitorPlugin) return
//        //这里appExtension获取方式 transform api
//        try {
//            val appException: AppExtension = project.extensions.getByName("android") as AppExtension
//            appException.registerTransform(OkHttpTransform(project))
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
    //agp8.0+
    fun apply8(project: Project) {
        var enableMonitorPlugin = true
        val properties = Properties()
        val file = project.rootProject.file("local.properties")
        if (file.exists()) {
            properties.load(file.inputStream())
            enableMonitorPlugin = properties.getProperty("monitor.enablePlugin", "true")?.toBoolean() ?: true
        }
        println("MonitorPlugin---->enableMonitorPlugin = $enableMonitorPlugin")
        if (!enableMonitorPlugin) return

        //这里appExtension获取方式与原transform api不同，可自行对比
        val appExtension = project.extensions.getByType(AndroidComponentsExtension::class.java)
        //这里通过transformClassesWith替换了原registerTransform来注册字节码转换操作
        // 注册单个字节码转换器任务
//        appExtension.onVariants { variant ->
//            //可以通过variant来获取当前编译环境的一些信息，最重要的是可以 variant.name 来区分是debug模式还是release模式编译
//            variant.instrumentation.transformClassesWith(OkHttpTransform8::class.java, InstrumentationScope.ALL) {
//            }
//            //InstrumentationScope.ALL 配合 FramesComputationMode.COPY_FRAMES可以指定该字节码转换器在全局生效，包括第三方lib
//            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
//        }
        // 注册多个字节码转换器任务
        appExtension.onVariants { variant ->
            //可以通过variant来获取当前编译环境的一些信息，最重要的是可以 variant.name 来区分是debug模式还是release模式编译
            variant.instrumentation.transformClassesWith(OkHttpTransform8::class.java, InstrumentationScope.ALL) {
            }
            //方法耗时
            variant.instrumentation.transformClassesWith(MethodTimeTransform::class.java, InstrumentationScope.ALL) {
            }
            //InstrumentationScope.ALL 配合 FramesComputationMode.COPY_FRAMES可以指定该字节码转换器在全局生效，包括第三方lib
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
        }
    }
    //带配置参数的
    override fun apply(project: Project) {
        var enableMonitorPlugin = true
        val properties = Properties()
        val file = project.rootProject.file("local.properties")
        if (file.exists()) {
            properties.load(file.inputStream())
            enableMonitorPlugin = properties.getProperty("monitor.enablePlugin", "true")?.toBoolean() ?: true
        }
        println("MonitorPlugin---->enableMonitorPlugin = $enableMonitorPlugin")
        if (!enableMonitorPlugin) return

        //这里appExtension获取方式与原transform api不同，可自行对比
        val appExtension = project.extensions.getByType(AndroidComponentsExtension::class.java)
        //读取配置文件 对应MonitorPlugin { includePackages = ['com.xx.xxx']}
        project.extensions.create("MonitorPlugin", ConfigExtension::class.java)
        //这里通过transformClassesWith替换了原registerTransform来注册字节码转换操作
        appExtension.onVariants { variant ->
            //新api方式，配置获取
            val extensionNew = project.extensions.getByType(ConfigExtension::class.java)
            //可以通过variant来获取当前编译环境的一些信息，最重要的是可以 variant.name 来区分是debug模式还是release模式编译
            variant.instrumentation.transformClassesWith(OkHttpTransform8::class.java, InstrumentationScope.ALL) {
                //配置通过指定配置的类，携带到TimeCostTransform中
                it.packageNames.set(extensionNew.includePackages.toList())
                //it.packageNames.set(extensionNew.includePackages)
            }
            //方法耗时
//            variant.instrumentation.transformClassesWith(MethodTimeTransform::class.java, InstrumentationScope.ALL) {
//                //如果MethodTimeTransform none没配置,就不需要这里赋值了
//                it.packageNames.set(extensionNew.includePackages.toList())
//            }
            //InstrumentationScope.ALL 配合 FramesComputationMode.COPY_FRAMES可以指定该字节码转换器在全局生效，包括第三方lib
            variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COPY_FRAMES)
        }
    }
}