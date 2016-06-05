package com.example.admin.myapplicationahah;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    ProgressBar mProgressBar;
    Button mButton;
    File file;
    private static int down = 0;
    private static final String URL_ADDRESS = "http://s3test.iobit.com.s3.amazonaws.com/lookout.apk";

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    mButton.setText("点击安装！！");
                    down =1;
                    break;
                case 2:
                    mButton.setText("打开");
                    down = 2;
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressBar = (ProgressBar)findViewById(R.id.progressbar);
        mButton = (Button)findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (down == 0){
                    downloadAPK(URL_ADDRESS);
                    mButton.setText("正在下载！！");
                }
            }
        });
    }

    private File downloadAPK(final String httpurl){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(httpurl);
                    HttpURLConnection connection =  (HttpURLConnection)url.openConnection();
                    connection.setConnectTimeout(5000);
                    FileOutputStream fileOutputStream = null;
                    InputStream inputStream;
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK){
                        inputStream = connection.getInputStream();
                        if (inputStream != null){
                            file = getFileStreamPath(httpurl);
                            fileOutputStream = new FileOutputStream(file);
                            byte[] buffer = new byte[1024];
                            int length = 0;
                            while ((length = inputStream.read(buffer))!= -1){
                                fileOutputStream.write(buffer,0,length);
                            }
                            fileOutputStream.close();;
                            fileOutputStream.flush();
                        }
                        inputStream.close();
                    }
                    System.out.println("已下载完成！！");
                    Message message = handler.obtainMessage();
                    message.what = 1;
                    handler.sendMessage(message);
                }catch (MalformedURLException e){
                    e.printStackTrace();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();
        return file;
    }

    private void installAPK(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file),"application/vnd.android.package-archive");
        startActivity(intent);
    }

    private void openAPK(Context context,String url){

    }

    private File getFile(String url){
        File files = new File();
    }

    private String getFilePath(String url){
        return url.substring(url.lastIndexOf("/"),url.length());
    }
}
