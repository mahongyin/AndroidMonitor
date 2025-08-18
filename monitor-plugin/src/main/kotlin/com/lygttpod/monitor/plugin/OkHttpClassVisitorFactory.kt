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
                                // 加载 Builder 实例（this）
                                mv.visitVarInsn(Opcodes.ALOAD, 0)
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
                                /*mv.visitLdcInsn("com/lygttpod/monitor/MonitorHelper\$INSTANCE")
                                    mv.visitMethodInsn(
                                        Opcodes.INVOKESTATIC,
                                        "com/lygttpod/monitor/MonitorHelper\$INSTANCE",
                                        "getHookInterceptors",
                                        "()Ljava/util/List;",
                                        false
                                    )*/
                                mv.visitVarInsn(Opcodes.ASTORE, 10) // 保存到局部变量10

                                // 获取 Builder 实例的 interceptors 字段
                                mv.visitVarInsn(Opcodes.ALOAD, 0) // this（Builder实例）
                                mv.visitFieldInsn(
                                    Opcodes.GETFIELD,
                                    "okhttp3/OkHttpClient\$Builder",
                                    "interceptors",
                                    "Ljava/util/List;"
                                )
                                mv.visitVarInsn(Opcodes.ALOAD, 10) // 取出局部变量10
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
