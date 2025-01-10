package app.jsch.androidtunnel.services.connectionservices.ssh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.text.TextUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.jsch.androidtunnel.MainActivity;
import app.jsch.androidtunnel.R;
import app.jsch.androidtunnel.services.connectionservices.ResolveAddrs;
import app.jsch.androidtunnel.vpnutils.CIDRIP;
import app.jsch.androidtunnel.vpnutils.IPUtil;
import app.jsch.androidtunnel.vpnutils.NetworkSpace;
import app.jsch.androidtunnel.vpnutils.Pdnsd;
import app.jsch.androidtunnel.vpnutils.Tun2Socks;
import app.jsch.androidtunnel.vpnutils.VpnUtils;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import app.jsch.androidtunnel.AppPrefs;
import app.jsch.androidtunnel.logs.AppLogManager;

public class EstablishVPN extends VpnService {

  private ConnectivityManager connectivityManager;
  public static boolean isRunning = false;
  private int mMtu = 1500;
  private VpnUtils.PrivateAddress mPrivateAddress;
  public ParcelFileDescriptor tunFd;
  private Tun2Socks mTun2Socks;
  private Pdnsd mPdnsd;
  private NetworkSpace mRoutes;
  private NetworkSpace mRoutesv6;
  private boolean isBypass = false;
  private Context mContext;

  private Thread tun2socksThread = null;
  private String currentIPAddr;

  static {
    System.loadLibrary("tun2socks");
  }

  private boolean isStopping = false;

  private BroadcastReceiver stopBr = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if ("stop_kill".equals(intent.getAction()) && !isStopping) {
        isStopping = true;
        AppLogManager.addToLog("<strong><font color=#ff8c00>Stopping VPN...</strong>");
        stopAllProcess();
        stopSelf();
      }
    }
  };


  private void setTunnelPrefs() {
    mRoutes = new NetworkSpace();
    mRoutesv6 = new NetworkSpace();

    try {
      mPrivateAddress = VpnUtils.selectPrivateAddress();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();

    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
      .permitAll()
      .build();

    StrictMode.setThreadPolicy(policy);

    mContext = MainActivity.mContext;

    LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
    lbm.registerReceiver(stopBr, new IntentFilter("stop_kill"));
  }

  /*
   * (non-Javadoc)
   *
   * @see android.app.Service#onStart(android.content.Intent, int)
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    //get is bypass
    isBypass = intent.getBooleanExtra("ENABLE_BYPASS", false);

    setDNS();

    currentIPAddr = intent.getStringExtra("SERVER_IP");
    currentIPAddr = ResolveAddrs.resolveHostDomain(currentIPAddr);

    //connecting manager
    connectivityManager =
      (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
    //set vpn prefs
    setTunnelPrefs();
    //start tunnel
   establishVpn();

    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    //VPN OK
    if (!isBypass){
       AppLogManager.addToLog("<b><font color=red>" + "VPN Tunnel Finished!" +"</font></b>");
    }
  }

  private void stopAllProcess() {
    // Usar ExecutorService para não bloquear a thread principal
    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(() -> {
      try {
        // Fechar o tun2socks
        if (mTun2Socks != null && mTun2Socks.isAlive()) {
          mTun2Socks.interrupt();
          mTun2Socks = null;
          AppLogManager.addToLog("Stopped tun2socks");
        }
      } catch (Exception e) {
        AppLogManager.addToLog("Stopping tun2socks error: " + e);
      }

      try {
        // Fechar o pdnsd
        if (mPdnsd != null && mPdnsd.isAlive()) {
          mPdnsd.interrupt();
          mPdnsd = null;
          AppLogManager.addToLog("Stopped pdnsd");
        }
      } catch (Exception e) {
        AppLogManager.addToLog("Stopping pdnsd error: " + e);
      }

      try {
        // Fechar o tun2socks thread
        if (tun2socksThread != null) {
          tun2socksThread.join();
          tun2socksThread = null;
          AppLogManager.addToLog("Stopped tun2socks thread");
        }
      } catch (Exception e) {
        AppLogManager.addToLog("Stopping tun2socks thread error: " + e);
        e.printStackTrace();
      }

      try {
        // Fechar o arquivo tunFd
        if (tunFd != null) {
          tunFd.close();
          tunFd = null;
          AppLogManager.addToLog("Stopped tunFd");
        }
      } catch (Exception e) {
        AppLogManager.addToLog("Stopping tunFd error: " + e);
      }


      AppLogManager.addToLog("<b><font color=red>Disconnected!</font></b>");

    });

    executor.shutdown();
  }



  public static String[] m_dnsResolvers = new String[] { "8.8.8.8", "8.8.4.4" };

  private void setDNS() {
    if (AppPrefs.isEnableCustomDNS()) {
      m_dnsResolvers = new String[] { AppPrefs.getDNS1(), AppPrefs.getDNS2() };
    } else {
      List<String> lista = VpnUtils.getNetworkDnsServer(this);
      m_dnsResolvers = new String[] { lista.get(0) };
    }
  }


  public synchronized boolean establishVpn() {
    //addLog("Starting Injector VPN Service");
    try {
      Locale.setDefault(Locale.ENGLISH);

      VpnService.Builder builder = new VpnService.Builder();
      builder.addAddress(mPrivateAddress.mIpAddress, mPrivateAddress.mPrefixLength);
      String release = Build.VERSION.RELEASE;
      if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
              && !release.startsWith("4.4.4") && !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
              && mMtu < 1280) {
        mMtu = 1280;
      }

      builder.setMtu(mMtu);
      mRoutes.addIP(new CIDRIP("0.0.0.0", 0), true);
      mRoutes.addIP(new CIDRIP("10.0.0.0", 8), false);

      mRoutes.addIP(new CIDRIP(mPrivateAddress.mSubnet, mPrivateAddress.mPrefixLength), true);

      /**
       * @param  GetIPV6Mask(hostString)
       * @see  hostString
       *author: staffnetDev github
       *The provided code snippet checks if the hostString contains a colon (:), which indicates that it is an IPv6 address. If it is an IPv6 address, it initializes Inet6Address and Inet4Address variables to null. It then iterates over all the addresses returned by InetAddress.getAllByName(hostString) and assigns the appropriate address to either ipv6 or ipv4.
       * If an IPv4 address is found (ipv4 is not null), it adds this address to the mRoutes with a subnet mask of 32.
       * If an IPv6 address is found (ipv6 is not null), it calculates the mask using the GetIPV6Mask method if the hostString contains a subnet mask, otherwise, it defaults to 128. It then adds this IPv6 address to mRoutesv6.
       * If the hostString does not contain a colon, it is
       * */
      if (!isBypass) {
        String hostString = this.currentIPAddr;


        if(hostString.contains(":")) {

          Inet6Address ipv6 = null;
          Inet4Address ipv4 = null;

          for(InetAddress addr : InetAddress.getAllByName(hostString)) {
            if(addr instanceof Inet6Address)
              ipv6 = (Inet6Address)addr;
            if(addr instanceof Inet4Address)
              ipv4 = (Inet4Address)addr;
          }

          if (ipv4 != null) {
            mRoutes.addIP(new CIDRIP(ipv4.getHostAddress(), 32), false);
          }


          if (ipv6 !=null){
            int mask = (hostString.contains("/")) ? GetIPV6Mask(hostString)  : 128;
            mRoutesv6.addIPv6(ipv6, mask, true);
          }

        }else

          mRoutes.addIP(new CIDRIP(hostString, 32), false);


      } else {
        mRoutes.addIP(new CIDRIP("192.198.0.1", 32), false);
      }

      //Commented because no data is generated when connecting to the VPN


      boolean allowUnsetAF = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

      if (allowUnsetAF) {
        /**
         * Disable this code to allow all traffic to pass through the VPN
         * @param allowAllAFFamilies(builder);
         * */
        if (!isBypass) {
          setAllowedVpnPackages(builder);
        }
      }

      // Add Dns
      String[] dnsResolver = m_dnsResolvers;

      for (String dns : dnsResolver) {
        try {
          // String dns2 = "208.67.222.123";
          if (dns.contains(":")) {
            builder.addDnsServer("1.1.1.1");
            //mRoutes.addIP(new CIDRIP("1.1.1.1", 32), true);
          } else {
            builder.addDnsServer(dns);
            //mRoutes.addIP(new CIDRIP(dns, 32), true);
          }
        } catch (IllegalArgumentException iae) {
          if (!isBypass) {
            AppLogManager.addLog(
                    Integer.parseInt(String.format(
                            "DNS error: <br> %s, %s",
                            dns,
                            iae.getLocalizedMessage()
                    ))
            );
          }
        }
      }

      boolean subroute = false;
      boolean tethering = false;
      boolean lan = false;

      if (subroute) {
        // Exclude IP ranges
        List<IPUtil.CIDR> listExclude = new ArrayList<>();
        listExclude.add(new IPUtil.CIDR("127.0.0.0", 8)); // localhost

        if (tethering) {

          listExclude.add(new IPUtil.CIDR("127.0.0.0", 8)); // localhost

          // USB tethering 192.168.42.x
          // Wi-Fi tethering 192.168.43.x
          listExclude.add(new IPUtil.CIDR("192.168.42.0", 23));
          // Bluetooth tethering 192.168.44.x
          listExclude.add(new IPUtil.CIDR("192.168.44.0", 24));
          // Wi-Fi direct 192.168.49.x
          listExclude.add(new IPUtil.CIDR("192.168.49.0", 24));
        }

        if (lan) {
          try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis.hasMoreElements()) {
              NetworkInterface ni = nis.nextElement();
              if (ni != null && ni.isUp() && !ni.isLoopback() &&
                      ni.getName() != null && !ni.getName().startsWith("tun"))
                for (InterfaceAddress ia : ni.getInterfaceAddresses())
                  if (ia.getAddress() instanceof Inet4Address) {
                    IPUtil.CIDR local = new IPUtil.CIDR(ia.getAddress(), ia.getNetworkPrefixLength());
                    AppLogManager.addToLog("Excluding " + ni.getName() + " " + local);
                    listExclude.add(local);
                  }
            }
          } catch (SocketException ex) {
            // Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
          }
        }

        // https://en.wikipedia.org/wiki/Mobile_country_code
        Configuration config = mContext.getResources().getConfiguration();

        // T-Mobile Wi-Fi calling
        if (config.mcc == 310 && (config.mnc == 160 ||
                config.mnc == 200 ||
                config.mnc == 210 ||
                config.mnc == 220 ||
                config.mnc == 230 ||
                config.mnc == 240 ||
                config.mnc == 250 ||
                config.mnc == 260 ||
                config.mnc == 270 ||
                config.mnc == 310 ||
                config.mnc == 490 ||
                config.mnc == 660 ||
                config.mnc == 800)) {
          listExclude.add(new IPUtil.CIDR("66.94.2.0", 24));
          listExclude.add(new IPUtil.CIDR("66.94.6.0", 23));
          listExclude.add(new IPUtil.CIDR("66.94.8.0", 22));
          listExclude.add(new IPUtil.CIDR("208.54.0.0", 16));
        }

        // Verizon wireless calling
        if ((config.mcc == 310 &&
                (config.mnc == 4 ||
                        config.mnc == 5 ||
                        config.mnc == 6 ||
                        config.mnc == 10 ||
                        config.mnc == 12 ||
                        config.mnc == 13 ||
                        config.mnc == 350 ||
                        config.mnc == 590 ||
                        config.mnc == 820 ||
                        config.mnc == 890 ||
                        config.mnc == 910)) ||
                (config.mcc == 311 && (config.mnc == 12 ||
                        config.mnc == 110 ||
                        (config.mnc >= 270 && config.mnc <= 289) ||
                        config.mnc == 390 ||
                        (config.mnc >= 480 && config.mnc <= 489) ||
                        config.mnc == 590)) ||
                (config.mcc == 312 && (config.mnc == 770))) {
          listExclude.add(new IPUtil.CIDR("66.174.0.0", 16)); // 66.174.0.0 - 66.174.255.255
          listExclude.add(new IPUtil.CIDR("66.82.0.0", 15)); // 69.82.0.0 - 69.83.255.255
          listExclude.add(new IPUtil.CIDR("69.96.0.0", 13)); // 69.96.0.0 - 69.103.255.255
          listExclude.add(new IPUtil.CIDR("70.192.0.0", 11)); // 70.192.0.0 - 70.223.255.255
          listExclude.add(new IPUtil.CIDR("97.128.0.0", 9)); // 97.128.0.0 - 97.255.255.255
          listExclude.add(new IPUtil.CIDR("174.192.0.0", 9)); // 174.192.0.0 - 174.255.255.255
          listExclude.add(new IPUtil.CIDR("72.96.0.0", 9)); // 72.96.0.0 - 72.127.255.255
          listExclude.add(new IPUtil.CIDR("75.192.0.0", 9)); // 75.192.0.0 - 75.255.255.255
          listExclude.add(new IPUtil.CIDR("97.0.0.0", 10)); // 97.0.0.0 - 97.63.255.255
        }

        // Broadcast
        listExclude.add(new IPUtil.CIDR("224.0.0.0", 3));

        Collections.sort(listExclude);

        try {
          InetAddress start = InetAddress.getByName("0.0.0.0");
          for (IPUtil.CIDR exclude : listExclude) {
            AppLogManager.addToLog("Exclude " + exclude.getStart().getHostAddress() + ", " + exclude.getEnd().getHostAddress());
            for (IPUtil.CIDR include : IPUtil.toCIDR(start, IPUtil.minus1(exclude.getStart())))
              try {
                builder.addRoute(include.address, include.prefix);
              } catch (Throwable ex) {
                // Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
              }
            start = IPUtil.plus1(exclude.getEnd());
          }
          String end = (lan ? "255.255.255.254" : "255.255.255.255");
          for (IPUtil.CIDR include : IPUtil.toCIDR("224.0.0.0", end))
            try {
              builder.addRoute(include.address, include.prefix);
            } catch (Throwable ex) {
              // Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            }
        } catch (UnknownHostException ex) {
          // Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
      }

      NetworkSpace.IpAddress multicastRange = new NetworkSpace.IpAddress(new CIDRIP("224.0.0.0", 3), true);

      for (NetworkSpace.IpAddress route : mRoutes.getPositiveIPList()) {
        try {
          if (multicastRange.containsNet(route)) {
            ////AppLogManager.logDebug("VPN: Ignorando rota multicast: " + route.toString());
          } else {
            builder.addRoute(route.getIPv4Address(), route.networkMask);
          }

        } catch (IllegalArgumentException ia) {
          //mHostService.onDiagnosticMessage("Rota rejeitada: " + route + " " + ia.getLocalizedMessage());
        }
      }

      for (NetworkSpace.IpAddress route6 : mRoutesv6.getPositiveIPList()) {
        try {
          builder.addRoute(route6.getIPv6Address(), route6.networkMask);
        } catch (IllegalArgumentException ia) {
          //SkStatus.logInfo("Rejected routes: " + route + " " + ia.getLocalizedMessage());
        }
      }

      if (!isBypass) {
        try {
          String checkV6 = TextUtils.join(", ", mRoutesv6.getNetworks(false));
          AppLogManager.addToLog("Routes: " + TextUtils.join(", ", mRoutes.getNetworks(true)));
          AppLogManager.addToLog("Routes excluded (IPv4): " + TextUtils.join(", ", mRoutes.getNetworks(false)));
          if (checkV6 != "") {
            AppLogManager.addToLog("Routes excluded (IPv6):" + TextUtils.join(", ", mRoutesv6.getNetworks(false)));
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      // bypass in the app itself to generate data
      builder.addDisallowedApplication(mContext.getPackageName());

      tunFd = builder
              .setSession(getApplicationName())
              .establish();


      String m_socksServerAddress = String.format("127.0.0.1:%s", AppPrefs.getDynamicPort());
      String m_udpResolver = AppPrefs.isEnableUDP() ? AppPrefs.getUDPResolver() : null;

      if (m_udpResolver != null && !m_udpResolver.matches("^\\d{1,3}(\\.\\d{1,3}){3}:\\d+$")) {
        m_udpResolver = null;
      }

      connectTunnel(
              m_socksServerAddress,
              m_udpResolver
      );

      mRoutes.clear();

      return tunFd != null;
    } catch (Exception e) {
      AppLogManager.addToLog("Failed to establish the VPN " + e);
      return false;
    }
  }

  /**
   * author: staffnetDev git
   * This method configures the VPN service by setting the allowed VPN packages.
   * It adds the packages that are disallowed from using the VPN.
   *
   * @param builder The VpnService.Builder instance used to configure the VPN.
   * @see #setAllowedVpnPackages(VpnService.Builder)
   */
  private void setAllowedVpnPackages(VpnService.Builder builder) {
    Set<String> excludedApps;
    excludedApps = AppPrefs.getStringSet("selectedApps", new HashSet<>());

    for (int i = 0; i < excludedApps.size(); i++) {
      try {
        if (!excludedApps.toArray()[i].toString().equals(mContext.getPackageName())) {
          builder.addDisallowedApplication(excludedApps.toArray()[i].toString());
        }
      } catch (PackageManager.NameNotFoundException e) {
        // Log the error if the package name is not found
        AppLogManager.addToLog("<strong></font><font color=red>" + mContext.getString(R.string.app_no_longer_exists) + " </strong>" + excludedApps.toArray()[i].toString());
      }
    }
  }
  private static final String VPN_INTERFACE_NETMASK = "255.255.255.0";
  private static final int DNS_RESOLVER_PORT = 53;
  boolean transparentDns = !AppPrefs.isEnableCustomDNS();

  public synchronized void connectTunnel(
          final String socksServerAddress,
          String m_udpResolver

  ) {
    if (socksServerAddress == null) {
      throw new IllegalArgumentException("Must provide an IP address to a SOCKS server.");
    }
    if (tunFd == null) {
      throw new IllegalStateException("Must establish the VPN before connecting the tunnel.");
    }
    if (tun2socksThread != null) {
      throw new IllegalStateException("Tunnel already connected");
    }

    isRunning = true;

    if (isBypass) {
      return;
    }

    String dnsgwRelay;
    int pdnsdPort = VpnUtils.findAvailablePort(8091, 10);

    String[] mServidorDNS = m_dnsResolvers;

    dnsgwRelay =
            String.format("%s:%d", mPrivateAddress.mIpAddress, pdnsdPort);

    mPdnsd =
            new Pdnsd(
                    mContext,
                    mServidorDNS,
                    DNS_RESOLVER_PORT,
                    mPrivateAddress.mIpAddress,
                    pdnsdPort
            );

    String finalDnsgwRelay = dnsgwRelay;
    mPdnsd.setOnPdnsdListener(
            new Pdnsd.OnPdnsdListener() {
              @Override
              public void onStart() {
                if (!isBypass) {
                  AppLogManager.addToLog("pdnsd relay: " + finalDnsgwRelay);

                }
                //addLog("pdnsd started");
              }

              @Override
              public void onStop() {
                //addLog("pdnsd stopped");
                //stop();
              }
            }
    );

    mPdnsd.start();

    // Tun2socks
    mTun2Socks =
            new Tun2Socks(
                    mContext,
                    tunFd,
                    mMtu,
                    mPrivateAddress.mRouter,
                    VPN_INTERFACE_NETMASK,
                    socksServerAddress,
                    m_udpResolver,
                    dnsgwRelay,
                    transparentDns
            );

    mTun2Socks.setOnTun2SocksListener(
            new Tun2Socks.OnTun2SocksListener() {
              @Override
              public void onStart() {
                if (!isBypass) {
                  AppLogManager.addToLog("socks local: " + socksServerAddress);
                }
              }

              @Override
              public void onStop() {
                //AppLogManager.addToLog("tun2socks stopped");
                //stop();
              }
            }
    );

    mTun2Socks.start();

    //EXIBE DNS DA REDE
    if (!isBypass) {
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    //VPN OK
    if (!isBypass) {
      AppLogManager.addToLog("<b><font color=#49C53C>VPN Tunnel Connected!</font></b>");
      //showCurrentDNS();
    }

  }

  private void showCurrentDNS() {
    try {
      String dnsList = "";
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          List<InetAddress> dnsServers = connectivityManager
                  .getLinkProperties(connectivityManager.getActiveNetwork())
                  .getDnsServers();

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dnsList = dnsServers.stream()
                    .map(InetAddress::getHostAddress)
                    .collect(Collectors.joining(", "));
          }
        }
      } catch (Exception e) {
        dnsList = "?";
        e.printStackTrace();
      }

      AppLogManager.addToLog("DNS: " + dnsList);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public final String getApplicationName() throws PackageManager.NameNotFoundException {
    PackageManager packageManager = mContext.getPackageManager();
    ApplicationInfo appInfo = packageManager.getApplicationInfo(mContext.getPackageName(), 0);
    return (String) packageManager.getApplicationLabel(appInfo);
  }

  private int GetIPV6Mask(String cidr){
    if (cidr.contains("/")) {
      int index = cidr.indexOf("/");
      String networkPart = cidr.substring(index + 1);
      return Integer.parseInt(networkPart);
    } else {
      throw new IllegalArgumentException("not an valid CIDR format!");
    }
  }


}
