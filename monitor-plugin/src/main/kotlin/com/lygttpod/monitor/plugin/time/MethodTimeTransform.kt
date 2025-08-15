package com.lygttpod.monitor.plugin.time

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.objectweb.asm.ClassVisitor

/**
 * Created By Mahongyin
 * Date    2024/3/7 11:00
 *
 */
abstract class MethodTimeTransform : AsmClassVisitorFactory<InstrumentationParameters.None> {
    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor {
        //指定真正的ASM转换器
        return MethodTimeClassVisitor(nextClassVisitor)
    }

    // 通过classData中的当前类的信息，用来过滤哪些类需要执行字节码转换，
    // 这里支持通过类名，包名，注解，接口，父类等属性来组合判断
    override fun isInstrumentable(classData: ClassData): Boolean {
        //指定包名执行
        return classData.className.startsWith("com.android.monitor.demo")
        //默认执行
//        return true
    }
}