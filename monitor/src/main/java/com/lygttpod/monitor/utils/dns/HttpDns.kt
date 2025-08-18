package com.lygttpod.monitor.utils.dns

import android.text.TextUtils
import okhttp3.Dns
import java.net.InetAddress


class HttpDns : Dns {
    /**
     * 通过host找ip
     */
    override fun lookup(hostname: String): List<InetAddress> {
//        val ip = HttpDnsHelper.getIpByHost(hostname)
//        if (!TextUtils.isEmpty(ip)) {
//            //返回自己解析的地址列表
//            return InetAddress.getAllByName(ip).toList()
//        } else {
            // 解析失败，使用系统解析
            return Dns.SYSTEM.lookup(hostname)
//        }
    }


//    override fun lookup(hostname: String): List<InetAddress> {
//        val inetAddresses = mutableListOf<InetAddress>()
//        HttpDns.getService(accountId)
//            .getHttpDnsResultForHostSync(hostname, RequestIpType.auto)?.apply {
//                if (!ipv6s.isNullOrEmpty()) {
//                    for (i in ipv6s.indices) {
//                        inetAddresses.addAll(
//                            InetAddress.getAllByName(ipv6s[i]).toList()
//                        )
//                    }
//                } else if (!ips.isNullOrEmpty()) {
//                    for (i in ips.indices) {
//                        inetAddresses.addAll(
//                            InetAddress.getAllByName(ips[i]).toList()
//                        )
//                    }
//                }
//            }
//
//        if (inetAddresses.isEmpty()) {
//            inetAddresses.addAll(Dns.SYSTEM.lookup(hostname))
//        }
//        return inetAddresses
//    }

}