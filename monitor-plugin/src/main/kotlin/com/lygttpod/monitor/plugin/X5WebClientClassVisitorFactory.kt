package com.lygttpod.monitor.plugin

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter


/**
 * Created By Mahongyin
 * Date    2025/9/11 11:34
 *
 */
abstract class X5WebClientClassVisitorFactory :
    AsmClassVisitorFactory<InstrumentationParameters.None> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        //println("插件w2："+classContext.toString())
        return object : ClassVisitor(Opcodes.ASM7, nextClassVisitor) {
            var className: String? = null;
            override fun visit(
                version: Int,
                access: Int,
                name: String?,
                signature: String?,
                superName: String?,
                interfaces: Array<String?>?
            ) {
                super.visit(version, access, name, signature, superName, interfaces)
                this.className = name
                //println("插件w3："+className)
            }

            override fun visitMethod(
                access: Int,
                name: String?,
                desc: String?,
                signature: String?,
                exceptions: Array<String?>?
            ): MethodVisitor? {
                val mv = super.visitMethod(access, name, desc, signature, exceptions)
                // 处理所有方法中的setWebViewClient调用
                return object : AdviceAdapter(api, mv, access, name, desc) {

                    override fun visitMethodInsn(
                        opcode: Int,
                        owner: String?,
                        name: String?,
                        desc: String?,
                        itf: Boolean
                    ) {
                        if (opcode == INVOKEVIRTUAL &&
                            "com/tencent/smtt/sdk/WebView" == owner &&
                            "setWebViewClient" == name &&
                            "(Lcom/tencent/smtt/sdk/WebViewClient;)V" == desc
                        ) { // 拦截X5 WebView的setWebViewClient调用
                            println("插件X5：$className#$owner.$name$desc")
                            // 将原始WebViewClient存储在局部变量中
                            val originalClientVar =
                                newLocal(Type.getType("Lcom/tencent/smtt/sdk/WebViewClient;"))
                            mv.visitVarInsn(ASTORE, originalClientVar)

                            // 创建新的WebView存储在局部变量中
                            val webViewVarIndex =
                                newLocal(Type.getType("Lcom/tencent/smtt/sdk/WebView;"));
                            mv.visitVarInsn(ASTORE, webViewVarIndex);

                            //先获取该 object的单例实例 INSTANCE字段
                            mv.visitFieldInsn(
                                GETSTATIC, // 指令
                                "com/lygttpod/monitor/MonitorHelper", // 类名
                                "INSTANCE", // 字段名 (指向单例实例)
                                "Lcom/lygttpod/monitor/MonitorHelper;" // 字段描述符 (类型是该object自身)
                            );
                            // 准备参数
                            mv.visitVarInsn(ALOAD, webViewVarIndex);
                            mv.visitVarInsn(ALOAD, originalClientVar);
                            // 调用MonitorHelper.handleX5WebViewClient
                            mv.visitMethodInsn(
                                INVOKEVIRTUAL,
                                "com/lygttpod/monitor/MonitorHelper", // 类名
                                "handleX5WebViewClient", // 方法名
                                "(Lcom/tencent/smtt/sdk/WebView;Lcom/tencent/smtt/sdk/WebViewClient;)Lcom/tencent/smtt/sdk/WebViewClient;",// 方法描述符
                                false // 不是接口方法
                            );
                            // 调用原始的setWebViewClient方法，但使用MonitorHelper处理后的结果
                            mv.visitVarInsn(ALOAD, webViewVarIndex);// 加载WebView对象
                            mv.visitInsn(SWAP); // 交换栈顶两个元素，使WebView在栈顶，处理后的WebViewClient在次栈顶

                            // 调用原始的setWebViewClient方法，但使用ProxyX5Client作为参数
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        } else {
                            super.visitMethodInsn(opcode, owner, name, desc, itf)
                        }
                    }

                }
            }
        }
    }

    //检测哪些类 需要插桩
    override fun isInstrumentable(classData: ClassData): Boolean {
//        if (classData.className.startsWith("android/webkit/WebView") ||
//            classData.superClasses.any { it.startsWith("android.webkit.WebView") }) {
//            println("插件w1：" + classData.className)
//        }
        return true
    }
}