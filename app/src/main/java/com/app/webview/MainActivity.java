package com.app.webview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.content.Intent;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.graphics.Bitmap;
import android.view.View.OnClickListener;
import android.content.Context;

public class MainActivity extends AppCompatActivity {

    private Button btn1;
    private Context context;

    Button playButton ;
    EditText rtspUrl ;
    RadioButton radioStream;
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn1 = (Button) findViewById(R.id.btn1);
        rtspUrl = (EditText)this.findViewById(R.id.url);
        playButton = (Button)this.findViewById(R.id.start_play);
        radioStream = (RadioButton)this.findViewById(R.id.radioButtonStream);

        btn1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //Intent是一种运行时绑定（run-time binding）机制，它能在程序运行过程中连接两个不同的组件。
                Intent i = new Intent(MainActivity.this, Main2Activity.class);
                //启动
                startActivity(i);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
                String fname = "/sdcard/DCIM/Camera" + sdf.format(new Date()) + ".jpg";
                View view = v.getRootView();
                view.setDrawingCacheEnabled(true);
                view.buildDrawingCache();
                Bitmap bitmap = view.getDrawingCache();
                if (bitmap != null) {
                    System.out.println("bitmap got!");
                    try {
                        FileOutputStream out = new FileOutputStream(fname);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        System.out.println("file " + fname + "output done.");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("bitmap is NULL!");
                }
            }

        });



        playButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                if (radioStream.isChecked()) {
                    PlayRtspStream(rtspUrl.getEditableText().toString());
                }
            }
        });

        webView = (WebView)this.findViewById(R.id.rtsp_player);
    }
    private void PlayRtspStream(String rtspUrl){
        webView.loadUrl(rtspUrl);
        webView.requestFocus();


    }

    //play rtsp stream

}
