package com.kakao.hulul;

import android.app.Activity;
import android.app.Dialog;
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
    //a WebView object to display a web page
    private WebView webView;
    //The button to launch the WebView dialog
    private Button btLaunchWVD;
    //The button that closes the dialog
    private Button btClose;
    private Dialog webViewDialog;
    private String ssid;
    private String capab;
    private String qrdom;
    private String password;

    String TAG = "GenerateQRCode";
    ImageView qrImage;
    Button start, save;
    String inputValue;
    Bitmap bitmap;
    QRGEncoder qrgEncoder;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Create a new dialog
        webViewDialog = new Dialog(WifiQrActivity.this);
        //Remove the dialog's title
        webViewDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //Inflate the contents of this dialog with the Views defined at 'webviewdialog.xml'
        webViewDialog.setContentView(R.layout.activity_wifiqr);
        //With this line, the dialog can be dismissed by pressing the back key
        webViewDialog.setCancelable(true);

        webViewDialog.show();

        //Initialize the Button object with the data from the 'webviewdialog.xml' file
        Button btClose = (Button) webViewDialog.findViewById(R.id.bt_close);
        //Define what should happen when the close button is pressed.
        btClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Dismiss the dialog
                webViewDialog.dismiss();
                finish();
            }
        });
        qrImage = (ImageView) webViewDialog.findViewById(R.id.imageView);
        inputValue = "WIFI:S:cafe_DRINKY;T:WPA;P:drinky1213;;";
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
        } else {

        }
    }
}