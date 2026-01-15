package com.lygttpod.monitor.plugin

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


abstract class OkHttpClassVisitorFactory : AsmClassVisitorFactory<InstrumentationParameters.None> {
    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor): ClassVisitor {
//        println("插件b："+classContext.currentClassData.className)
//        if (classContext.currentClassData.className != "okhttp3.OkHttpClient\$Builder") {
//            return nextClassVisitor
//        }
        return object : ClassVisitor(Opcodes.ASM7, nextClassVisitor) {
            override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
                val mv = super.visitMethod(access, name, desc, signature, exceptions)
                // 匹配 Builder 的构造方法
                if ("<init>" == name && "()V" == desc) {
                    return object : MethodVisitor(api, mv) {
                        override fun visitInsn(opcode: Int) {
                            // 在构造方法返回前插入代码
                            if (opcode == Opcodes.RETURN) {
                                // 调用静态方法MonitorHelper.getHookInterceptors() 获取自定义拦截器列表
                                mv.visitFieldInsn(
                                    Opcodes.GETSTATIC,
                                    "com/lygttpod/monitor/MonitorHelper",
                                    "INSTANCE",
                                    "Lcom/lygttpod/monitor/MonitorHelper;"
                                )
                                mv.visitMethodInsn(
                                    Opcodes.INVOKEVIRTUAL,
                                    "com/lygttpod/monitor/MonitorHelper",
                                    "getHookInterceptors",
                                    "()Ljava/util/List;",
                                    false
                                )

                                // 复制栈顶的 List（需要用于 addAll 调用）
                                mv.visitInsn(Opcodes.DUP)

                                // 获取 Builder 实例的 interceptors 字段
                                mv.visitVarInsn(Opcodes.ALOAD, 0) // this（Builder实例）
                                mv.visitFieldInsn(
                                    Opcodes.GETFIELD,
                                    "okhttp3/OkHttpClient\$Builder",
                                    "interceptors",
                                    "Ljava/util/List;"
                                )

                                // 交换栈顶两个元素，使 hookInterceptors 在上面，interceptors 在下面
                                mv.visitInsn(Opcodes.SWAP)

                                // 调用 interceptors.addAll(hookInterceptors)
                                mv.visitMethodInsn(
                                    Opcodes.INVOKEINTERFACE,
                                    "java/util/List",
                                    "addAll",
                                    "(Ljava/util/Collection;)Z",
                                    true
                                )
                                mv.visitInsn(Opcodes.POP) // 弹出返回值（boolean）
                            }
                            super.visitInsn(opcode)
                        }

                    }
                }
                return mv
            }
        }
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        //println("插件a："+classData.className)
        return classData.className == "okhttp3.OkHttpClient\$Builder"
    }

}
