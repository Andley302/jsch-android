package app.jsch.androidtunnel.configs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import app.jsch.androidtunnel.AppPrefs;
import app.jsch.androidtunnel.MainActivity;
import app.jsch.androidtunnel.R;
import app.jsch.androidtunnel.services.connectionservices.ssh.ConnectVPN;


public class ImportConfig extends AppCompatActivity {

    public static SharedPreferences app_prefs;
    private boolean isImportFromExternal;
    public static SharedPreferences start_msg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty);

        app_prefs = AppPrefs.sharedPreferences;
        // Get the intent that started this activity
        Intent intent = getIntent();
        String scheme = intent.getScheme();

        if (!ConnectVPN.IS_SERVICE_RUNNING){
            // Figure out what to do based on the intent type
            if (scheme != null && (scheme.equals("file") || scheme.equals("content"))) {
                isImportFromExternal = true;

                Uri data = intent.getData();

                try {
                    ReadConfigFile(data);
                } catch(Exception e) {
                    Toast.makeText(this, R.string.error_file_config_incompatible,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }

                finish();
            }else{
                isImportFromExternal = false;
                Uri uri = Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/Download");
                importConfigFileFromAPI(uri);
            }
        }else{
            Toast.makeText(this, R.string.only_disconnected,
                    Toast.LENGTH_SHORT).show();
            finish();
        }

    }


    private static final int PICK_CONFIG_FILE = 2;
    private void importConfigFileFromAPI(Uri pickerInitialUri) {
        try{
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
            this.startActivityForResult(intent, PICK_CONFIG_FILE);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_file_config_incompatible,
                    Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_CONFIG_FILE) {
            // Get the Uri of the selected file
            try{
                Uri uri = data.getData();
                ReadConfigFile(uri);
            } catch (Exception e) {
                finish();
                e.printStackTrace();
            }
        }
    }

    public void ReadConfigFile(Uri uri) {
        BufferedReader br;
        FileOutputStream os;
        try {
            br = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            //WHAT TODO ? Is this creates new file with
            //the name NewFileName on internal app storage?
            os = openFileOutput("newFileName", Context.MODE_PRIVATE);
            String line = null;
            while ((line = br.readLine()) != null) {
                os.write(line.getBytes());
                checkAndImportConfig(line);
            }
            br.close();
            os.close();
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.import_file_error), Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }
    }

    private void checkAndImportConfig(String config){

            try{
                JSONObject jcfg = new JSONObject(config);

                resetAppPrefs();

                AppPrefs.setUsername(jcfg.getString("username"));
                AppPrefs.setPassword(jcfg.getString("password"));
                AppPrefs.setUDPResolver(jcfg.getString("udpResolver"));
                AppPrefs.setTLSVersion(jcfg.getString("tlsVersion"));
                AppPrefs.setPayloadKey(jcfg.getString("payload"));
                AppPrefs.setConnectionMode(jcfg.getString("connectionMode"));
                AppPrefs.setEnableWakeLock(jcfg.getBoolean("wakeLockEnabled"));
                AppPrefs.setEnableSSHCompress(jcfg.getBoolean("sshCompressEnabled"));
                AppPrefs.setEnableUDP(jcfg.getBoolean("udpEnabled"));
                AppPrefs.setEnableCustomDNS(jcfg.getBoolean("dnsEnabled"));
                AppPrefs.setSNI(jcfg.getString("sni"));
                AppPrefs.setDNS1(jcfg.getString("dns1"));
                AppPrefs.setDNS2(jcfg.getString("dns2"));
                AppPrefs.setProxyIP(jcfg.getString("proxyIP"));
                AppPrefs.setProxyPort(jcfg.getInt("proxyPort"));

                if (isImportFromExternal){
                        Toast.makeText(this, getString(R.string.import_sucess), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("IS_IMPORT",true);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }else{
                        Toast.makeText(this, getString(R.string.import_sucess), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.putExtra("IS_IMPORT",true);
                        //intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();

                    }


            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, getString(R.string.file_config_incompatible), Toast.LENGTH_SHORT).show();
                finish();
            }

    }

    private void resetAppPrefs() {
        SharedPreferences.Editor editor = app_prefs.edit();
        editor.clear();
        editor.apply();
    }

}
