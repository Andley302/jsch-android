package app.jsch.androidtunnel.configs;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import app.jsch.androidtunnel.AppPrefs;
import app.jsch.androidtunnel.R;


public class ExportConfig extends AppCompatActivity {
    public static SharedPreferences app_prefs;


    private String configToExport;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.empty);

        app_prefs = AppPrefs.sharedPreferences;

        export();

    }

    private String getJschFilePathWithTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        return "jsch_" + timestamp + ".jsch";
    }

    // Request code for creating a PDF document.
    private static final int CREATE_FILE = 1;

    private void createFile(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, getJschFilePathWithTimestamp());

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, CREATE_FILE);

    }

    private void export(){
        JSONObject configJson = new JSONObject();

        try{
            configJson.put("username", AppPrefs.getUsername());
            configJson.put("password", AppPrefs.getPassword());
            configJson.put("udpResolver", AppPrefs.getUDPResolver());
            configJson.put("tlsVersion", AppPrefs.getTLSVersion());
            configJson.put("connectionMode", AppPrefs.getConnectionMode());
            configJson.put("payload", AppPrefs.getPayloadKey());
            configJson.put("sni", AppPrefs.getSNI());
            configJson.put("dns1", AppPrefs.getDNS1());
            configJson.put("dns2", AppPrefs.getDNS2());
            configJson.put("proxyIP", AppPrefs.getProxyIP());
            configJson.put("proxyPort", AppPrefs.getProxyPort());
            configJson.put("wakeLockEnabled", AppPrefs.isEnableWakeLock());
            configJson.put("sshCompressEnabled", AppPrefs.isEnableSSHCompress());
            configJson.put("udpEnabled", AppPrefs.isEnableUDP());
            configJson.put("dnsEnabled", AppPrefs.isEnableCustomDNS());

        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.erro_save_file), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        //configToExportOnPermisson = configJson.toString().replace("\n","");
        configToExport = configJson.toString();

        //createFile(toEnc);
        Uri uri = Uri.parse("file://" + Environment.getExternalStorageDirectory() + "/Download");
        createFile(uri);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                try {
                    Uri uri = data.getData();

                    OutputStream outputStream = getContentResolver().openOutputStream(uri);

                    outputStream.write(configToExport.getBytes());

                    outputStream.close(); // very important

                    Toast.makeText(this, getString(R.string.export_sucess), Toast.LENGTH_SHORT).show();
                    finish();
                } catch (IOException e) {
                    Toast.makeText(this, getString(R.string.erro_save_file), Toast.LENGTH_SHORT).show();

                    e.printStackTrace();
                }
            }
        }else{
            Toast.makeText(this,getString(R.string.erro_save_file) , Toast.LENGTH_SHORT).show();
        }
    }
}
