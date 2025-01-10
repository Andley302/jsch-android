package app.jsch.androidtunnel;


import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import app.jsch.androidtunnel.configs.ExportConfig;
import app.jsch.androidtunnel.configs.ImportConfig;
import app.jsch.androidtunnel.logs.AppLogManager;
import app.jsch.androidtunnel.logs.DrawerLog;
import app.jsch.androidtunnel.services.connectionservices.ssh.ConnectVPN;
import app.jsch.androidtunnel.services.connectionservices.ssh.ConnectionInfo;
import app.jsch.androidtunnel.*;
public class MainActivity extends AppCompatActivity implements DrawerLayout.DrawerListener,View.OnClickListener{


    private DrawerLog mDrawer;
    private Toolbar app_toolbar;
    private Button start_connection;
    private FloatingActionButton logs;
    public static Context mContext;

    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText udpResolverEditText;
    private TextInputEditText payloadEditText;

    private TextInputEditText dns1Text;
    private TextInputEditText dns2Text;
    private TextInputEditText tlsSniText;
    private TextInputEditText proxyIp;
    private TextInputEditText proxyPort;

    private TextView  tlsVersionLabel;
    private TextInputLayout  tlsVersionInputLayout;

    // Switches
    private SwitchCompat wakeLockSwitch;
    private SwitchCompat sshCompressSwitch;
    private SwitchCompat udpSwitch;
    private SwitchCompat dnsSwitch;

    //Spinners
    private Spinner tlsVersionSpinner;
    private Spinner connectionModeSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drawer);

        getPermissionAndroid13();

        AppPrefs.init(this);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        mDrawer = new DrawerLog(this);
        app_toolbar = findViewById(R.id.app_toolbar);
        setSupportActionBar(app_toolbar);
        app_toolbar.requestFocus();

        mDrawer.setDrawer(this);

        mContext = this;

        logs = findViewById(R.id.logs);
        logs.setOnClickListener(this);

        start_connection = (Button) findViewById(R.id.start_connection);
        start_connection.setOnClickListener(this);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        udpResolverEditText = findViewById(R.id.udpResolverEditText);
        tlsVersionSpinner = findViewById(R.id.tlsVersionSpinner);
        connectionModeSpinner = findViewById(R.id.connectionModeSpinner);
        payloadEditText = findViewById(R.id.payloadEditText);

        tlsSniText= findViewById(R.id.tlsSniEditText);
        dns1Text = findViewById(R.id.dns1EditText);
        dns2Text = findViewById(R.id.dns2EditText);
        proxyIp = findViewById(R.id.serverIpEditText);
        proxyPort = findViewById(R.id.serverPortEditText);


        wakeLockSwitch = findViewById(R.id.wakeLockSwitch);
        sshCompressSwitch = findViewById(R.id.sshCompressSwitch);
        udpSwitch = findViewById(R.id.udpSwitch);
        dnsSwitch = findViewById(R.id.enableDnsSwitch);

        tlsVersionLabel = findViewById(R.id.tlsVersionLabel);
        tlsVersionInputLayout = findViewById(R.id.tlsVersionInputLayout);

        ArrayAdapter<CharSequence> tlsAdapter = ArrayAdapter.createFromResource(this,
                R.array.tls, android.R.layout.simple_spinner_item);
        tlsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tlsVersionSpinner.setAdapter(tlsAdapter);

        ArrayAdapter<CharSequence> connectionModeAdapter = ArrayAdapter.createFromResource(this,
                R.array.connection_modes, android.R.layout.simple_spinner_item);
        connectionModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        connectionModeSpinner.setAdapter(connectionModeAdapter);

        loadSettings();

        addListeners();

    }

    private void addListeners() {
        wakeLockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        sshCompressSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        udpSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());
        dnsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettings());

        tlsVersionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                saveSettings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        connectionModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                saveSettings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        usernameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                saveSettings();
            }
        });

        passwordEditText.addTextChangedListener(new SimpleTextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                saveSettings();
            }
        });

        udpResolverEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                saveSettings();
            }
        });

        payloadEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                saveSettings();
            }
        });
        dns1Text.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                saveSettings();
            }
        });
        dns2Text.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                saveSettings();
            }
        });
        tlsSniText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                saveSettings();
            }
        });
        proxyIp.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                saveSettings();
            }
        });
        proxyPort.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                saveSettings();
            }
        });
    }

    private void updateUIState(boolean isRunning) {
        usernameEditText.setEnabled(!isRunning);
        passwordEditText.setEnabled(!isRunning);
        udpResolverEditText.setEnabled(!isRunning);
        payloadEditText.setEnabled(!isRunning);

        dns1Text.setEnabled(!isRunning);
        dns2Text.setEnabled(!isRunning);
        tlsSniText.setEnabled(!isRunning);
        proxyIp.setEnabled(!isRunning);
        proxyPort.setEnabled(!isRunning);

        wakeLockSwitch.setEnabled(!isRunning);
        sshCompressSwitch.setEnabled(!isRunning);
        udpSwitch.setEnabled(!isRunning);
        dnsSwitch.setEnabled(!isRunning);

        tlsVersionSpinner.setEnabled(!isRunning);
        connectionModeSpinner.setEnabled(!isRunning);

        String connectionMode = connectionModeSpinner.getSelectedItem().toString();
        if ("HTTPS".equals(connectionMode) || "HTTPS_PROXY".equals(connectionMode)) {
            tlsSniText.setVisibility(View.VISIBLE);
            tlsVersionLabel.setVisibility(View.VISIBLE);
            tlsVersionInputLayout.setVisibility(View.VISIBLE);
        } else {
            tlsSniText.setVisibility(View.GONE);
            tlsVersionLabel.setVisibility(View.GONE);
            tlsVersionInputLayout.setVisibility(View.GONE);
        }

        if ("HTTPS".equals(connectionMode)) {
            payloadEditText.setVisibility(View.GONE);

        }

        if (dnsSwitch.isChecked()) {
            dns1Text.setVisibility(View.VISIBLE);
            dns2Text.setVisibility(View.VISIBLE);
        } else {
            dns1Text.setVisibility(View.GONE);
            dns2Text.setVisibility(View.GONE);
        }

        if (udpSwitch.isChecked()) {
            udpResolverEditText.setVisibility(View.VISIBLE);
        } else {
            udpResolverEditText.setVisibility(View.GONE);
        }
    }


    private void saveSettings() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String udpResolver = udpResolverEditText.getText().toString().trim();
        String tlsVersion = tlsVersionSpinner.getSelectedItem().toString();
        String payload = payloadEditText.getText().toString().trim();
        String connectionMode = connectionModeSpinner.getSelectedItem().toString();
        String tls_sni =  tlsSniText.getText().toString().trim();
        String dns1 = dns1Text.getText().toString().trim();
        String dns2 = dns2Text.getText().toString().trim();
        String proxyip = proxyIp.getText().toString().trim();
        String proxyport = proxyPort.getText().toString().trim();

        if (!proxyport.isEmpty() && proxyport.matches("\\d+")) {
            AppPrefs.setProxyPort(Integer.parseInt(proxyport));
        }

        boolean wakeLockEnabled = wakeLockSwitch.isChecked();
        boolean sshCompressEnabled = sshCompressSwitch.isChecked();
        boolean udpEnabled = udpSwitch.isChecked();
        boolean dnsEnabled = dnsSwitch.isChecked();

        AppPrefs.setUsername(username);
        AppPrefs.setPassword(password);
        AppPrefs.setUDPResolver(udpResolver);
        AppPrefs.setTLSVersion(tlsVersion);
        AppPrefs.setPayloadKey(payload);
        AppPrefs.setConnectionMode(connectionMode);
        AppPrefs.setEnableWakeLock(wakeLockEnabled);
        AppPrefs.setEnableSSHCompress(sshCompressEnabled);
        AppPrefs.setEnableUDP(udpEnabled);
        AppPrefs.setEnableCustomDNS(dnsEnabled);
        AppPrefs.setSNI(tls_sni);
        AppPrefs.setDNS1(dns1);
        AppPrefs.setDNS2(dns2);
        AppPrefs.setProxyIP(proxyip);

        updateUIState(ConnectVPN.IS_SERVICE_RUNNING);
    }


    private void loadSettings() {
        String username = AppPrefs.getUsername();
        String password = AppPrefs.getPassword();
        String udpResolver = AppPrefs.getUDPResolver();
        String tlsVersion = AppPrefs.getTLSVersion();
        String connectionMode = AppPrefs.getConnectionMode();
        String payload = AppPrefs.getPayloadKey();

        String sni = AppPrefs.getSNI();
        String dns1 = AppPrefs.getDNS1();
        String dns2 = AppPrefs.getDNS2();
        String proxyip = AppPrefs.getProxyIP();
        int proxyport = AppPrefs.getProxyPort();

        boolean wakeLockEnabled = AppPrefs.isEnableWakeLock();
        boolean sshCompressEnabled = AppPrefs.isEnableSSHCompress();
        boolean udpEnabled =  AppPrefs.isEnableUDP();
        boolean dnsEnabled = AppPrefs.isEnableCustomDNS();

        usernameEditText.setText(username);
        passwordEditText.setText(password);
        udpResolverEditText.setText(udpResolver);
        dns1Text.setText(dns1);
        dns2Text.setText(dns2);
        proxyIp.setText(proxyip);
        proxyPort.setText(String.valueOf(proxyport));
        tlsSniText.setText(sni);

        ArrayAdapter<CharSequence> tlsAdapter = (ArrayAdapter<CharSequence>) tlsVersionSpinner.getAdapter();
        int tlsPosition = tlsAdapter.getPosition(tlsVersion);
        tlsVersionSpinner.setSelection(tlsPosition);

        ArrayAdapter<CharSequence> connectionAdapter = (ArrayAdapter<CharSequence>) connectionModeSpinner.getAdapter();
        int connectionPosition = connectionAdapter.getPosition(connectionMode);
        connectionModeSpinner.setSelection(connectionPosition);

        payloadEditText.setText(payload);
        wakeLockSwitch.setChecked(wakeLockEnabled);
        sshCompressSwitch.setChecked(sshCompressEnabled);
        udpSwitch.setChecked(udpEnabled);
        dnsSwitch.setChecked(dnsEnabled);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!ConnectVPN.IS_SERVICE_RUNNING) {
            start_connection.setText("Start");
        } else {
            saveSettings();
            start_connection.setText("Stop");
        }

        updateUIState(ConnectVPN.IS_SERVICE_RUNNING);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_about:
                showAboutDialog();
                return true;

            case R.id.export_file:
                saveSettings();
                startActivity(new Intent(this, ExportConfig.class));
                return true;

            case R.id.import_file:
                handleImportFile();
                return true;

            case R.id.clearlogs:
                AppLogManager.clearLog();
                return true;

            case R.id.clearconfig:
                handleClearConfig();
                return true;

            case R.id.forcestop:
                android.os.Process.killProcess(android.os.Process.myPid());
                return true;

            case R.id.cleardata:
                handleClearData();
                return true;

            case R.id.exit_app:
                finishAffinity();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void handleImportFile() {
        if (ConnectVPN.IS_SERVICE_RUNNING) {
            showToast(R.string.only_disconnected);
        } else {
            startActivity(new Intent(this, ImportConfig.class));
        }
    }

    private void handleClearConfig() {
        if (ConnectVPN.IS_SERVICE_RUNNING) {
            showToast(R.string.only_disconnected);
        } else {
            showAlert(getString(R.string.reset_app_config_title),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            AppPrefs.resetAppPrefs();
                            loadSettings();
                        }
                    });
        }
    }

    private void handleClearData() {
        if (ConnectVPN.IS_SERVICE_RUNNING) {
            showToast(R.string.only_disconnected);
        } else {
            showAlert(getString(R.string.reset_app_title),
                    getString(R.string.reset_app_txt),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            clearAppData();
                        }
                    });
        }
    }

    private void showToast(int resId) {
        Toast.makeText(this, getString(resId), Toast.LENGTH_SHORT).show();
    }

    private void showAlert(String title, DialogInterface.OnClickListener positiveListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setCancelable(true)
                .setPositiveButton("Ok", positiveListener)
                .setNegativeButton(getString(R.string.cancel), (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showAlert(String title, String message, DialogInterface.OnClickListener positiveListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("Ok", positiveListener)
                .setNegativeButton(getString(R.string.cancel), (dialog, id) -> dialog.cancel());
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void clearAppData() {
        try {
            String packageName = getApplicationContext().getPackageName();
            Runtime runtime = Runtime.getRuntime();
            runtime.exec("pm clear " + packageName);
        } catch (Exception e) {
            showToast(R.string.operation_failed);
            e.printStackTrace();
        }
    }

    private void minimizeApp() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    private void showAboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(this.getString(R.string.app_name));

        String message =
                "App Version: " + BuildConfig.VERSION_NAME + "\n\n" +
                        "JSCH Version: " + com.jcraft.jsch.JSch.VERSION + "\n\n" +
                        "This project is a port of JSCH to Android for VPN connections, with port forwarding support and " +
                        "additional features like payload injection and HTTP/HTTPS proxy support.\n\n" +
                        "The links provided are for the JSCH library, PNDSD DNS proxy, and tun2socks tool, which are " +
                        "used in this project to enable VPN functionality, proxy support, and tunneling capabilities.\n\n" +
                        "Author: Andley302";

        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }



    private int START_VPN_PROFILE = 70;
    @Override
    public void onClick(View v) {
        {
            switch (v.getId()) {
                case R.id.start_connection:
                    if (!ConnectVPN.IS_SERVICE_RUNNING){
                        //verifica
                        if (!validateInputs()){
                            return;
                        }

                        Intent intent = VpnService.prepare(this);

                        if (intent != null) {
                            try {
                                startActivityForResult(intent, START_VPN_PROFILE);
                            } catch (ActivityNotFoundException ane) {

                            }
                        } else {
                            onActivityResult(START_VPN_PROFILE, Activity.RESULT_OK, null);
                        }

                        showLog();
                        start_connection.setText("Stop");
                        updateUIState(true);

                    }else{
                        disconnect_ssh();
                        start_connection.setText("Start");
                        updateUIState(false);
                    }
                    break;

                case R.id.logs:
                    showLog();
                    break;

            }

        }
    }

    private void connect_ssh() {
        try {
            ConnectionInfo ci = new ConnectionInfo(
                    "127.0.0.1",
                    AppPrefs.getUsername(),
                    8084,
                    "",
                    AppPrefs.getPassword(),
                    "",
                    AppPrefs.isEnableSSHCompress(),
                    true,
                    true,
                    AppPrefs.getDynamicPort(),
                    false
            );

            //ci.AddForwards("0");
            ci.Validate();

            //set config and start
            ConnectVPN.ci = ci;
            Intent start_ssh = new Intent(this, ConnectVPN.class);
            startService(start_ssh);
        } catch (Exception e) {
            AppLogManager.addToLog(
                    "<strong><font color=#FF0000>VPN: </strong></font>" +
                            e.toString().split(":")[1]
            );
        }
    }

    private void disconnect_ssh() {
        try{
            Intent stop_ssh = new Intent(this, ConnectVPN.class);
            stopService(stop_ssh);
        } catch (Exception e) {
            AppLogManager.addToLog("VPN error: " + e.toString());
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == START_VPN_PROFILE) {
            if (resultCode == Activity.RESULT_OK) {
                getPermissionAndroid13();
                connect_ssh();
            }
        }

    }

    public void getPermissionAndroid13() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private boolean isValidPort(String portText) {
        try {
            int port = Integer.parseInt(portText);
            return port >= 1 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidIp(String ipText) {
        String ipPattern = "^(\\d{1,3}\\.){3}\\d{1,3}$";
        return ipText.matches(ipPattern);
    }

    private boolean isValidUdpFormat(String udpResolver) {
        String[] parts = udpResolver.split(":");
        if (parts.length != 2) {
            return false;
        }

        String ip = parts[0].trim();
        String port = parts[1].trim();

        if (!isValidIp(ip)) {
            return false;
        }

        try {
            int portNumber = Integer.parseInt(port);
            return portNumber >= 1 && portNumber <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean validateInputs() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username and password cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }

        String proxyIpText = proxyIp.getText().toString().trim();
        String proxyPortText = proxyPort.getText().toString().trim();
        if (proxyIpText.isEmpty() || proxyPortText.isEmpty() || !isValidPort(proxyPortText)) {
            Toast.makeText(this, "Proxy IP and port cannot be empty or invalid", Toast.LENGTH_SHORT).show();
            return false;
        }

        String payload = payloadEditText.getText().toString().trim();
        if (payload.isEmpty()) {
            Toast.makeText(this, "Payload cannot be empty", Toast.LENGTH_SHORT).show();
            return false;
        }

        String connectionMode = connectionModeSpinner.getSelectedItem().toString();
        if ("HTTPS".equals(connectionMode) || "HTTPS_PROXY".equals(connectionMode)) {
            String tlsSni = tlsSniText.getText().toString().trim();
            if (tlsSni.isEmpty()) {
                Toast.makeText(this, "TLS SNI cannot be empty for HTTPS connection", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (udpSwitch.isChecked()) {
            String udpResolver = udpResolverEditText.getText().toString().trim();

            if (udpResolver.isEmpty()) {
                Toast.makeText(this, "UDP resolver cannot be empty when UDP is enabled", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (!isValidUdpFormat(udpResolver)) {
                Toast.makeText(this, "UDP resolver must be in the format ip:port (e.g., 127.0.0.1:7300)", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (dnsSwitch.isChecked()) {
            String dns1 = dns1Text.getText().toString().trim();
            String dns2 = dns2Text.getText().toString().trim();

            if (dns1.isEmpty() || dns2.isEmpty() || !isValidIp(dns1) || !isValidIp(dns2)) {
                Toast.makeText(this, "DNS 1 and DNS 2 must be valid IP addresses when DNS is enabled", Toast.LENGTH_SHORT).show();
                return false;
            }
        }else{
            String udpResolver = udpResolverEditText.getText().toString().trim();

            if (!udpSwitch.isChecked() || !isValidUdpFormat(udpResolver) || udpResolver.isEmpty()){
                dnsSwitch.setChecked(true);
                String dns1 = dns1Text.getText().toString().trim();
                String dns2 = dns2Text.getText().toString().trim();

                if (dns1.isEmpty() || dns2.isEmpty() || !isValidIp(dns1) || !isValidIp(dns2)) {
                    dns1Text.setText("1.1.1.1");
                    dns2Text.setText("1.0.0.1");

                }
            }
        }

        return true;
    }


    private void showLog() {
        if (mDrawer != null && !isFinishing()) {
            DrawerLayout drawerLayout = mDrawer.getDrawerLayout();

            if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        }
    }

    @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {

        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
            if (drawerView.getId() == R.id.activity_mainLogsDrawerLinear) {
                //app_toolbar.getMenu().clear();
                //getMenuInflater().inflate(R.menu.main_menu, app_toolbar.getMenu());
            }

        }

        @Override
        public void onDrawerStateChanged(int newState) {

        }

}