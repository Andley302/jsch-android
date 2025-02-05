# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
#-keep class app.jsch.androidtunnel.TestClass
#-keep class app.jsch.androidtunnel.** { *;}
-dontwarn com.android.org.conscrypt.SSLParametersImpl
-dontwarn org.apache.harmony.xnet.provider.jsse.SSLParametersImpl
-dontwarn sun.security.x509.X509Key
-keep class org.** { *; }
-keep class com.jsch.** { *; }
-keep class com.jcraft.** { *; }
-keep class net.** { *;}
-keep class com.android.org.conscrypt.** { *;}
-keep class org.apache.harmony.xnet.provider.** { *;}
-keep class sun.security.x509.** { *;}