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
abstract class WebClientClassVisitorFactory :
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
                        // 拦截webview.setWebViewClient调用
                        if (opcode == INVOKEVIRTUAL &&
                            "android/webkit/WebView" == owner &&
                            "setWebViewClient" == name &&
                            "(Landroid/webkit/WebViewClient;)V" == desc
                        ) {
                            println("插件w4：$className#$owner.$name$desc")
                            // 将原始WebViewClient存储在局部变量中
                            val originalClientVar =
                                newLocal(Type.getType("Landroid/webkit/WebViewClient;"))
                            mv.visitVarInsn(ASTORE, originalClientVar)

                            // 创建新的WebView存储在局部变量中
                            val webViewVarIndex =
                                newLocal(Type.getType("Landroid/webkit/WebView;"));
                            mv.visitVarInsn(ASTORE, webViewVarIndex);

                            // 启用JavaScript
//                            mv.visitVarInsn(ALOAD, webViewVarIndex);// 加载WebView实例
//                            mv.visitMethodInsn(INVOKEVIRTUAL,
//                                "android/webkit/WebView",
//                                "getSettings",
//                                "()Landroid/webkit/WebSettings;",
//                                false);
//                            mv.visitInsn(ICONST_1);
//                            mv.visitMethodInsn(INVOKEVIRTUAL,
//                                "android/webkit/WebSettings",
//                                "setJavaScriptEnabled",
//                                "(Z)V",
//                                false);

//                            // 创建代理WebViewClient
//                            mv.visitTypeInsn(NEW, "com/lygttpod/monitor/web/ProxyWebViewClient");
//                            mv.visitInsn(DUP);
//                            mv.visitVarInsn(
//                                ALOAD,
//                                originalClientVar
//                            );//加载原始的WebViewClient 放到ProxyWebViewClient()
//                            mv.visitMethodInsn(
//                                INVOKESPECIAL,
//                                "com/lygttpod/monitor/web/ProxyWebViewClient",
//                                "<init>",
//                                "(Landroid/webkit/WebViewClient;)V",
//                                false
//                            )

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
                            // 调用MonitorHelper.handleWebViewClient
                            mv.visitMethodInsn(
                                INVOKEVIRTUAL,/*这里没@JvmStatic不是静态的 不能使用INVOKESTATIC指令，INVOKEVIRTUAL或INVOKESPECIAL等，取决于方法类型*/
                                "com/lygttpod/monitor/MonitorHelper", // 类名
                                "handleWebViewClient", // 方法名
                                "(Landroid/webkit/WebView;Landroid/webkit/WebViewClient;)Landroid/webkit/WebViewClient;",// 方法描述符
                                false // 不是接口方法
                            );
                            // 调用原始的setWebViewClient方法，但使用MonitorHelper处理后的结果
                            mv.visitVarInsn(ALOAD, webViewVarIndex);// 加载WebView对象
                            mv.visitInsn(SWAP); // 交换栈顶两个元素，使WebView在栈顶，处理后的WebViewClient在次栈顶

                            // 调用原始的setWebViewClient方法，但使用ProxyWebViewClient作为参数
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
        if (classData.className == "android/webkit/WebView") {
            println("插件w1：" + classData.className)
        }
        return true//classData.className == ""
    }
}