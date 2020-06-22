package com.kakao.hulul;

import android.app.AlertDialog;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class WifiDialog extends AlertDialog implements View.OnClickListener {

    private EditText mEtPasswd;
    private Button mBtnCancel, mBtnConnect;
    private Context mContext;
    private String cap;
    private String ssid;
    private String bssid;

    public WifiDialog(Context context, String cap, String ssid, String bssid) {
        super(context, R.style.Theme_AppCompat_Light_Dialog);
        mContext = context;
        this.cap = cap;
        this.ssid = ssid;
        this.bssid = bssid;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        setCancelable(false);
        setContentView(R.layout.activity_wifi);

        mEtPasswd = (EditText) findViewById(R.id.et_passwd);
        mBtnConnect = (Button) findViewById(R.id.btn_connect);
        mBtnCancel = (Button) findViewById(R.id.btn_cancel);

        mBtnConnect.setOnClickListener(this);
        mBtnCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_cancel:
                this.dismiss();
                break;
            case R.id.btn_connect:
                if (TextUtils.isEmpty(mEtPasswd.getText())) {
                    Toast.makeText(mContext, "비밀번호는 공백일 수 없습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    boolean isRegistered = isAlreadyRegisteredWifi();
                    if(!isRegistered)
                        new DatabaseTask().execute(Integer.toString(DatabaseTask.SET), bssid, mEtPasswd.getText().toString());
                    connectWifi(cap, ssid, mEtPasswd.getText().toString());
                    this.dismiss();
                }
                break;
            default:
                break;

        }
    }

    public void connectWifi(String cap, String ssid, String passkey) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        String networkSSID = ssid;
        String networkPass = passkey;

        List<ScanResult> scanResultList = wifiManager.getScanResults();

        for (ScanResult result : scanResultList) {
            if (result.SSID.equals(networkSSID)) {
                String securityMode = cap;

                if (securityMode.equalsIgnoreCase("WEP")) {
                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.wepKeys[0] = "\"" + networkPass + "\"";
                    wifiConfiguration.wepTxKeyIndex = 0;
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                    int res = wifiManager.addNetwork(wifiConfiguration);
                    boolean b = wifiManager.enableNetwork(res, true);
                    wifiManager.setWifiEnabled(true);
                }

                wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                wifiConfiguration.preSharedKey = "\"" + networkPass + "\"";
                wifiConfiguration.hiddenSSID = true;
                wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

                int res = wifiManager.addNetwork(wifiConfiguration);
                wifiManager.enableNetwork(res, true);

                boolean changeHappen = wifiManager.saveConfiguration();

                if (res != -1 && changeHappen) {

                } else {
                    //Log.d(TAG, "*** Change NOT happen");
                }

                wifiManager.setWifiEnabled(true);
                Toast.makeText(mContext, "연결되었습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public boolean isAlreadyRegisteredWifi() {
        try {
            ArrayList<HashMap<String, String>> resultList = new ArrayList<HashMap<String, String>>();
            String TAG_RESULTS = "result";
            String TAG_BSSID = "bssid";
            String TAG_SESSION = "session";
            String TAG_PSK = "psk";
            String a = new DatabaseTask().execute(Integer.toString(DatabaseTask.GET)).get();
            System.out.println("aaa" + a);
            JSONObject jsonObj = new JSONObject(a);
            JSONArray results = jsonObj.getJSONArray(TAG_RESULTS);
            String bsid = null;
            String psk = null;

            for (int i = 0; i < results.length(); i++) {
                JSONObject c = results.getJSONObject(i);
                bsid = c.getString(TAG_BSSID);
                String session = c.getString(TAG_SESSION);
                psk = c.getString(TAG_PSK);

                HashMap<String, String> resultMap = new HashMap<String, String>();

                resultMap.put(TAG_BSSID, bsid);
                resultMap.put(TAG_SESSION, session);
                resultMap.put(TAG_PSK, psk);

                resultList.add(resultMap);
            }

            for(HashMap<String, String> res : resultList) {
                for(String o: res.keySet()) {
                    System.out.println("res.get(o) : " + res.get(o));
                    if (res.get(o).equals(bssid)) {
                        new DatabaseTask().execute(Integer.toString(DatabaseTask.UPDATE), bssid, mEtPasswd.getText().toString());
                        return true;
                    }
                }
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
}