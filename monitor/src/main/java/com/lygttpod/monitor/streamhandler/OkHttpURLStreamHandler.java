package com.lygttpod.monitor.streamhandler;


import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Created By Mahongyin
 * Date    2025/9/4 10:55
 */
public class OkHttpURLStreamHandler extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        //Log.d(TAG, "Intercepted URL: $u");
        return new OkHttpConnection(u);
    }

    @Override
    protected URLConnection openConnection(URL u, Proxy p) throws IOException {
        return new OkHttpConnection(u, p);
    }
}
