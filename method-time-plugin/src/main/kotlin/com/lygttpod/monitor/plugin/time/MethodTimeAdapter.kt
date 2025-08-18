package com.lygttpod.monitor.plugin.time

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

/**
 * Created By Mahongyin
 * Date    2024/3/7 10:52
 * 方法耗时打印
 */
class MethodTimeAdapter( methodVisitor: MethodVisitor, access:Int, name:String?, descriptor:String?) : AdviceAdapter(
    ASM9, methodVisitor, access, name, descriptor) {
    private var startTimeLocal = -1 // 保存 startTime 的局部变量索引

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
    }

    @Override
    override fun onMethodEnter() {
        super.onMethodEnter();
        // 在onMethodEnter中插入代码 val startTime = System.currentTimeMillis()
        mv.visitMethodInsn(
            INVOKESTATIC,
            "java/lang/System",
            "currentTimeMillis",
            "()J",
            false)
        startTimeLocal = newLocal(Type.LONG_TYPE) // 创建一个新的局部变量来保存 startTime
        mv.visitVarInsn(LSTORE, startTimeLocal) //存给变量
    }

    @Override
    override fun onMethodExit(opcode: Int) {
        val logTag = "MethodTime"
        // 在onMethodExit中插入代码 Log.i("$logTag", "Method: $name, timeCost: " + (System.currentTimeMillis() - startTime))
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("Method: $name, timeCost: ");
        mv.visitMethodInsn(
            INVOKESPECIAL,
            "java/lang/StringBuilder",
            "<init>",
            "(Ljava/lang/String;)V",
            false);
        mv.visitMethodInsn(
            INVOKESTATIC,
            "java/lang/System",
            "currentTimeMillis",
            "()J",
            false);
        mv.visitVarInsn(LLOAD, startTimeLocal); //从变量取出
        mv.visitInsn(LSUB);
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "append",
            "(J)Ljava/lang/StringBuilder;",
            false);
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "toString",
            "()Ljava/lang/String;",
            false);
        mv.visitLdcInsn(logTag)//log的tag
        mv.visitInsn(SWAP)
        mv.visitMethodInsn(
            INVOKESTATIC,
            "android/util/Log",
            "i",
            "(Ljava/lang/String;Ljava/lang/String;)I",
            false)
        mv.visitInsn(POP)
        super.onMethodExit(opcode);
    }
}