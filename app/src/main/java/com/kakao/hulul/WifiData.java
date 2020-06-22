package com.kakao.hulul;

import android.net.wifi.ScanResult;

public class WifiData {
    private ScanResult result;
    private String BSSID; // Mac주소(고유번호)
    private String SSID; // 보여줄 이름
    private String CAP;
    private boolean isOn; // 허용or차단

    public WifiData(ScanResult result, String BSSID, String SSID, String CAP, boolean isOn) {
        this.result = result;
        this.BSSID = BSSID;
        this.SSID = SSID;
        this.CAP = CAP;
        this.isOn = isOn;
    }

    public ScanResult getResult() {
        return result;
    }

    public void setResult(ScanResult result) {
        this.result = result;
    }

    public String getBSSID() {
        return BSSID;
    }

    public void setBSSID(String BSSID) {
        this.BSSID = BSSID;
    }

    public String getSSID() {
        return SSID;
    }

    public void setSSID(String SSID) {
        this.SSID = SSID;
    }

    public String getCAP() {
        return CAP;
    }

    public void setCap(String CAP) {
        this.SSID = CAP;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean on) {
        isOn = on;
    }
}