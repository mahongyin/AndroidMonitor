package com.lygttpod.monitor.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.lygttpod.monitor.MonitorHelper
import com.lygttpod.monitor.data.PropertiesData
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.Properties


class MonitorProperties {
    companion object {
        private const val TAG = "MonitorHelper"
        private const val KEY_MONITOR_PORT = "monitor.port"
        private const val KEY_MONITOR_DB_NAME = "monitor.dbName"
        private const val KEY_WHITE_CONTENT_TYPES = "monitor.whiteContentTypes"
        private const val KEY_WHITE_HOSTS = "monitor.whiteHosts"
        private const val KEY_BLACK_HOSTS = "monitor.blackHosts"
        private const val KEY_IS_FILTER_IPADDRESS_HOST = "monitor.isFilterIPAddressHost"
        private const val ASSETS_FILE_NAME = "monitor.properties"
    }

    /**
     * 用进程pid当端口号。进程id 32位整数（4 字节） 通常范围通常从 1-32768（系统相关）由操作系统分配和管理
     * 端口范围 2字节 系统端口(0-1023)、注册端口(1024-49151)和动态端口(49152-65535)
     */
    private fun getMyPid(): String {
        val myPid = android.os.Process.myPid().toString()
        if (myPid.isEmpty()) {
            // 需要权限: <uses-permission android:name="android.permission.GET_TASKS" />
            val context = MonitorHelper.context
            val activityManager =
                context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val processes = activityManager?.runningAppProcesses
            if (processes != null) {
                for (processInfo in processes) {
                    val processName = processInfo.processName
                    val pid = processInfo.pid
                    if (context.packageName == processName) {
                        Log.d(TAG, "Process Name: $processName, PID: $pid")
                        return "${pid}"
                    }
                }
            }
        }
        Log.d(TAG, "Process PID: $myPid")
        return myPid
    }

    fun paramsProperties(): PropertiesData? {
        var propertiesData: PropertiesData? = null
        var inputStream: InputStream? = null
        val p = Properties()

        try {
            val context = MonitorHelper.context
            if (context == null) {
                Log.d(TAG, "初始化获取context失败")
                return propertiesData
            }
            inputStream = context.assets.open(ASSETS_FILE_NAME)
            if (inputStream != null) {
                p.load(inputStream)
                val port = getMyPid()//p.getProperty(KEY_MONITOR_PORT)
                val dbName = p.getProperty(KEY_MONITOR_DB_NAME)
                val whiteContentTypes = p.getProperty(KEY_WHITE_CONTENT_TYPES)
                //获取配置的host白名单
                val whiteHosts = p.getProperty(KEY_WHITE_HOSTS)
                //获取配置的host黑名单
                val blackHosts = p.getProperty(KEY_BLACK_HOSTS)
                val isFilterIPAddressHost =
                    p.getProperty(KEY_IS_FILTER_IPADDRESS_HOST)?.toBoolean() ?: false

                propertiesData =
                    PropertiesData(
                        port,
                        dbName,
                        whiteContentTypes,
                        whiteHosts,
                        blackHosts,
                        isFilterIPAddressHost
                    )
            }
        } catch (e: IOException) {
            if (e is FileNotFoundException) {
                Log.d(TAG, "not found monitor.properties")
            } else {
                e.printStackTrace()
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return propertiesData
    }
}