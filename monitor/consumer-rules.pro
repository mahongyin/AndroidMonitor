# monitor
-keep class com.lygttpod.monitor.** { *; }

-keepclassmembers class * extends java.net.HttpURLConnection {
    <methods>;
}

# 保留数据收集方法
-keep class com.lygttpod.monitor.streamhandler.OkHttpConnection {
  public static *;
}

# 保留 ASM 注入生成的方法
-keepclassmembers class * {
    *** access$*(...);
}

# 保留 UUID 方法
-keepclassmembers class java.util.UUID {
    public static randomUUID();
    public java.lang.String toString();
}