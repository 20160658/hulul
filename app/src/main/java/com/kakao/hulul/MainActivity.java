package com.kakao.hulul;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.net.wifi.WifiConfiguration.KeyMgmt;

import com.kakao.auth.Session;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 0;
    private boolean isFocused = true;
    private WifiManager wifiManager;
    private WifiDialog mDialog;
    private List<ScanResult> scanDatas; // ScanResult List

    private List<WifiData> wifiList;
    private ListView listView;
    private Context context;
    private ProgressBar bar;
    private TextView tv;
    private Button btn;

    public static final int SECURITY_NONE = 0, SECURITY_WEP = 1, SECURITY_PSK = 2, SECURITY_EAP = 3;

    IntentFilter f1;
    IntentFilter f2;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                scanDatas = wifiManager.getScanResults();

                wifiList = new ArrayList<>();
                for(ScanResult select : scanDatas){
                    String BBSID = select.BSSID;
                    String SSID = select.SSID;
                    String CAP = select.capabilities;
                    if(select.SSID.equals(""))
                        continue;
                    WifiData wifiData = new WifiData(select, BBSID, SSID, CAP, false);
                    wifiList.add(wifiData);
                }
                if(scanDatas.size() == 0)
                    Toast.makeText(context, "주변 와이파이를 감지할 수 없습니다.", Toast.LENGTH_SHORT).show();

                // 어댑터뷰(리스트 뷰)
                listView = (ListView)findViewById(R.id.listView);
                // 어댑터
                ArrayAdapter adapter = new WifiAdapter(getApplicationContext(), R.layout.item_layout, wifiList); // 안드로이드에서 기본적으로 제공되는 레이아웃
                listView.setAdapter(adapter);
                final ListView listView = (ListView) findViewById(R.id.listView);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        //Toast.makeText(getApplicationContext(), wifiList.get(i).getSSID(), Toast.LENGTH_SHORT).show();
                        //if(wifiList.get(i).getSSID().equals("2018170056")) {
                        //    Toast.makeText(getApplicationContext(), "123321aaa", Toast.LENGTH_SHORT).show();
                        //}
                        //scanDatas.contains()
                        //Intent intent = new Intent(
                        //        Settings.ACTION_WIFI_SETTINGS);
                        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        //startActivityForResult(intent, 0);
                        String result = getScanResultSecurity(wifiList.get(i).getResult());
                        if(result.equalsIgnoreCase("PSK") || result.equalsIgnoreCase("EAP") || result.equalsIgnoreCase("WEP")) {
                            mDialog = new WifiDialog(MainActivity.this, result, wifiList.get(i).getSSID());
                            mDialog.show();
                            //unRegisterWifiReceivers();
                        } else if(result.equalsIgnoreCase("OPEN")) {
                            OPEN(wifiManager, wifiList.get(i).getSSID());
                            insertWifi(wifiList.get(i).getSSID(), Session.getCurrentSession().toString(), "");
                            Toast.makeText(MainActivity.this, "연결되었습니다.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                // listview 갱신
                adapter.notifyDataSetChanged();
                bar.setVisibility(View.INVISIBLE);

            }else if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                getConnectedWifi();
                sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        context = MainActivity.this;
        checkPermission(context);
        registerWifiReceivers();
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        listView = (ListView) findViewById(R.id.listView);
        bar = (ProgressBar) findViewById(R.id.progressBar3);
        btn = (Button) findViewById(R.id.button);
        tv = (TextView) findViewById(R.id.textView);
        btn = (Button) findViewById(R.id.button);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WifiConfiguration conf = new WifiConfiguration();
                Intent intent = new Intent(MainActivity.this, WifiQrActivity.class);
                intent.putExtra("SSID", conf.SSID);
                intent.putExtra("CAPAB", getSecurityType(conf));
                //intent.putExtra("PSK", "");
                startActivityForResult(intent, 202);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);
        bar.setVisibility(View.VISIBLE);
        wifiManager.startScan();
        listView.setFocusable(true);
        getConnectedWifi();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void showWifiList(View v){
        wifiManager = (WifiManager) getApplicationContext().getSystemService(this.WIFI_SERVICE);
    }

    private boolean checkPermission(Context mContext) {

        List<String> permissionsList = new ArrayList<String>();

        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (permissionsList.size() > 0) {
            ActivityCompat.requestPermissions((Activity) mContext, permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    public boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting() && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        return isConnected;
    }

    public static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK))
            return SECURITY_PSK;

        if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP) || config.allowedKeyManagement.get(KeyMgmt.IEEE8021X))
            return SECURITY_EAP;

        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    public static String getSecurityType(WifiConfiguration config) {
        switch (getSecurity(config)) {
            case SECURITY_WEP:
                return "WEP";
            case SECURITY_PSK:
                if (config.allowedProtocols.get(WifiConfiguration.Protocol.RSN))
                    return "WPA2";
                else
                    return "WPA";
            default:
                return "NONE";
        }
    }

    protected void registerWifiReceivers()
    {
        f1 = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        f2 = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        this.registerReceiver(mReceiver, f1);
        this.registerReceiver(mReceiver, f2);
    }

    protected void unRegisterWifiReceivers()
    {
        unregisterReceiver(mReceiver);
    }

    final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            System.out.println("BroadcastReceiver: " + action );

            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION))
            {
                getConnectedWifi();
                System.out.println("handling event: WifiManager.NETWORK_STATE_CHANGED_ACTION action: "+action );
                handleWifiStateChange(intent);
            }
            else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action))
            {
                System.out.println("ignoring event: WifiManager.WIFI_STATE_CHANGED_ACTION action: "+action );
            }
        }
    };

    protected void handleWifiStateChange ( Intent intent )
    {
        NetworkInfo info = (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (info.getState().equals(NetworkInfo.State.CONNECTED) && !isFocused)
        {
            wifiManager.disconnect();
            DisconnectWifi discon = new DisconnectWifi();
            registerReceiver(discon, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
            Toast.makeText(context, "어플을 통해 와이파이를 연결해주세요.", Toast.LENGTH_SHORT).show();
            Intent inteent = new Intent(MainActivity.this, MainActivity.class);
            inteent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(inteent);
            finish();
            //do something...
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && !hasWindowFocus()) {
            isFocused = false;
        } else {
            isFocused = true;
            getConnectedWifi();
        }
    }

    public String getScanResultSecurity(ScanResult scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = { "WEP", "PSK", "EAP" };

        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }

        return "OPEN";
    }

    public class DisconnectWifi extends BroadcastReceiver  {

        @Override
        public void onReceive(Context c, Intent intent) {
            if(!intent.getParcelableExtra(wifiManager.EXTRA_NEW_STATE).toString().equals(SupplicantState.SCANNING)) wifiManager.disconnect();
        }
    }

    private void OPEN(WifiManager wifiManager, String networkSSID) {
        WifiConfiguration wc = new WifiConfiguration();
        wc.SSID = "\"" + networkSSID + "\"";
        wc.hiddenSSID = true;
        wc.priority = 0xBADBAD;
        wc.status = WifiConfiguration.Status.ENABLED;
        wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        wc.allowedAuthAlgorithms.clear();
        wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        int id = wifiManager.addNetwork(wc);
        wifiManager.enableNetwork(id, true);
        getConnectedWifi();
    }

    public void getConnectedWifi() {
        if(isConnected()) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo ();
            String ssid  = info.getSSID();
            tv.setText("연결된 와이파이\n" + ssid);
            tv.setEnabled(true);
            btn.setEnabled(true);
        } else {
            tv.setText(" ───────────────────");
            tv.setEnabled(false);
            btn.setEnabled(false);
        }
    }

    public String insertData(String urlString, final String bssid, final String session,final String psk) {
        try {
            URL url = new URL(urlString);
            String postData = "bssid=" + bssid + "&" + "session=" + session + "&" + "psk=" + psk;
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(postData.getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();
            String result = readStream(conn.getInputStream());
            conn.disconnect();
            return result;
        }
        catch (Exception e) {
            Log.i("PHPRequest", "request was failed.");
            return null;
        }
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is),1000);
        for (String line = r.readLine(); line != null; line =r.readLine()){
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }

    public void insertWifi(String bssid, String session, String psk) {
        String result = insertData("http://hulu.dothome.co.kr/set.php", bssid, session, psk);
        if (result.equals("1")) {
            Toast.makeText(getApplication(), "들어감", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplication(), "안 들어감", Toast.LENGTH_SHORT).show();
        }
    }

}
