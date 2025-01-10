package app.jsch.androidtunnel.services.connectionservices;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.net.InetAddress;
import java.net.UnknownHostException;


import app.jsch.androidtunnel.AppPrefs;
import app.jsch.androidtunnel.logs.AppLogManager;

public class ResolveAddrs {

  public static boolean checkIsIPV6(String host){
    if (host.contains(":")){
      return true;
    }else{
      return false;
    }
  }

  public static String resolveHostDomain(String hostIP) {
    try {
      InetAddress a = InetAddress.getByName(hostIP);
      hostIP = a.getHostAddress();
      return hostIP;
    } catch (Exception e) {
      // hostIP = hostIP;
      //AppLogManager.addToLog("Unresolved host, continuing connection...");
      return hostIP;
    }
  }


  public static String getOperatorName() {
    try {
      final ConnectivityManager connMgr = (ConnectivityManager) AppPrefs.getAppContext().getSystemService(
        Context.CONNECTIVITY_SERVICE
      );
      final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(
        ConnectivityManager.TYPE_WIFI
      );
      TelephonyManager tm = (TelephonyManager) AppPrefs.getAppContext().getSystemService(
        Context.TELEPHONY_SERVICE
      );
      if (wifi.isConnectedOrConnecting()) {
        return "Wi-Fi";
      } else {
        if (Build.VERSION.SDK_INT >= 24) {
          tm =
            tm.createForSubscriptionId(
              SubscriptionManager.getDefaultDataSubscriptionId()
            );
          return String.valueOf(tm.getNetworkOperatorName());
        } else {
          return String.valueOf(tm.getNetworkOperatorName());
        }
      }
    } catch (Exception e) {
      return "Default";
    }
  }

  public static InetAddress intToInetAddress(int hostAddress) {
    byte[] addressBytes = {
      (byte) (0xff & hostAddress),
      (byte) (0xff & (hostAddress >> 8)),
      (byte) (0xff & (hostAddress >> 16)),
      (byte) (0xff & (hostAddress >> 24)),
    };

    try {
      return InetAddress.getByAddress(addressBytes);
    } catch (UnknownHostException e) {
      throw new AssertionError();
    }
  }

  public String getLocalDNS() {
    try {
      //OBTEM DNS LOCAL
      final ConnectivityManager connectivityManager = (ConnectivityManager) AppPrefs.getAppContext().getSystemService(
        Context.CONNECTIVITY_SERVICE
      );
      for (Network network : connectivityManager.getAllNetworks()) {
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
        if (networkInfo.isConnected()) {
          LinkProperties linkProperties = connectivityManager.getLinkProperties(
            network
          );
          linkProperties.getDnsServers();

          //OBTEM ARRAY DE TODOS DNS LOCAL
          String current_dns_string = String.valueOf(
            linkProperties.getDnsServers().get(0)
          );

          //SPLITA DNS
          String final_dns = current_dns_string.replace("/", "");

          return final_dns;
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }


  public static String getAPN() {
    try {
      //OBTEM DNS LOCAL
      final ConnectivityManager connMgr = (ConnectivityManager) AppPrefs.getAppContext().getSystemService(
        Context.CONNECTIVITY_SERVICE
      );
      for (Network network : connMgr.getAllNetworks()) {
        NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
          return "Wi-Fi";
        }
        if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
          try {
            String apn = String.valueOf(networkInfo.getExtraInfo());
            if (apn != null && !apn.contains("ims")) {
              return apn;
            }
          } catch (Exception e) {
            return "Unknown";
          }
        }
      }
    } catch (Exception e) {
      return "Unknown";
    }
    return "Unknown";
  }
}
