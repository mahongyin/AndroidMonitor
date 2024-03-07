package com.lygttpod.monitor.plugin

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import org.objectweb.asm.ClassVisitor

/**
 * Created By Mahongyin
 * Date    2024/3/7 11:00
 *
 */
abstract class MethodTimeTransform : AsmClassVisitorFactory</*InstrumentationParameters.None*/ConfigOkHttp> {
    override fun createClassVisitor(classContext: ClassContext,
        nextClassVisitor: ClassVisitor): ClassVisitor {
        //指定真正的ASM转换器
        return MethodTimeClassVisitor(nextClassVisitor)
    }

    // 通过classData中的当前类的信息，用来过滤哪些类需要执行字节码转换，
    // 这里支持通过类名，包名，注解，接口，父类等属性来组合判断
    override fun isInstrumentable(classData: ClassData): Boolean {
        //指定包名执行
        //return classData.className.startsWith("com.xxx.app")
        //通过parameters.get()来获取传递的配置参数 包名
        val packageConfig = parameters.get().packageNames.get()
        if (packageConfig.isNotEmpty()) {
            //包含包名就执行
            return packageConfig.any { classData.className.contains(it) }
        }
        //默认执行
        return true
    }
}