package com.android.monitor.demo;

import android.util.Log;

import com.lygttpod.monitor.MonitorHelper;
import com.lygttpod.monitor.utils.OkhttpUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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
//        OkHttpClient client;
//        OkHttpClient.Builder builder = new OkHttpClient.Builder();
//        List<Interceptor> list = MonitorHelper.INSTANCE.getHookInterceptors();
//        builder.interceptors().addAll(list);
//        client = builder.build();

    }
   public interface HttpCallback {
        void onSuccess(String result);

        void onFailure(String errorMsg);
    }
    public static void getRequest(String baseUrl, HashMap<String, String> paramsMap, HttpCallback  callback) {
        InputStream inputStream = null;
        HttpURLConnection connection = null;
        ByteArrayOutputStream baos = null;
        StringBuilder tempParams = new StringBuilder();
        try {
            if (paramsMap != null) {
                int pos = 0;
                for (String key : paramsMap.keySet()) {
                    if (pos > 0) {
                        tempParams.append("&");
                    }
                    String result = String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8"));
                    tempParams.append(result);
                    pos++;
                }
            }
            String requestUrl = baseUrl + tempParams.toString();
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            // 设定请求的方法为"POST"，默认是GET
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(50000);
            connection.setReadTimeout(50000);
            // User-Agent  IE9的标识
            connection.setRequestProperty("User-Agent", OkhttpUtils.webUserAgent);
            connection.setRequestProperty("Accept-Language", "zh-CN");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Charset", "UTF-8");
            /*
             * 当我们要获取我们请求的http地址访问的数据时就是使用connection.getInputStream().read()方式时我们就需要setDoInput(true)，
             * 根据api文档我们可知doInput默认就是为true。我们可以不用手动设置了，如果不需要读取输入流的话那就setDoInput(false)。
             * 当我们要采用非get请求给一个http网络地址传参 就是使用connection.getOutputStream().write() 方法时我们就需要setDoOutput(true), 默认是false
             */
            // 设置是否从httpUrlConnection读入，默认情况下是true;
            connection.setDoInput(true);
            // 设置是否向httpUrlConnection输出，如果是post请求，参数要放在http正文内，因此需要设为true, 默认是false;
            //connection.setDoOutput(true);//Android  4.0 GET时候 用这句会变成POST  报错java.io.FileNotFoundException
            connection.setUseCaches(false);
            connection.connect();//
            int contentLength = connection.getContentLength();
            if (connection.getResponseCode() == 200) {
                inputStream = connection.getInputStream();//会隐式调用connect()
                baos = new ByteArrayOutputStream();
                int readLen;
                byte[] bytes = new byte[1024];
                while ((readLen = inputStream.read(bytes)) != -1) {
                    baos.write(bytes, 0, readLen);
                }
                String result = baos.toString();
                Log.i("HTTPTEST", " result:" + result);
                callback.onSuccess(result);
            } else {
                String message = "请求失败 code:" + connection.getResponseCode();
                Log.d("HTTPTEST", message);
                callback.onFailure(message);
            }
        } catch (Exception e) {
            String message = e.getMessage();
            Log.d("HTTPTEST", message);
            callback.onFailure(message);
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static void postRequest(String requestUrl, HashMap<String, String> paramsMap, HttpCallback callback) {
        InputStream inputStream = null;
        HttpURLConnection connection = null;
        ByteArrayOutputStream baos = null;
        StringBuilder tempParams = new StringBuilder();
        try {
            if (paramsMap != null) {
                int pos = 0;
                for (String key : paramsMap.keySet()) {
                    if (pos > 0) {
                        tempParams.append("&");
                    }
                    String result = String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8"));
                    tempParams.append(result);
                    pos++;
                }
            }
            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            // 设定请求的方法为"POST"，默认是GET
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(50000);
            connection.setReadTimeout(50000);
            // User-Agent  IE9的标识
            connection.setRequestProperty("User-Agent", OkhttpUtils.webUserAgent);
            connection.setRequestProperty("Accept-Language", "zh-CN");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Charset", "UTF-8");
            /*
             * 当我们要获取我们请求的http地址访问的数据时就是使用connection.getInputStream().read()方式时我们就需要setDoInput(true)，
             * 根据api文档我们可知doInput默认就是为true。我们可以不用手动设置了，如果不需要读取输入流的话那就setDoInput(false)。
             * 当我们要采用非get请求给一个http网络地址传参 就是使用connection.getOutputStream().write() 方法时我们就需要setDoOutput(true), 默认是false
             */
            // 设置是否从httpUrlConnection读入，默认情况下是true;
            connection.setDoInput(true);
            // 设置是否向httpUrlConnection输出，如果是post请求，参数要放在http正文内，因此需要设为true, 默认是false;
            connection.setDoOutput(true);
            connection.setUseCaches(false);

            // set  params three way  OutputStreamWriter
            OutputStreamWriter out = new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8);
            // 发送请求params参数"username=admin&password=123456"
            out.write(tempParams.toString());
            out.flush();
            // 通过输出流将数据写入到请求体中
//            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
//            // 提交数据的格式为 key=value&key2=value2
//            output.writeBytes(tempParams.toString());
//            output.flush();
            connection.connect();

            int contentLength = connection.getContentLength();
            if (connection.getResponseCode() == 200) {
                // 会隐式调用connect()
                inputStream = connection.getInputStream();
                baos = new ByteArrayOutputStream();
                int readLen;
                byte[] bytes = new byte[1024];
                while ((readLen = inputStream.read(bytes)) != -1) {
                    baos.write(bytes, 0, readLen);
                }
                String backStr = baos.toString();
                Log.d("HTTPTEST", "postRequest: " + backStr);
                callback.onSuccess(backStr);
            } else {
                String message = "请求失败 code:" + connection.getResponseCode();
                Log.d("HTTPTEST", message);
                callback.onFailure(message);
            }
            out.close();
        } catch (Exception e) {
            String message = e.getMessage();
            Log.d("HTTPTEST", message);
            callback.onFailure(message);
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
