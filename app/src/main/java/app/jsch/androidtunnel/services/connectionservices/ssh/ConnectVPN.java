package app.jsch.androidtunnel.services.connectionservices.ssh;

/* Copyright 2011 Shai Ayal
 *
 * This file is part of SPT.
 *
 * SPT is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SPT is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SPT.  If not, see <http://www.gnu.org/licenses/>.
 */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.ProxyDirect;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.Session;
import app.jsch.androidtunnel.AppPrefs;
import app.jsch.androidtunnel.R;
import app.jsch.androidtunnel.services.connectionservices.ResolveAddrs;
import app.jsch.androidtunnel.services.injector.InjectionService;
import app.jsch.androidtunnel.logs.AppLogManager;
import app.jsch.androidtunnel.notification.NotificationService;
import app.jsch.androidtunnel.vpnutils.TunnelUtils;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 *         The service in charge of maintaining a working SSH connection with
 *         port forwarding. It should be robust, and should try to reconnect at
 *         any sign of connection problem.
 *
 * @author Shai Ayal.
 */

public class ConnectVPN extends Service {

  public static final String TAG = "SPT.Connection";
  PowerManager.WakeLock wakeLock;

  private void setWakelock()
  {
    try
    {
      //CERTO DEPOIS DE 10 MIN
      AppLogManager.addToLog("Acquire wakelock");
      this.wakeLock.acquire(10*60*1000L /*10 minutes*/);
      wakeLock.acquire();
    }
    catch (Exception e)
    {
      Log.d("WAKELOCK", e.getMessage());
    }
  }


  private void unsetWakelock()
  {
    if (this.wakeLock != null && this.wakeLock.isHeld())
    {
      try{
        this.wakeLock.release();
        AppLogManager.addToLog("Release wakelock");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public enum ConnectionState {
    CONNECTED, // Connected (including loss of connectivity not related to network change)
    PENDING, // Waiting before reconnect after new network has come up
    NO_NETWORK, // No available network
    IDLE, // Doing nothing -- no ConnectionInfo
    CONNECTING, // Connection process -- makes the checkTimer return immediately
  }

  private static class ConnectionStateWrapper {

    private ConnectionState state = null;

    public ConnectionStateWrapper() {
      set(ConnectionState.IDLE);
    }

    public ConnectionStateWrapper(
      ConnectionStateWrapper connectionStateWrapper
    ) {
      set(connectionStateWrapper.get());
    }

    public ConnectionState get() {
      return state;
    }

    public void set(ConnectionState state) {
      this.state = state;
    }
  }

  private ConnectionStateWrapper state = new ConnectionStateWrapper();

  public static ConnectionInfo ci = null;

  private DynamicForwarder df = null;

  // JSch SSH members
  private JSch jsch = null;
  private Session session = null;
  private static final int SOCKET_TIMEOUT = 5000;

  private ConnectivityChangeReceiver ccr = null;

  private static final int CHECK_CONNECTION_DELAY = 1500; //original 5000
  private Timer checkTimer = null;

  private CharSequence lastNotification = "";

  public CharSequence getLastNotification() {
    return lastNotification;
  }

  public static boolean IS_SERVICE_RUNNING;

  /*
   * (non-Javadoc)
   *
   * @see android.app.Service#onCreate()
   */
  @Override
  public void onCreate() {
    super.onCreate();

    IS_SERVICE_RUNNING = true;

    //AppLogManager.addToLog("Starting VPN...");

    //SETA WAKELOCK
    if (AppPrefs.isEnableWakeLock()){
      try{
        wakeLock = ((PowerManager)getSystemService(Context.POWER_SERVICE)).newWakeLock(1, "vpn test::WakeLock");
        setWakelock();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    //INFO DA REDE
    // -----------------------------------------------------------------------------------
    // Initialize state --  NOTE -- this should come first!
    NetworkInfo ni =
      (
        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)
      ).getActiveNetworkInfo();
    if (null != ni) {
      if (ni.getState() == State.CONNECTED) {
        state.set(ConnectionState.IDLE);
      } else {
        state.set(ConnectionState.NO_NETWORK);
      }
    }

    StartingVPNNotification();

    try {
      AppLogManager.addToLog("Current network: " + ResolveAddrs.getOperatorName() + " | " + ResolveAddrs.getAPN());

      String ipAddress = TunnelUtils.getLocalIpAddress();

      if (ipAddress != null && !ipAddress.trim().isEmpty()) {
        String[] ipAddresses = ipAddress.split("[\n,]");

        for (String ip : ipAddresses) {
          ip = ip.trim();
          if (!ip.isEmpty()) {
            AppLogManager.addToLog("IP: " + ip);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    //payload
    String proxyAddress = AppPrefs.getProxyIP() + ":" + AppPrefs.getProxyPort();
    AppLogManager.addToLog("Connecting to: " + proxyAddress);
    AppPrefs.setNetworkName(proxyAddress);

    if (!AppPrefs.getConnectionMode().equals("HTTPS")) {
      AppLogManager.addToLog("Payload: " + AppPrefs.getPayloadKey());
    }

    //REINICIA TUNNEL COM BYPASS PRA INJEÇÃO DE PAYLOAD
    startTunnel(true);

    //inicia serviço de injeção
    if (!AppPrefs.getConnectionMode().equals("HTTP_DIRECT")) {
      startInjector();
    }

    // -----------------------------------------------------------------------------------
    // Jsch
    jsch = new JSch();

    // -----------------------------------------------------------------------------------
    // Connectivity status
    ccr = new ConnectivityChangeReceiver();
    registerReceiver(
      ccr,
      new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
    );

    //delay injector
    //REINICIA TUNNEL COM BYPASS PRA INJEÇÃO DE PAYLOAD
    startTunnel(true);

    new Timer()
      .schedule(
        new TimerTask() {
          @Override
          public void run() {
            ConnectivityRestored();
            ConnectivityLosShowLog = true;
          }
        },
        500
      );

    //Notify(R.drawable.ic_stat_ic_tunnel2_48px, "Service running", "SPT", "Service running");

    // -----------------------------------------------------------------------------------
    // checkTimer setup
    checkTimer = new Timer();
    checkTimer.schedule(
      new TimerTask() {
        @Override
        public void run() {
          synchronized (state) {
            switch (state.get()) {
              case CONNECTING:
                //AppLogManager.addToLog("Connecting...");
                // Nothing to do, we are in the middle of connection/disconnection
                break;
              case NO_NETWORK:
                // Nothing to do, Only a new network can get us out of this state ...
                AppLogManager.addToLog("No network...");
                noNetworkNotification();
                break;
              case IDLE:
                // If we have get ConnectionInfo, let's connect!
                if (ci != null) {
                  state.set(ConnectionState.CONNECTED);
                }
                break;
              case PENDING:
                // next time around we'll try to connect. This introduces a
                // delay to allow the newly connected network to settle down.
                ///AppLogManager.addToLog("Connecting pending...");
                state.set(ConnectionState.CONNECTED);
                break;
              case CONNECTED:
                if (ci == null) {
                  state.set(ConnectionState.IDLE);
                  ConnectivityLost();
                } else {
                  // reconnect if needed!
                  if (session == null || !session.isConnected()) {
                    reconnectNotification();
                    // Reconectando
                    AppLogManager.addToLog(
                      "<b><font color=#ffa500>Reconnecting...</font></b>"
                    );

                    //REINICIA TUNNEL COM BYPASS PRA INJEÇÃO DE PAYLOAD
                    startTunnel(true);


                    ConnectivityRestored();
                    ConnectivityLosShowLog = true;
                  }
                }
                break;
              default:
                AppLogManager.addToLog("Unknown state: " + state.toString());
                Log.d(TAG, "checkTimer: Unknown state " + state.toString());
                break;
            }
          }
        }
      },
      CHECK_CONNECTION_DELAY,
      CHECK_CONNECTION_DELAY
    );
  }

  private void reconnectNotification() {
    Intent startNotification = new Intent(this, NotificationService.class);
    startNotification.putExtra("TITLE",this.getString(R.string.app_name));
    startNotification.putExtra("BODY","Reconnecting...");
    startNotification.putExtra("IS_CONNECTED",false);
    try{
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.startForegroundService(startNotification);
      } else {
        this.startService(startNotification);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void noNetworkNotification() {
    Intent startNotification = new Intent(this, NotificationService.class);
    startNotification.putExtra("TITLE",this.getString(R.string.app_name));
    startNotification.putExtra("BODY","No network...");
    startNotification.putExtra("IS_CONNECTED",false);
    try{
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.startForegroundService(startNotification);
      } else {
        this.startService(startNotification);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void StartingVPNNotification() {
    if (AppPrefs.isEnableNotification()) {
      Intent startNotification = new Intent(this, NotificationService.class);
      startNotification.putExtra("TITLE", this.getString(R.string.app_name));
      startNotification.putExtra(
        "BODY",
        this.getString(R.string.starting_notification)
      );
      startNotification.putExtra("IS_CONNECTED", false);
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          this.startForegroundService(startNotification);
        } else {
          this.startService(startNotification);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see android.app.Service#onStart(android.content.Intent, int)
   */
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    return START_STICKY;
  }

  private void stopNotification() {
    if (AppPrefs.isEnableNotification()) {
      Intent stopNotification = new Intent(this, NotificationService.class);
      try {
        this.stopService(stopNotification);
      } catch (Exception e) {
        NotificationService.stopNotification();
        e.printStackTrace();
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see android.app.Service#onDestroy()
   */
  @Override
  public void onDestroy() {
    super.onDestroy();

    IS_SERVICE_RUNNING = false;

    if (checkTimer != null) {
      checkTimer.cancel();
      checkTimer.purge();
    }

    unregisterReceiver(ccr);

    ci = null;

    if (!AppPrefs.getConnectionMode().equals("HTTP_DIRECT")) {
      stopInjector();
    }

    if (AppPrefs.isEnableWakeLock()) {
      unsetWakelock();
    }

    if (df != null) {
      AppLogManager.addToLog("stopping jsch port forwarding...");
      df.stop();
    }

    stopTunnel();

    if (AppPrefs.isEnableNotification()) {
      stopNotification();
    }
  }


  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  public class ConnectivityChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      NetworkInfo info =
        (
          (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)
        ).getActiveNetworkInfo();
      synchronized (state) {
        if (info != null && info.getState() == State.CONNECTED) {
          if (ci == null) {
            state.set(ConnectionState.IDLE);
          } else {
            state.set(ConnectionState.PENDING);
          }
        } else {
          state.set(ConnectionState.NO_NETWORK);
          ConnectivityLost();
        }
      }
    }
  }

  private boolean ConnectivityLosShowLog = true;

  private void ConnectivityLost() {
    if (ConnectivityLosShowLog) {
      if (IS_SERVICE_RUNNING) {
        AppLogManager.addToLog(
          "<b><font color=#ffa500>Connection lost</font></b>"
        );
        AppLogManager.addToLog(
          "<b><font color=#FF4500>Waiting to reconnect...</font></b>"
        );
        ConnectivityLosShowLog = false;
      }
    }

    //vpn tunnel with bypass do inject custom payload
    startTunnel(true);

    synchronized (state) {
      ConnectionStateWrapper tmp = new ConnectionStateWrapper(state);
      state.set(ConnectionState.CONNECTING);
      try {
        if (session != null && session.isConnected()) {
          session.disconnect();
        }
        if (jsch != null) {
          jsch.removeAllIdentity();
        }
        if (df != null) {
          df.stop();
        }
      } catch (Exception e) {
        Log.e(TAG, "ConnectivityLost: " + e.toString());
        AppLogManager.addToLog("Connectivity Lost: " + e.toString());
      }
      state = new ConnectionStateWrapper(tmp);
    }
  }

  private void ConnectivityRestored() {
    synchronized (this) {
      ConnectionStateWrapper tmp = new ConnectionStateWrapper(state);
      state.set(ConnectionState.CONNECTING);
      if (null != ci) {
        try {
          if (ci.getKeypath().length() > 0) {
            jsch.addIdentity(ci.getKeypath());
          }
          jsch.setKnownHosts(
            getFilesDir() +
            File.separator +
            getResources().getText(R.string.knownhosts)
          );
          session = jsch.getSession(ci.getUser(), ci.getHost(), ci.getPort());
          session.setUserInfo(ci);

          //version
          AppLogManager.addToLog("jsch version: " + JSch.VERSION);
          AppLogManager.addToLog("ssh version: " + session.getClientVersion());

          //session.setConfig("HashKnownHosts",  "no");
          if (AppPrefs.getConnectionMode().equals("HTTP_PROXY")) {
            session.setProxy(new ProxyHTTP("127.0.0.1", 8084));
          }
          if (AppPrefs.getConnectionMode().equals("HTTPS_PROXY")) {
            session.setProxy(new ProxyHTTP("127.0.0.1", 8084));
          }
          if (AppPrefs.getConnectionMode().equals("HTTP_DIRECT")) {
            AppLogManager.addToLog("<b>Mode: http direct</b>");

            session.setProxy(
              new ProxyDirect(
                AppPrefs.getProxyIP(),
                AppPrefs.getProxyPort(),
                AppPrefs.getPayloadKey()
              )
            );
          }
          if (AppPrefs.getConnectionMode().equals("HTTPS")) {
            session.setProxy(new ProxyHTTP("127.0.0.1", 8084));
          }


          /*if (isHostKnown(ci.getHost())) {
						session.setConfig("StrictHostKeyChecking", "yes");
					}*/
          java.util.Properties config = new java.util.Properties();
          config.put("StrictHostKeyChecking", "no");
          //config.put("max_input_buffer_size", "1024");
          session.setConfig(
            "PreferredAuthentications",
            "password,keyboard-interactive"
          );
          session.setConfig(config);

          if (AppPrefs.isEnableSSHCompress()) {
            if (ci.getCompression()) {
              AppLogManager.addToLog("set ssh compression");
              session.setConfig(
                "compression.s2c",
                "zlib@openssh.com,zlib,none"
              );
              String level_compress = AppPrefs.getSSHCompressLevel();
              session.setConfig("compression_level", level_compress); //original 3
              AppLogManager.addToLog("compression_level: " + level_compress);
            }
          }


          /*String strictHostKeyChecking = session.getConfig("StrictHostKeyChecking");
          String preferredAuthentications = session.getConfig("PreferredAuthentications");
          String compressionS2C = session.getConfig("compression.s2c");

          AppLogManager.addToLog("StrictHostKeyChecking: " + strictHostKeyChecking);
          AppLogManager.addToLog("PreferredAuthentications: " + preferredAuthentications);
          AppLogManager.addToLog("compression.s2c: " + compressionS2C);*/

          session.connect();
          ci.setPortForwardingL(session);


          if (ci.getDynamic_port() > 0) {
            if (df != null) {
              df.stop();
            }
            AppLogManager.addToLog("starting jsch port forwarding...");
            df = new DynamicForwarder(ci.getDynamic_port(), session);
          }
          session.setServerAliveInterval(SOCKET_TIMEOUT);
          session.setServerAliveCountMax(10);
          state.set(ConnectionState.CONNECTED);

          AppLogManager.addToLog("<b><font color=#49C53C>Connected!</font></b>");

          startTunnel(false);

          ConnectedVPNNotification();

        } catch (Exception e) {
          if (session != null) {
            session.disconnect();
          }
          if (df != null) {
            df.stop();
          }
          AppLogManager.addToLog("<strong><font color=#FF0000>VPN: </strong></font>" +  e.toString());

        }
      }
      state = new ConnectionStateWrapper(tmp);
    }
  }

  private void ConnectedVPNNotification() {
    if (AppPrefs.isEnableNotification()) {
      Intent startNotification = new Intent(this, NotificationService.class);
      startNotification.putExtra("TITLE", this.getString(R.string.app_name));
      startNotification.putExtra("BODY", "Connected!");
      startNotification.putExtra("IS_CONNECTED", false);
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          this.startForegroundService(startNotification);
        } else {
          this.startService(startNotification);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void startTunnel(Boolean isBypass) {
    Intent tunnel = new Intent(this, EstablishVPN.class);
    tunnel.putExtra("ENABLE_BYPASS", isBypass);
    tunnel.putExtra("SERVER_IP", AppPrefs.getProxyIP());
    try {
      this.startService(tunnel);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void stopTunnel() {
    Intent intent = new Intent("stop_kill");
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  private void startInjector() {
    Intent injector = new Intent(this, InjectionService.class);
    NetworkInfo ni =
      (
        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)
      ).getActiveNetworkInfo();
    if (null != ni) {
      if (ni.getState() == State.CONNECTED) {
        try {
          this.startService(injector);
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        AppLogManager.addToLog("No network to connect thread");
      }
    }
  }

  private void stopInjector() {
    Intent injector = new Intent(this, InjectionService.class);
    try {
      this.stopService(injector);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Boolean isHostKnown(String host) {
    try {
      jsch.setKnownHosts(
        getFilesDir() +
        File.separator +
        getResources().getText(R.string.knownhosts)
      );
      HostKeyRepository hkr = jsch.getHostKeyRepository();
      HostKey[] hks = hkr.getHostKey();
      if (hks != null) {
        for (HostKey hk : hks) if (
          hk.getHost().compareToIgnoreCase(host) == 0
        ) {
          return true;
        }
      }
    } catch (Exception e) {
      Log.e(TAG, e.toString());
    }
    return false;
  }

  public ConnectionState getState() {
    return state.get();
  }
}
