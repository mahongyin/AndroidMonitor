package com.android.monitor.demo;

import com.lygttpod.monitor.MonitorHelper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

/**
 * Created By Mahongyin  看字节码用
 * Date    2025/8/14 20:39
 */
public class Test {
    public static void main(String[] args) {
        System.out.println("Hello World!");
        OkHttpClient client;
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        List<Interceptor> list = MonitorHelper.INSTANCE.getHookInterceptors();
        builder.interceptors().addAll(list);
        client = builder.build();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://www.baidu.com").openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept","*/*");
//            connection.setRequestProperty("Content-Type","application/xml");
            connection.connect();
        } catch (Exception e) {

        }

    }
}
