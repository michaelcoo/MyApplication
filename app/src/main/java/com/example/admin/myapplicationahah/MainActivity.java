package com.example.admin.myapplicationahah;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    ProgressBar mProgressBar;
    TextView mTextView;
    Button mButton;
    File file;
    private static int down = 0;
    private static final String URL_ADDRESS = "http://s3test.iobit.com.s3.amazonaws.com/lookout.apk";
    final public static int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 159;
    private int FileLength;
    private int DownedFileLength;

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
                case 3:
                    mProgressBar.setMax(FileLength);
                    mProgressBar.setProgress(DownedFileLength);
                    int progress = DownedFileLength * 100 / FileLength;
                    mTextView.setText(progress + "%");
                    break;
            }
        }
    };

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("收到aok安装完成的广播");
            Message message = handler.obtainMessage();
            message.what = 2;
            handler.sendMessage(message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressBar = (ProgressBar)findViewById(R.id.progressbar);
        mTextView = (TextView)findViewById(R.id.progressb_text);
        mButton = (Button)findViewById(R.id.button);
        DownedFileLength = 0;
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (down == 0) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        int checkStoragePermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if (checkStoragePermission != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
                        }else {
                            downloadAPK(URL_ADDRESS);
                        }
                    }else {
                        downloadAPK(URL_ADDRESS);
                    }
                    mButton.setText("正在下载！！");
                }else if (down == 1 ){
                    installAPK();
                }else if (down == 2){
                    openAPK(MainActivity.this,URL_ADDRESS);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addDataScheme("package");
        registerReceiver(receiver,intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private File downloadAPK(final String httpurl){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(httpurl);
                    HttpURLConnection connection =  (HttpURLConnection)url.openConnection();
                    connection.setConnectTimeout(5000);
                    FileLength = connection.getContentLength();
                    FileOutputStream fileOutputStream = null;
                    InputStream inputStream;
                    if (connection.getResponseCode() == HttpURLConnection.HTTP_OK){
                        inputStream = connection.getInputStream();
                        if (inputStream != null){
                            file = getFile(httpurl);
                            fileOutputStream = new FileOutputStream(file);
                            byte[] buffer = new byte[1024];
                            int length = 0;
                            while ((length = inputStream.read(buffer))!= -1){
                                DownedFileLength += length;
                                Message message_progressbar = handler.obtainMessage();
                                message_progressbar.what = 3;
                                handler.sendMessage(message_progressbar);
                                fileOutputStream.write(buffer,0,length);
                            }
                            fileOutputStream.close();
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
        PackageManager manager =  context.getPackageManager();
        PackageInfo info = manager.getPackageArchiveInfo(Environment.getExternalStorageDirectory().getAbsolutePath() + getFilePath(url),PackageManager.GET_ACTIVITIES);
        if (info != null){
            Intent intent = manager.getLaunchIntentForPackage(info.applicationInfo.packageName);
            startActivity(intent);
        }
    }

    private File getFile(String url){
        File files = new File(Environment.getExternalStorageDirectory(),getFilePath(url));
        return  files;
    }

    private String getFilePath(String url){
        return url.substring(url.lastIndexOf("/"),url.length());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    downloadAPK(URL_ADDRESS);
                }else {
                    Toast.makeText(MainActivity.this,"WRITE_EXTERNAL_STORAGE",Toast.LENGTH_SHORT);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
