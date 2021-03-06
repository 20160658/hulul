package com.kakao.hulul;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;

import com.google.zxing.WriterException;

public class WifiQrActivity extends Activity
{
    private Dialog wifiQrDialog;
    private String ssid;
    private String psk;
    private String cap;

    String TAG = "GenerateQRCode";
    ImageView qrImage;
    String inputValue;
    Bitmap bitmap;
    QRGEncoder qrgEncoder;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();

        ssid = intent.getExtras().getString("SSID");
        psk = intent.getExtras().getString("PSK");
        cap = intent.getExtras().getString("CAPAB");
        System.out.println("capab "+ cap);

        //Create a new dialog
        wifiQrDialog = new Dialog(WifiQrActivity.this);
        //Remove the dialog's title
        wifiQrDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Inflate the contents of this dialog with the Views defined at 'webviewdialog.xml'
        wifiQrDialog.setContentView(R.layout.activity_wifiqr);
        //With this line, the dialog can be dismissed by pressing the back key
        wifiQrDialog.setCancelable(true);

        wifiQrDialog.show();

        //Initialize the Button object with the data from the 'webviewdialog.xml' file
        Button btClose = (Button) wifiQrDialog.findViewById(R.id.bt_close);
        //Define what should happen when the close button is pressed.
        btClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Dismiss the dialog
                wifiQrDialog.dismiss();
                finish();
            }
        });
        qrImage = (ImageView) wifiQrDialog.findViewById(R.id.imageView);
        String capab = "FAIL";
        if(cap.equals("OPEN"))
            capab = "nopass";
        else if(cap.equals("PSK"))
            capab = "WPA";
        inputValue = "WIFI:S:" + ssid.substring(1, ssid.length()-1) + ";T:" + capab + ";P:" + psk + ";;";
        System.out.println(inputValue);
        if (inputValue.length() > 0) {
            WindowManager manager = (WindowManager) getSystemService(WINDOW_SERVICE);
            Display display = manager.getDefaultDisplay();
            Point point = new Point();
            display.getSize(point);
            int width = point.x;
            int height = point.y;
            int smallerDimension = width < height ? width : height;
            smallerDimension = smallerDimension * 3 / 4;

            qrgEncoder = new QRGEncoder(
                    inputValue, null,
                    QRGContents.Type.TEXT,
                    smallerDimension);
            try {
                bitmap = qrgEncoder.encodeAsBitmap();
                qrImage.setImageBitmap(bitmap);
            } catch (WriterException e) {
                Log.v(TAG, e.toString());
            }
        }
    }
}