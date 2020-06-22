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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.net.wifi.WifiConfiguration.KeyMgmt;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 0;
    private boolean isFocused = true;
    private WifiManager wifiManager;
    private WifiDialog mDialog;
    private List<ScanResult> scanDatas;

    private List<WifiData> wifiList;
    private ListView listView;
    private Context context;
    private ProgressBar bar;
    private TextView tv;
    private Button btn;

    public static final int SECURITY_NONE = 0, SECURITY_WEP = 1, SECURITY_PSK = 2, SECURITY_EAP = 3;

    private IntentFilter f1;
    private IntentFilter f2;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                scanDatas = wifiManager.getScanResults();

                wifiList = new ArrayList<>();
                for (ScanResult select : scanDatas) {
                    String BBSID = select.BSSID + " 강도 " + WifiManager.calculateSignalLevel(select.level, 5) + " 보안 " + getScanResultSecurity(select);
                    String SSID = select.SSID;
                    String CAP = select.capabilities;
                    if (select.SSID.equals(""))
                        continue;
                    WifiData wifiData = new WifiData(select, BBSID, SSID, CAP, false);
                    wifiList.add(wifiData);
                }
                if (scanDatas.size() == 0)
                    Toast.makeText(context, "주변 와이파이를 감지할 수 없습니다.", Toast.LENGTH_SHORT).show();

                listView = (ListView) findViewById(R.id.listView);
                ArrayAdapter adapter = new WifiAdapter(getApplicationContext(), R.layout.item_layout, wifiList);
                listView.setAdapter(adapter);
                final ListView listView = (ListView) findViewById(R.id.listView);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        String result = getScanResultSecurity(wifiList.get(i).getResult());
                        String splitBssid = wifiList.get(i).getBSSID().substring(0, 17);
                        if (result.equalsIgnoreCase("PSK") || result.equalsIgnoreCase("EAP") || result.equalsIgnoreCase("WEP")) {
                            mDialog = new WifiDialog(MainActivity.this, result, wifiList.get(i).getSSID(), splitBssid);
                            mDialog.show();
                        } else if (result.equalsIgnoreCase("OPEN")) {
                            OPEN(wifiManager, wifiList.get(i).getSSID());
                            boolean isRegistered = isRegisteredWifi(splitBssid, true);
                            if (!isRegistered)
                                new DatabaseTask().execute(Integer.toString(DatabaseTask.SET), splitBssid, "", "OPEN");
                            Toast.makeText(MainActivity.this, "연결되었습니다.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                adapter.notifyDataSetChanged();
                bar.setVisibility(View.INVISIBLE);

            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
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
                WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo currentWifi = manager.getConnectionInfo();
                Intent qrIntent = new Intent(MainActivity.this, WifiQrActivity.class);
                qrIntent.putExtra("SSID", currentWifi.getSSID());
                qrIntent.putExtra("PSK", getCurrentWifiPsk(currentWifi.getBSSID()));
                qrIntent.putExtra("CAPAB", getCurrentWifiCap(currentWifi.getBSSID()));
                startActivityForResult(qrIntent, 202);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);
        if (!mReceiver.isOrderedBroadcast())
            registerWifiReceivers();
        bar.setVisibility(View.VISIBLE);
        wifiManager.startScan();
        listView.setFocusable(true);
        getConnectedWifi();

        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo currentWifi = manager.getConnectionInfo();
        if(haveNetworkConnection()) {
            if (isRegisteredWifi(currentWifi.getBSSID(), false))
                btn.setEnabled(true);
            else
                btn.setEnabled(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public void showWifiList(View v) {
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
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

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

    public void registerWifiReceivers() {
        f1 = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        f2 = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        this.registerReceiver(mReceiver, f1);
        this.registerReceiver(mReceiver, f2);
    }

    final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println("BroadcastReceiver: " + action);

            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                getConnectedWifi();
                System.out.println("handling event: WifiManager.NETWORK_STATE_CHANGED_ACTION action: " + action);
                handleWifiStateChange(intent);
            } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                System.out.println("ignoring event: WifiManager.WIFI_STATE_CHANGED_ACTION action: " + action);
            }
        }
    };

    protected void handleWifiStateChange(Intent intent) {
        NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (info.getState().equals(NetworkInfo.State.CONNECTED) && !isFocused) {
            wifiManager.disconnect();
            DisconnectWifi discon = new DisconnectWifi();
            registerReceiver(discon, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
            Toast.makeText(context, "어플을 통해 와이파이를 연결해주세요.", Toast.LENGTH_SHORT).show();
            Intent inteent = new Intent(MainActivity.this, MainActivity.class);
            inteent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(inteent);
            finish();
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
        final String[] securityModes = {"WEP", "PSK", "EAP"};

        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }
        return "OPEN";
    }

    public class DisconnectWifi extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (!intent.getParcelableExtra(wifiManager.EXTRA_NEW_STATE).toString().equals(SupplicantState.SCANNING))
                wifiManager.disconnect();
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
        if (isConnected()) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            String ssid = info.getSSID();
            tv.setText("연결된 와이파이\n" + ssid);
            tv.setEnabled(true);
        } else {
            tv.setText(" ───────────────────");
            tv.setEnabled(false);
        }
    }

    public ArrayList<HashMap<String, String>> getWifiData() {
        try {
            ArrayList<HashMap<String, String>> resultList = new ArrayList<HashMap<String, String>>();
            String TAG_RESULTS = "result";
            String TAG_BSSID = "bssid";
            String TAG_SESSION = "session";
            String TAG_PSK = "psk";
            String TAG_CAP = "cap";
            String a = new DatabaseTask().execute(Integer.toString(DatabaseTask.GET)).get();
            JSONObject jsonObj = new JSONObject(a);
            JSONArray results = jsonObj.getJSONArray(TAG_RESULTS);
            String bsid = null;
            String session = null;
            String psk = null;
            String capa = null;

            for (int i = 0; i < results.length(); i++) {
                JSONObject c = results.getJSONObject(i);
                bsid = c.getString(TAG_BSSID);
                session = c.getString(TAG_SESSION);
                psk = c.getString(TAG_PSK);
                capa = c.getString(TAG_CAP);

                HashMap<String, String> resultMap = new HashMap<String, String>();

                resultMap.put(TAG_BSSID, bsid);
                resultMap.put(TAG_SESSION, session);
                resultMap.put(TAG_PSK, psk);
                resultMap.put(TAG_CAP, capa);

                resultList.add(resultMap);
            }
            return resultList;
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String parseWifiData(String bsid, String dataKey) {
        ArrayList<HashMap<String, String>> resultList = getWifiData();
        for (HashMap<String, String> res : resultList) {
            for (String o : res.keySet()) {
                if(res.values().contains(bsid)) {
                    if (o.equals(dataKey)) {
                        return res.get(o);
                    }
                }
            }
        }
        return null;
    }

    public String getCurrentWifiPsk(String bsid) {
        return parseWifiData(bsid, "psk");
    }

    public String getCurrentWifiCap(String bsid) {
        return parseWifiData(bsid, "cap");
    }

    public boolean isRegisteredWifi(String bsid, boolean isAlready) {
        ArrayList<HashMap<String, String>> resultList = getWifiData();
        for (HashMap<String, String> res : resultList) {
            for (String o : res.keySet()) {
                if (res.get(o).equals(bsid)) {
                    if(isAlready)
                        new DatabaseTask().execute(Integer.toString(DatabaseTask.UPDATE), bsid, "");
                    return true;
                }
            }
        }
        return false;
    }

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
}
