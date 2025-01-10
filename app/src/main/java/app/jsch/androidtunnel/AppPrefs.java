package app.jsch.androidtunnel;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import app.jsch.androidtunnel.utils.ResolveAddrs;

public class AppPrefs {

    public static SharedPreferences sharedPreferences;

    public static void init(Context context) {
        sharedPreferences = context.getSharedPreferences("jsch_android_prefs", Context.MODE_PRIVATE);
    }

    public static void resetAppPrefs() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    public static String getUsername() {
        return sharedPreferences.getString("username", "");
    }

    public static void setUsername(String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.apply();
    }

    public static String getPassword() {
        return sharedPreferences.getString("password", "");
    }

    public static void setPassword(String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("password", password);
        editor.apply();
    }

    public static String getUDPResolver() {
        return sharedPreferences.getString("udpResolver", "127.0.0.1:7300");
    }

    public static void setUDPResolver(String udpResolver) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("udpResolver", udpResolver);
        editor.apply();
    }

    public static boolean isEnableCustomDNS() {
        return sharedPreferences.getBoolean("enableCustomDNS", true);
    }

    public static void setEnableCustomDNS(boolean enableCustomDNS) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("enableCustomDNS", enableCustomDNS);
        editor.apply();
    }

    public static String getDNS1() {
        return sharedPreferences.getString("dns1", "1.1.1.1");
    }

    public static void setDNS1(String dns1) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("dns1", dns1);
        editor.apply();
    }

    public static String getDNS2() {
        return sharedPreferences.getString("dns2", "1.0.0.1");
    }

    public static void setDNS2(String dns2) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("dns2", dns2);
        editor.apply();
    }

    public static String getConnectionMode() {
        return sharedPreferences.getString("connectionMode", "HTTP_PROXY");
    }

    public static void setConnectionMode(String connectionMode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("connectionMode", connectionMode);
        editor.apply();
    }

    public static boolean isEnableUDP() {
        return sharedPreferences.getBoolean("enableUDP", true);
    }

    public static void setEnableUDP(boolean enableUDP) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("enableUDP", enableUDP);
        editor.apply();
    }

    public static boolean isEnableWakeLock() {
        return sharedPreferences.getBoolean("enableWakeLock", true);
    }

    public static void setEnableWakeLock(boolean enableWakeLock) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("enableWakeLock", enableWakeLock);
        editor.apply();
    }

    public static String getProxyIP() {
        //return ResolveAddrs.resolveHostDomain(sharedPreferences.getString("proxyIP", ""));
        return sharedPreferences.getString("proxyIP", "");

    }

    public static void setProxyIP(String proxyIP) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("proxyIP", proxyIP);
        editor.apply();
    }

    public static String getNetworkName() {
        return sharedPreferences.getString("networkName", "SSH");
    }

    public static void setNetworkName(String networkName) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("networkName", networkName);
        editor.apply();
    }

    public static boolean isEnableNotification() {
        return sharedPreferences.getBoolean("enableNotification", true);
    }

    public static void setEnableNotification(boolean enableNotification) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("enableNotification", enableNotification);
        editor.apply();
    }

    public static int getProxyPort() {
        return sharedPreferences.getInt("proxyPort", 80);
    }

    public static void setProxyPort(int proxyPort) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("proxyPort", proxyPort);
        editor.apply();
    }

    public static String getPayloadKey() {
        return sharedPreferences.getString("payloadKey", "GET / HTTP/1.1[crlf]Host: [crlf]Upgrade: websocket[crlf][crlf]");
    }

    public static void setPayloadKey(String payloadKey) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("payloadKey", payloadKey);
        editor.apply();
    }

    public static boolean isEnableSSHCompress() {
        return sharedPreferences.getBoolean("enableSSHCompress", true);
    }

    public static void setEnableSSHCompress(boolean enableSSHCompress) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("enableSSHCompress", enableSSHCompress);
        editor.apply();
    }

    public static String getSNI() {
        return sharedPreferences.getString("sni", "web.whatsapp.com");
    }

    public static void setSNI(String sni) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("sni", sni);
        editor.apply();
    }

    public static String getTLSVersion() {
        return sharedPreferences.getString("tlsVersion", "auto");
    }

    public static void setTLSVersion(String tlsVersion) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("tlsVersion", tlsVersion);
        editor.apply();
    }

    public static int getDynamicPort() {
        return sharedPreferences.getInt("dynamicPort", 1080);
    }

    public static void setDynamicPort(int dynamicPort) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("dynamicPort", dynamicPort);
        editor.apply();
    }

    public static Set<String> getStringSet(String selectedApps, HashSet<Object> objects) {
        return java.util.Collections.emptySet();
    }

    public static Context getAppContext() {
        return MainActivity.mContext;
    }

    public static String getSSHCompressLevel() {
        return "9";
    }
}