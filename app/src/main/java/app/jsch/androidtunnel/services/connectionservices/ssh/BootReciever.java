package app.jsch.androidtunnel.services.connectionservices.ssh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootReciever extends BroadcastReceiver {
	// Fires up the tunnel on boot if so configured
	@Override
	public void onReceive(Context context, Intent arg1) {
		/*final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean("onboot", false)) {
			Intent intent = new Intent (context,Main.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
			intent.setAction(Main.ACTION_CONNECT);
			context.startActivity(intent);
		}*/
	}
}
