package com.kakao.hulul;

import android.os.AsyncTask;
import android.util.Log;

import com.kakao.auth.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DatabaseTask extends AsyncTask<String, Void, String> {

    public static int GET = 0;
    public static int SET = 1;
    public static int UPDATE = 2;

    protected String doInBackground(String... params) {
        String result;
        //UPDATE
        if(Integer.parseInt(params[0]) == UPDATE)
            insertData(params[1], params[2]);
        //POST
        else if(Integer.parseInt(params[0]) == SET)
            result = insertData(params[1], Session.getCurrentSession().toString(), params[2]);
        //GET
        else if(Integer.parseInt(params[0]) == GET) {
            String uri = "http://hulu.dothome.co.kr/get.php";

            BufferedReader bufferedReader = null;
            try {
                URL url = new URL(uri);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                StringBuilder sb = new StringBuilder();

                bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

                String json;
                while ((json = bufferedReader.readLine()) != null) {
                    sb.append(json + "\n");
                }
                return sb.toString().trim();

            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    protected void onPostExecute(String result) {
        //result를 처리한다.
    }

    public String connectData(String ... params) {
        String postData = null;
        if(params.length == 3)
            postData = "bssid=" + params[1] + "&" + "psk=" + params[2];
        else if(params.length == 4)
            postData = "bssid=" + params[1] + "&" + "session=" + params[2] + "&" + "psk=" + params[3];
        try {
            URL url = new URL(params[0]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
        } catch (Exception e) {
            Log.i("PHPRequest", "request was failed.");
            return null;
        }
    }

    public String insertData(String ... params) {//String bssid, String session, String psk) {
        String result = null;
        if(params.length == 2)
            connectData("http://hulu.dothome.co.kr/update.php", params[0], params[1]);
        else if(params.length == 3)
            result = connectData("http://hulu.dothome.co.kr/set.php", params[0], params[1], params[2]);
        if (result != null) {
            if (result.equals("1")) {
                //Toast.makeText(MainActivity.this, "들어감", Toast.LENGTH_SHORT).show();
                return "들어감";
            }
        }
        return "안들어감";
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is), 1000);
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }
}
