package app.jsch.androidtunnel.services.injector;

import org.conscrypt.Conscrypt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Security;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import app.jsch.androidtunnel.AppPrefs;
import app.jsch.androidtunnel.*;
import app.jsch.androidtunnel.logs.AppLogManager;


public class SSLTunnel
{

	private Socket incoming;
	private int h = 0;

	static {
		try {
			Security.insertProviderAt(Conscrypt.newProvider(), 1);

		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
		}}

	public SSLTunnel(Socket in){
		incoming = in;
	}

	private void sendForwardSuccess(Socket socket) throws IOException
	{
		String respond = "HTTP/1.1 200 OK\r\n\r\n";
		socket.getOutputStream().write(respond.getBytes());
		socket.getOutputStream().flush();
	}


	public Socket inject() {
		try {
			String readLine;
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.incoming.getInputStream()));
			StringBuilder stringBuilder = new StringBuilder();
			while (true) {
				readLine = bufferedReader.readLine();
				if (readLine != null && readLine.length() > 0) {
					stringBuilder.append(readLine);
					stringBuilder.append("\r\n");
				}
				if (stringBuilder.toString().equals("")) {
					return null;
				}
				String servidor = AppPrefs.getProxyIP();
				int porta = AppPrefs.getProxyPort();
				String sni = AppPrefs.getSNI();
				AppLogManager.addToLog("SNI: " + sni);
				//Socket socket = SocketChannel.open().socket();

				Socket socket = new Socket();
		        //COMENTA SE DER ERROR
				sendForwardSuccess(incoming);

				socket.connect(new InetSocketAddress(servidor, porta));
				if(socket.isConnected()){
					socket = doSSLHandshake(servidor, sni ,porta);
				}
				return socket;

			}
		} catch (Exception e) {
			return null;
		}
	}


	private SSLSocket doSSLHandshake(String host, String sni, int port) throws IOException {
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers()
					{
						return null;
					}
					public void checkClientTrusted(
							java.security.cert.X509Certificate[] certs, String authType)
					{
					}
					public void checkServerTrusted(
							java.security.cert.X509Certificate[] certs, String authType)
					{
					}
				}
		};


		try {
			X509TrustManager tm = Conscrypt.getDefaultX509TrustManager();
			SSLContext sslContext = SSLContext.getInstance("TLS", "Conscrypt");
			sslContext.init(null, new TrustManager[] { tm }, null);

			TLSSocketFactory tsf = new TLSSocketFactory();
			SSLSocket socket = (SSLSocket) tsf.createSocket(host, port);

			try {
				socket.getClass().getMethod("setHostname", String.class).invoke(socket, sni);

			} catch (Throwable e) {
				// ignore any error, we just can't set the hostname...
			}


			String[] currentTlsVersion = (new String[] {""});
			try{
				if (AppPrefs.getTLSVersion().equals("auto")){
					//currentTlsVersion = (new String[] {"TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"});
					currentTlsVersion = socket.getSupportedProtocols();
				}else{
					currentTlsVersion = (new String[] {AppPrefs.getTLSVersion()});

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			socket.setEnabledProtocols(currentTlsVersion);

			//socket.setEnabledProtocols(socket.getSupportedProtocols());
			socket.setEnabledCipherSuites(socket.getEnabledCipherSuites());
			socket.addHandshakeCompletedListener(new SSLTunnel.mHandshakeCompletedListener(host, port, socket));
			AppLogManager.addToLog("Starting SSL...");
			socket.startHandshake();


			return socket;

		} catch (Exception e) {
			AppLogManager.addToLog("<strong><font color=#FF0000>VPN: </strong></font>" + e.toString());
			IOException iOException = new IOException(new StringBuffer().append("Could not complete SSL handshake: ").append(e).toString());
			throw iOException;
		}

	}

	class mHandshakeCompletedListener implements HandshakeCompletedListener {
        private final SSLSocket val$sslSocket;

		mHandshakeCompletedListener( String str, int i, SSLSocket sSLSocket) {
            this.val$sslSocket = sSLSocket;
		}

		public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
			AppLogManager.addToLog("<b><font color=#49C53C>SSL: Using cipher " + handshakeCompletedEvent.getSession().getCipherSuite()+"</font></b>");
			AppLogManager.addToLog(new StringBuffer().append("SSL: Supported protocols: <br>").append(Arrays.toString(val$sslSocket.getSupportedProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
			AppLogManager.addToLog(new StringBuffer().append("SSL: Enabled protocols: <br>").append(Arrays.toString(val$sslSocket.getEnabledProtocols())).toString().replace("[", "").replace("]", "").replace(",", "<br>"));
			AppLogManager.addToLog("SSL: Using protocol " + handshakeCompletedEvent.getSession().getProtocol());
			AppLogManager.addToLog("<b><font color=#49C53C>SSL: Handshake finished!</font></b>");
		}
	}



	private String d(String str) {
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

	private String a(String str, String str2, String str3) {
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



	public String ua() {
		String property = System.getProperty("http.agent");
		return property == null ? "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.130 Safari/537.36" : property;
	}



	private boolean a(String str, Socket socket, OutputStream outputStream) throws Exception{
		if (!str.contains("[split]")) {
			return true;
		}
		for (String str2 : str.split(Pattern.quote("[split]"))) {
			outputStream.write(str2.getBytes());
			outputStream.flush();
			//b(str2, socket);
		}
		return false;
	}




}
