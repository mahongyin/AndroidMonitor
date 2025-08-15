# monitor
-keep class com.lygttpod.monitor.** { *; }

-keepclassmembers class * extends java.net.HttpURLConnection {
    <methods>;
}