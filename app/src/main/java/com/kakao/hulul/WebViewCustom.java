package com.kakao.hulul;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebSettings;

/**
 * Created by d on 2017. 12. 30..
 * 웹뷰 커스텀
 */

public class WebViewCustom extends android.webkit.WebView{
    public WebViewCustom(Context context) {
        super(context);
        init();
    }

    public WebViewCustom(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WebViewCustom(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public WebViewCustom(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }


    private void init(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //android:hardwareAccelerated="true"
            //평균적으로 킷캣 이상에서는 하드웨어 가속이 성능이 좋음.
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        WebSettings settings = getSettings();

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            //기기에 따라서 동작할수도있는걸 확인
            settings.setRenderPriority(WebSettings.RenderPriority.HIGH);

            //최신 SDK 에서는 Deprecated 이나 아직 성능상에서는 유용하다
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);

            //부드러운 전환 또한 아직 동작
            settings.setEnableSmoothTransition(true);
        }


        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setAppCacheEnabled(true);


        loadUrl("about:blank");
    }

    @Override
    public void destroy() {

        //현재 웹뷰 메모리누수를 막으려면 수동으로 삭제할수밖에...
        ViewParent parent = this.getParent();
        if(parent instanceof ViewGroup){
            ((ViewGroup) parent).removeView(this);
        }

        loadUrl("about:blank");

        try{
            removeAllViews();
        }catch (Exception e){
            e.printStackTrace();
        }

        super.destroy();
    }
}