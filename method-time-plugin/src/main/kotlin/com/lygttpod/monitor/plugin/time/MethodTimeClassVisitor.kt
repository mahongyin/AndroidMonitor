package com.lygttpod.monitor.plugin.time

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * Created By Mahongyin
 * Date    2024/3/7 11:02
 *
 */
class MethodTimeClassVisitor (nextVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM7, nextVisitor) {
    //方法耗时 插入Log.i
    override fun visitMethod(access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?): MethodVisitor {

        val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (name == "<clinit>" || name == "<init>") {//排除类初始方法
            return methodVisitor
        }
        val newMethodVisitor = MethodTimeAdapter(methodVisitor, access, name, descriptor)
        return newMethodVisitor
    }
}