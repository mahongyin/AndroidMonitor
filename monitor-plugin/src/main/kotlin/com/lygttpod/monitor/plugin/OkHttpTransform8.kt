package com.lygttpod.monitor.plugin

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import org.objectweb.asm.ClassVisitor

// 通过继承AsmClassVisitorFactory，来实现自定义的字节码转换器  插件运行可以携带一些配置参数ConfigExtension
abstract class OkHttpTransform8 : AsmClassVisitorFactory</*InstrumentationParameters.None*/ConfigOkHttp> {
    override fun createClassVisitor(classContext: ClassContext,
        nextClassVisitor: ClassVisitor): ClassVisitor {
        //指定真正的ASM转换器
        return OkHttpClassVisitor8(nextClassVisitor,/*加上配置参数*/parameters.get())
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
        } else {//不执行，看自己需求了 release 完全可以设置不执行
           // return false
        }
        //默认执行
        return true
    }
}