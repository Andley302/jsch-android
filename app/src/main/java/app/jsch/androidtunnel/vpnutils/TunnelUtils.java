package app.jsch.androidtunnel.vpnutils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.VpnService;
import android.os.Build;
import android.util.ArrayMap;


import app.jsch.androidtunnel.AppPrefs;
import app.jsch.androidtunnel.logs.AppLogManager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TunnelUtils
{
	public static Map<String, CharSequence> BBCODES_LIST;

	public static String formatCustomPayload(String hostname, int port, String payload) {
		BBCODES_LIST = new ArrayMap<>();

		BBCODES_LIST.put("[method]", "CONNECT");
		BBCODES_LIST.put("[host]", hostname);
		BBCODES_LIST.put("[ip]", hostname);
		BBCODES_LIST.put("[protocol]", "HTTP/1.0");
		BBCODES_LIST.put("[port]", Integer.toString(port));

		BBCODES_LIST.put("[host_port]", String.format("%s:%d", hostname, port));
		BBCODES_LIST.put("[ssh]", String.format("%s:%d", hostname, port));
		BBCODES_LIST.put("[vpn]", String.format("%s:%d", hostname, port));


		BBCODES_LIST.put("[crlf]", "\r\n");
		BBCODES_LIST.put("[cr]", "\r");
		BBCODES_LIST.put("[lf]", "\n");
		BBCODES_LIST.put("[lfcr]", "\n\r");
		BBCODES_LIST.put("\\n", "\n");
		BBCODES_LIST.put("\\r", "\r");

		String ua = System.getProperty("http.agent");
		BBCODES_LIST.put("[ua]", ua == null ? "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36" : ua);

		if (!payload.isEmpty()) {
			for (String key : BBCODES_LIST.keySet()) {
				key = key.toLowerCase();
				payload = payload.replace(key, BBCODES_LIST.get(key));
			}
			payload = parseRandom(parseRotate(payload));

		}
		return d(payload);
	}

	public static boolean isNetworkOnline(Context context) {
		ConnectivityManager manager = (ConnectivityManager) context
				.getSystemService(context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();

		return (networkInfo != null && networkInfo.isConnectedOrConnecting());
	}



	public static String getLocalIpAddress() {
		String ipAddress = "";
		ConnectivityManager connectivityManager = (ConnectivityManager) AppPrefs.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);

		try{
			ipAddress= getDefaultIpAddresses(connectivityManager);
		} catch (Exception e) {
		}

		if (!Objects.equals(getTUNInterfaceIpAddress(), "")){
			ipAddress = getTUNInterfaceIpAddress() + " (VPN)";

		}

		return ipAddress;
	}


	private static String getDefaultIpAddresses(ConnectivityManager connectivityManager) {
		String str = "";
		try{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				str = formatIpAddresses(connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork()));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return str;
	}

	private static String formatIpAddresses(LinkProperties prop) {
		try{
			if (prop == null) return null;
			Iterator<LinkAddress> iter = prop.getLinkAddresses().iterator();
			// If there are no entries, return null
			if (!iter.hasNext()) return null;
			// Concatenate all available addresses, newline separated
			StringBuilder addresses = new StringBuilder();
			while (iter.hasNext()) {
				addresses.append(iter.next().getAddress().getHostAddress());
				if (iter.hasNext()) addresses.append("\n");
			}
			return addresses.toString();
		} catch (Exception e) {
			return "";
		}
	}


	public static String getTUNInterfaceIpAddress() {
		String tunInterfaceIpAddress = "";

		try {
			List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface intf : interfaces) {
				if (intf.getName().startsWith("tun")) { // Verifica se é a interface TUN desejada
					List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
					for (InetAddress addr : addrs) {
						if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
							tunInterfaceIpAddress = addr.getHostAddress();
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return tunInterfaceIpAddress;
	}

	private static String d(String str) {
		String str2 = str;
		String str3 = str2;
		if (str2.contains("[cr*")) {
			str3 = a(str2, "[cr*", "\r");
		}
		String str4 = str3;
		if (str3.contains("[lf*")) {
			str4 = a(str3, "[lf*", "\n");
		}
		str2 = str4;
		if (str4.contains("[crlf*")) {
			str2 = a(str4, "[crlf*", "\r\n");
		}
		String str5 = str2;
		if (str2.contains("[lfcr*")) {
			str5 = a(str2, "[lfcr*", "\n\r");
		}
		return str5;
	}

	private static String a(String str, String str2, String str3) {
		while (str.contains(str2)) {
			Matcher matcher = Pattern.compile("\\[.*?\\*(.*?[0-9])\\]").matcher(str);
			if (matcher.find()) {
				int intValue = Integer.valueOf(matcher.group(1)).intValue();
				String str7 = "";
				for (int i = 0; i < intValue; i++) {
					str7 = new StringBuffer().append(str7).append(str3).toString();
				}
				String str8 = str;
				str = str8.replace(new StringBuffer().append(str2).append(String.valueOf(intValue)).append("]").toString(), str7);
			}
		}
		return str;
	}



	public static boolean injectSplitPayload(String requestPayload, OutputStream out) throws IOException {

		if (requestPayload.contains("[delay_split]")) {
			String[] split = requestPayload.split(Pattern.quote("[delay_split]"));

			for (int n = 0; n < split.length; n++) {
				String str = split[n];

				if (!injectSimpleSplit(str, out)) {
					try {
						out.write(str.getBytes("ISO-8859-1"));
					} catch (UnsupportedEncodingException e2) {
						out.write(str.getBytes());
					}
					out.flush();
				}

				try {
					if (n != (split.length-1))
						Thread.sleep(1000);
				} catch(InterruptedException e) {}
			}

			return true;
		}
		else if (injectSimpleSplit(requestPayload, out)) {
			return true;
		}

		return false;
	}


	private static boolean injectSimpleSplit(String requestPayload, OutputStream out) throws IOException {

		String[] split3;
		int i2;

		if (requestPayload.contains("[split]")) {
			split3 = requestPayload.split(Pattern.quote("[split]"));
			for (i2 = 0; i2 < split3.length; i2++)
			{
				try {
					out.write(split3[i2].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split3[i2].getBytes());
				}

				out.flush();

			}
		} else if (requestPayload.contains("[splitNoDelay]")) {
			split3 = requestPayload.split(Pattern.quote("[splitNoDelay]"));
			for (i2 = 0; i2 < split3.length; i2++) {
				try {
					out.write(split3[i2].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split3[i2].getBytes());
				}
				out.flush();
			}
		} else if (requestPayload.contains("[instant_split]")) {
			split3 = requestPayload.split(Pattern.quote("[instant_split]"));
			for (i2 = 0; i2 < split3.length; i2++) {
				try {
					out.write(split3[i2].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split3[i2].getBytes());
				}

				out.flush();
			}
		} else if (requestPayload.contains("[delay]")) {
			split3 = requestPayload.split(Pattern.quote("[delay]"));
			for (i2 = 0; i2 < split3.length; i2++) {
				try {
					out.write(split3[i2].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split3[i2].getBytes());
				}

				out.flush();
				try {
					if (i2 != (split3.length-1))
						Thread.sleep(1000);
				} catch(InterruptedException e) {}
			}

		} else if (requestPayload.contains("[split_delay]")) {
			split3 = requestPayload.split(Pattern.quote("[split_delay]"));
			for (i2 = 0; i2 < split3.length; i2++) {
				try {
					out.write(split3[i2].getBytes("ISO-8859-1"));
				} catch (UnsupportedEncodingException e2) {
					out.write(split3[i2].getBytes());
				}

				out.flush();
				try {
					if (i2 != (split3.length-1))
						Thread.sleep(1000);
				} catch(InterruptedException e) {}
			}

			return true;
		}

		return false;
	}






	private static Map<Integer,Integer> lastRotateList = new ArrayMap<>();
	private static String lastPayload = "";

	public static String parseRotate(String payload) {
		Matcher match = Pattern.compile("\\[rotate=(.*?)\\]")
				.matcher(payload);

		if (!lastPayload.equals(payload)) {
			restartRotateAndRandom();
			lastPayload = payload;
		}



		int i = 0;
		while (match.find()) {
			String group = match.group(1);

			String[] split = group.split(";");
			if (split.length <= 0) continue;

			int split_key;
			if (lastRotateList.containsKey(i)) {
				split_key = lastRotateList.get(i)+1;
				if (split_key >= split.length) {
					split_key = 0;
				}
			}
			else  {
				split_key = 0;
			}

			String host = split[split_key];

			payload = payload.replace(match.group(0), host);

			lastRotateList.put(i, split_key);

			i++;
		}

		return payload;
	}


	public static String parseRandom(String payload) {
		Matcher match = Pattern.compile("\\[random=(.*?)\\]")
				.matcher(payload);


		int i = 0;
		while (match.find()) {
			String group = match.group(1);

			String[] split = group.split(";");
			if (split.length <= 0) continue;

			Random r = new Random();
			int split_key = r.nextInt(split.length);

			if (split_key >= split.length || split_key < 0) {
				split_key = 0;
			}

			String host = split[split_key];

			payload = payload.replace(match.group(0), host);

			i++;
		}

		return payload;
	}

	public static void restartRotateAndRandom() {
		lastRotateList.clear();

	}
	public static boolean isActiveVpn(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
			Network network = cm.getActiveNetwork();
			NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);

			return (capabilities!= null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN));
		}
		else {
			NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_VPN);

			return (info != null && info.isConnectedOrConnecting());
		}
	}

	private static Socket output = null;

	public static void setSocket(Socket socks) {
		output = socks;
	}

	public static boolean protect(VpnService vpnService)
	{
		if (output == null)
		{
			addLog("Vpn Protect Socket is null");
			return false;
		}
		else if (output.isClosed())
		{
			addLog("Vpn Protect Socket is closed");
			return false;
		}
		else if (!output.isConnected())
		{
			addLog("Vpn Protect Socket not connected");
			return false;
		}
		else if (vpnService.protect(output))
		{
			addLog("Vpn Protect Socket has protected");
			return true;
		}
		else
		{
			addLog("Vpn Protect Failed to protecting socket, reboot this device required");
			return false;
		}
	}

	private static void addLog(String msg){
		AppLogManager.addToLog(msg);
	}
}

