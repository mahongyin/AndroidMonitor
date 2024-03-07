package com.lygttpod.monitor.plugin

import com.lygttpod.monitor.okhttp.OkHttpMethodAdapter
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class OkHttpClassVisitor8(nextVisitor: ClassVisitor,val config: ConfigOkHttp) : ClassVisitor(Opcodes.ASM7, nextVisitor) {
    private var className: String? = null

    override fun visit(version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?) {
        super.visit(version, access, name, signature, superName, interfaces)
        this.className = name
    }

    override fun visitMethod(access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?): MethodVisitor? {
        val methodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions)

        //如果不在配置的方法名列表中，不执行
//        val methodNameConfig = config.methodNames.get()
//        if (methodNameConfig.isNotEmpty()) {
//            if (methodNameConfig.none { name == it }) {
//                return methodVisitor
//            }
//        }
        return if (className == "okhttp3/OkHttpClient\$Builder" && name == "<init>") {
            if (methodVisitor == null) {
                null //为啥不用super，因为super返回的是null?
            } else {
                OkHttpMethodAdapter(methodVisitor, access, name, descriptor)
            }
        } else {
            methodVisitor
        }
    }

//InstrumentationParameters，插件配置参数

}