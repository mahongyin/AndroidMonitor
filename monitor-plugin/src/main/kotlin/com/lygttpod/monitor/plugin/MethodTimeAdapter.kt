package com.lygttpod.monitor.plugin

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

/**
 * Created By Mahongyin
 * Date    2024/3/7 10:52
 * 方法耗时打印
 */
class MethodTimeAdapter( methodVisitor: MethodVisitor, access:Int, name:String?, descriptor:String?) : AdviceAdapter(Opcodes.ASM7, methodVisitor, access, name, descriptor) {
    private var startTimeLocal = -1 // 保存 startTime 的局部变量索引

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
    }

    @Override
    override fun onMethodEnter() {
        super.onMethodEnter();
        // 在onMethodEnter中插入代码 val startTime = System.currentTimeMillis()
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
            "java/lang/System",
            "currentTimeMillis",
            "()J",
            false)
        startTimeLocal = newLocal(Type.LONG_TYPE) // 创建一个新的局部变量来保存 startTime
        mv.visitVarInsn(Opcodes.LSTORE, startTimeLocal)
    }

    @Override
    override fun onMethodExit(opcode: Int) {
        // 在onMethodExit中插入代码 Log.i("tag", "Method: $name, timecost: " + (System.currentTimeMillis() - startTime))
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("Method: $name, timeCost: ");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
            "java/lang/StringBuilder",
            "<init>",
            "(Ljava/lang/String;)V",
            false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
            "java/lang/System",
            "currentTimeMillis",
            "()J",
            false);
        mv.visitVarInsn(Opcodes.LLOAD, startTimeLocal);
        mv.visitInsn(Opcodes.LSUB);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "append",
            "(J)Ljava/lang/StringBuilder;",
            false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
            "java/lang/StringBuilder",
            "toString",
            "()Ljava/lang/String;",
            false);
        mv.visitLdcInsn("monitor")//log的tag
        mv.visitInsn(Opcodes.SWAP)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
            "android/util/Log",
            "i",
            "(Ljava/lang/String;Ljava/lang/String;)I",
            false)
        mv.visitInsn(POP)
        super.onMethodExit(opcode);
    }
}