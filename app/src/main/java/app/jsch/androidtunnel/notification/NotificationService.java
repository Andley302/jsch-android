package app.jsch.androidtunnel.notification;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import app.jsch.androidtunnel.MainActivity;
import app.jsch.androidtunnel.R;
import app.jsch.androidtunnel.logs.AppLogManager;
import app.jsch.androidtunnel.AppPrefs;
import app.jsch.androidtunnel.services.connectionservices.ssh.ConnectVPN;

public class NotificationService extends Service {
    final static int NOTIFY_STARTING = 100260;
    final static int NOTIFY_CONNECTED = 100261;
    static NotificationManager notifManager = null;
    static NotificationManager notifManagerStarting = null;
    String title;
    String body;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getExtras() != null){
            title = intent.getStringExtra("TITLE") + " - " +  AppPrefs.getNetworkName();
            body = intent.getStringExtra("BODY");
            boolean isConnected = intent.getBooleanExtra("IS_CONNECTED",false);
            boolean isReconect = intent.getBooleanExtra("IS_RECON",false);

            if (isConnected){
                ConnectedNotification();
            }else{
                if (ConnectVPN.IS_SERVICE_RUNNING){
                    StartingNotification();
                }

            }

            if (isReconect){
                DisconnectedNotification();
            }




        }

        return START_STICKY;
    }
    public void StartingNotification() {
        try{
            if (notifManagerStarting  == null) {
                notifManagerStarting  =
                        (NotificationManager) this.getSystemService(this.NOTIFICATION_SERVICE);
            }

            final NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
            bigText.setBigContentTitle(title);

            final NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, String.valueOf(NOTIFY_STARTING));


            Intent intent;
            PendingIntent pendingIntent;
            intent = this.getPackageManager().getLaunchIntentForPackage(this.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            }

            //	bigText.setSummaryText("Velocidade da rede");
            bigText.bigText(body);

            pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            mBuilder.setContentIntent(pendingIntent);

            mBuilder.setSmallIcon(R.drawable.baseline_cloud_24) ;// required
            mBuilder.setContentTitle(title);
            mBuilder.setContentText(body);
            mBuilder.setPriority(Notification.PRIORITY_LOW);
            mBuilder.setStyle(bigText);
            mBuilder.setOnlyAlertOnce(true);
            //mBuilder.setWhen(System.currentTimeMillis());
            mBuilder.setUsesChronometer(true);
            mBuilder.setSound(null);
            mBuilder.setOngoing(true);



            // === Removed some obsoletes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = String.valueOf(NOTIFY_STARTING);
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        title,
                        NotificationManager.IMPORTANCE_LOW);
                notifManagerStarting .createNotificationChannel(channel);
                channel.setSound(null, null);
                mBuilder.setChannelId(channelId);
            }


            Notification notification = mBuilder.build();
            notifManagerStarting .notify(NOTIFY_STARTING, notification);

            try {
                //this.startForeground(NOTIFY_STARTING, notification);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.startForeground(NOTIFY_STARTING, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                }else{
                    this.startForeground(NOTIFY_STARTING, notification);
                }
            } catch (Exception e) {
                AppLogManager.addToLog("Notification error: " + e);
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public void ConnectedNotification() {
        try{
            //REMOVE NOTIFICAÇÕES E INICIA
            try{
                notifManagerStarting.cancelAll();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // There are hardcoding only for show it's just strings
            String name = "One VPN";
            String id = "One VPN Notification"; // The user-visible name of the channel.
            String description = "One VPN Notification Service"; // The user-visible description of the channel.

            Intent intent;
            PendingIntent pendingIntent;
            //PendingIntent pendingReconnect;
            NotificationCompat.Builder builder;

            if (notifManager == null) {
                notifManager =
                        (NotificationManager)this.getSystemService(this.NOTIFICATION_SERVICE);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = notifManager.getNotificationChannel(id);
                if (mChannel == null) {
                    mChannel = new NotificationChannel(id, name, importance);
                    mChannel.setDescription(description);
                    mChannel.enableVibration(true);
                    mChannel.setLightColor(Color.GREEN);
                    mChannel.setVibrationPattern(new long[]{ 0 });
                  // mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                    notifManager.createNotificationChannel(mChannel);
                }
                builder = new NotificationCompat.Builder(this, id);

                intent = this.getPackageManager().getLaunchIntentForPackage(this.getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                }

                pendingIntent =  PendingIntent.getActivity(this, 0, intent,  PendingIntent.FLAG_IMMUTABLE);


                builder.setContentTitle(title)  // required
                        //.setSmallIcon(android.R.drawable.ic_popup_reminder) // required
                        .setSmallIcon(R.drawable.baseline_cloud_done_24) // required
                        .setContentText(body)  // required
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setOngoing(true)
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(false)
                        .setContentIntent(pendingIntent)
                        .setTicker(title);
                        //.setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            } else {

                builder = new NotificationCompat.Builder(this);

                intent = this.getPackageManager().getLaunchIntentForPackage(this.getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                }

                pendingIntent =  PendingIntent.getActivity(this, 0, intent,  PendingIntent.FLAG_IMMUTABLE);


                builder.setContentTitle(title)                           // required
                        //.setSmallIcon(android.R.drawable.ic_popup_reminder) // required
                        .setSmallIcon(R.drawable.baseline_cloud_done_24) // required
                        .setContentText(body)  // required
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setAutoCancel(false)
                        //.setOnlyAlertOnce(true)
                        .setOngoing(true)
                        .setContentIntent(pendingIntent)
                        .setTicker(title)
                       // .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                        .setPriority(Notification.PRIORITY_HIGH);
            }

            Notification notification = builder.build();
            notifManager.notify(NOTIFY_CONNECTED, notification);

           try{
               this.startForeground(NOTIFY_CONNECTED,notification);
           } catch (Exception e) {
               AppLogManager.addToLog("Notification error: " + e);
               e.printStackTrace();
           }
        } catch (Exception e) {
           // ServiceManagerModule.addLog("Notification: " + e);
            e.printStackTrace();
        }

    }

    public void DisconnectedNotification() {
        try{
            //REMOVE NOTIFICAÇÕES E INICIA
            try{
                notifManagerStarting.cancelAll();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // There are hardcoding only for show it's just strings
            String name = "One VPN";
            String id = "One VPN Notification"; // The user-visible name of the channel.
            String description = "One VPN Notification Service"; // The user-visible description of the channel.

            Intent intent;
            PendingIntent pendingIntent;
            //PendingIntent pendingReconnect;
            NotificationCompat.Builder builder;

            if (notifManager == null) {
                notifManager =
                        (NotificationManager)this.getSystemService(this.NOTIFICATION_SERVICE);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = notifManager.getNotificationChannel(id);
                if (mChannel == null) {
                    mChannel = new NotificationChannel(id, name, importance);
                    mChannel.setDescription(description);
                    mChannel.enableVibration(true);
                    mChannel.setLightColor(Color.GREEN);
                    mChannel.setVibrationPattern(new long[]{ 0 });
                    // mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                    notifManager.createNotificationChannel(mChannel);
                }
                builder = new NotificationCompat.Builder(this, id);

                intent = this.getPackageManager().getLaunchIntentForPackage(this.getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                }

                pendingIntent =  PendingIntent.getActivity(this, 0, intent,  PendingIntent.FLAG_IMMUTABLE);


                builder.setContentTitle(title)  // required
                        //.setSmallIcon(android.R.drawable.ic_popup_reminder) // required
                        .setSmallIcon(R.drawable.baseline_cloud_off_24) // required
                        .setContentText(body)  // required
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setOngoing(false)
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setTicker(title);
                //.setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            } else {

                builder = new NotificationCompat.Builder(this);

                intent = this.getPackageManager().getLaunchIntentForPackage(this.getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                }

                pendingIntent =  PendingIntent.getActivity(this, 0, intent,  PendingIntent.FLAG_IMMUTABLE);

                builder.setContentTitle(title)                           // required
                        //.setSmallIcon(android.R.drawable.ic_popup_reminder) // required
                        .setSmallIcon(R.drawable.baseline_cloud_off_24) // required
                        .setContentText(body)  // required
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setOngoing(false)
                        .setContentIntent(pendingIntent)
                        .setTicker(title)
                        // .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                        .setPriority(Notification.PRIORITY_HIGH);
            }

            Notification notification = builder.build();
            notifManager.notify(NOTIFY_CONNECTED, notification);

            try{
                this.startForeground(NOTIFY_CONNECTED,notification);
            } catch (Exception e) {
                AppLogManager.addToLog("Notification error: " + e);
                e.printStackTrace();
            }
        } catch (Exception e) {
            // ServiceManagerModule.addLog("Notification: " + e);
            e.printStackTrace();
        }

    }

    public static void stopNotification(){
        try{
            notifManager.cancelAll();
            notifManagerStarting.cancelAll();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        if (AppPrefs.isEnableNotification()){
            stopNotification();
        }
        stopSelf();
        super.onDestroy();
    }


    
    
}
