package app.jsch.androidtunnel.services.injector;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import app.jsch.androidtunnel.AppPrefs;
import app.jsch.androidtunnel.MainActivity;
import app.jsch.androidtunnel.R;
import app.jsch.androidtunnel.logs.AppLogManager;
import app.jsch.androidtunnel.services.connectionservices.ResolveAddrs;
import app.jsch.androidtunnel.services.connectionservices.ssh.ConnectVPN;
import app.jsch.androidtunnel.vpnutils.TunnelUtils;

import org.jetbrains.annotations.Nullable;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class InjectionService extends Service implements Runnable {

  private String TAG = "InjectionService";
  private Thread mInjectThread;
  private ServerSocket listen_socket;
  private Socket input;
  private static Socket output;
  private HTTPThread sc1;
  private HTTPThread sc2;
  private int lport = 8084;

  private static final int CHECK_CONNECTION_DELAY = 1000; //original 5000
  private final Handler handlerNetworkStatus = new Handler();

  @Nullable
  public IBinder onBind(Intent intent) {
    return null;
  }

  public void onCreate() {
    super.onCreate();
  }

  @SuppressLint("WrongConstant")
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (mInjectThread != null) {
      mInjectThread.interrupt();
    }
    //AppLogManager.addToLog("Starting injection thread...");
    mInjectThread = new Thread(this, "mInjectThread");
    mInjectThread.start();

    return 1;
  }

  public void onDestroy() {
    super.onDestroy();
    stopInjectThread();
  }

  @Override
  public void run() {
    try {

      listen_socket = new ServerSocket(lport);
      listen_socket.setReuseAddress(true);
      while (true) {
        input = listen_socket.accept();
        input.setSoTimeout(0);

        if (AppPrefs.getConnectionMode().equals("HTTP_PROXY")) {
          AppLogManager.addToLog("<b>Mode: http proxy</b>");
          AppLogManager.addToLog("Connecting, please wait...");
          output = new HTTPProxy(input).socket2();
        }

        if (AppPrefs.getConnectionMode().equals("HTTPS_PROXY")) {
          AppLogManager.addToLog("<b>Mode: https + payload</b>");
          AppLogManager.addToLog("Connecting, please wait...");
          output = new SSLProxy(input).inject();
        }
        if (AppPrefs.getConnectionMode().equals("HTTPS")) {
          AppLogManager.addToLog("<b>Mode: https</b>");
          AppLogManager.addToLog("Connecting, please wait...");
          output = new SSLTunnel(input).inject();
        }


        if (input != null) {
          input.setKeepAlive(true);
        }
        if (output != null) {
          output.setKeepAlive(true);
        }
        if (output == null) {
          output.close();
        }

        if (output.isConnected()) {
          //SOCKET CONECTADO
          Log.d(TAG, "Socket output is connected");

          sc1 = new HTTPThread(input, output, true);
          sc2 = new HTTPThread(output, input, false);
          sc1.setDaemon(true);
          sc1.start();
          sc2.setDaemon(true);
          sc2.start();
        } else {
          AppLogManager.addToLog(
            "<strong></font><font color=#FF0000>Socket output is no connected</strong><br><br><strong></strong><br>"
          );
          Log.d(TAG, "Socket output is no connected");
        }
      }
    } catch (Exception e) {
      if (e instanceof NullPointerException) {
        // Código a ser executado quando uma NullPointerException for capturada
        if (ConnectVPN.IS_SERVICE_RUNNING) {
          new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
              //verifica rede
              networkTimer.run();
            }
          }, 500);
        }
      } else {
        // Código a ser executado quando uma exceção diferente de NullPointerException for capturada
        if (ConnectVPN.IS_SERVICE_RUNNING) {
          AppLogManager.addToLog(
                  "<strong><font color=#FF0000>VPN: </strong></font>" + e.toString()
          );
        }
      }


    }
  }

  Runnable networkTimer = new Runnable() {
    @Override
    public void run() {

      if(!TunnelUtils.isNetworkOnline(InjectionService.this)) {
        AppLogManager.addToLog("No network...");
        handlerNetworkStatus.postDelayed(this, CHECK_CONNECTION_DELAY);
      }else{
        reiniciarThread();
      }
    }
  };

  @SuppressLint("WrongConstant")
  public void reiniciarThread() {
    try {
      if (sc1 != null) {
        sc1.interrupt();
        sc1 = null;
      }
      if (sc2 != null) {
        sc2.interrupt();
        sc2 = null;
      }

      if (listen_socket != null) {
        listen_socket.close();
        listen_socket = null;
      }
      if (input != null) {
        input.close();
        input = null;
      }
      if (output != null) {
        output.close();
        output = null;
      }
      if (mInjectThread != null) {
        mInjectThread.interrupt();
      }

      handlerNetworkStatus.removeCallbacks(networkTimer);

      // Criar e iniciar uma nova instância da thread
      mInjectThread = new Thread(this, "mInjectThread");
      mInjectThread.start();



    } catch (Exception e) {}

  }


  public void stopInjectThread() {
    Log.i(TAG, "Socket Stopped");
    try {
      if (sc1 != null) {
        sc1.interrupt();
        sc1 = null;
      }
      if (sc2 != null) {
        sc2.interrupt();
        sc2 = null;
      }

      if (listen_socket != null) {
        listen_socket.close();
        listen_socket = null;
      }
      if (input != null) {
        input.close();
        input = null;
      }
      if (output != null) {
        output.close();
        output = null;
      }
      if (mInjectThread != null) {
        mInjectThread.interrupt();
      }
    } catch (Exception e) {}
  }

}
