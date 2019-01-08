package g33.uav;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public byte[] data=new byte[34];           //定义通信数组
    public OutputStream out;                   //定义输出流

    /**无人机链接状态*/
    public boolean isOnline=false;
    public boolean isRock=false;
    /**无人机操控控制*/
    private SeekBar throttleControl;    //油门控制

    public void initdata(){
        data[0]=(byte) 0xAA;   //协议固定数据
        data[1]=(byte) 0xC0;   //协议固定数据
        data[2]=(byte) 0x1C;   //协议固定数据
        //设置油门值，控制上下方向
        data[3]=(byte) (300>>8);  //设置油门的高八位
        data[4]=(byte) (300&0xff);//设置油门的低八位
        data[31]=(byte) 0x1C;   //协议固定数据
        data[32]=(byte) 0x0D;	//协议固定数据
        data[33]=(byte) 0x0A;	//协议固定数据
    }

    //读写权限 具体权限加在字符串里面
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.BLUETOOTH_ADMIN,
            //            允许程序进行发现和配对新的蓝牙设备
            Manifest.permission.BLUETOOTH,
            //            允许程序连接配对过的蓝牙设备
            Manifest.permission.ACCESS_FINE_LOCATION,
            //            允许程序通过GPS芯片接收卫星的定位信息

            Manifest.permission.ACCESS_COARSE_LOCATION
            //            允许程序通过WiFi或移动基站的方式获取用户错略的经纬度信息
    };

    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 1;

    public  void getPremissions(){
        //判断是否有权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_PERMISSION_CODE);/* 请求权限 */
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_CONTACTS)) {//判断是否需要 向用户解释，为什么要申请该权限
                Toast.makeText(this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initdata(); //初始化数据

        WebView webView=(WebView)findViewById(R.id.top_center_web);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        //确保跳转到另一个网页时仍然在当前WebView显示
        webView.loadUrl("http://www.baidu.com");
        getPremissions();
    }

    public class MyThread implements Runnable{

        @Override
        public void run() {
            Button btnC=(Button)findViewById(R.id.btn_connect);
            //第三步：在线程里实现蓝牙通信
            //3.1  获取蓝牙设备
            BluetoothAdapter adpter=BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device=adpter.getRemoteDevice("00:0E:0E:15:84:EE");  //蓝牙地址
            //BluetoothDevice device=adpter.getRemoteDevice("00:0E:0E:0E:30:F3");  //蓝牙地址
            //BluetoothDevice device=adpter.getRemoteDevice("00:0E:0E:15:84:F5");  //蓝牙地址
            //3.2 连接服务端
            UUID uuid=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

            BluetoothSocket socket= null;
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid);
                //3.3获取socket
                socket.connect();
                isOnline=true;
                //3.4 通过socket输出流发送数据
                out=socket.getOutputStream();//获得输出流
            } catch (IOException e) {                //连接失败
                e.printStackTrace();
                isOnline=false;
            }
        }
    }

    public void showMessage(String title,String content){
        /* @setIcon 设置对话框图标
         * @setTitle 设置对话框标题
         * @setMessage 设置对话框消息提示
         * setXXX方法返回Dialog对象，因此可以链式设置属性
         */
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        normalDialog.setTitle(title);
        normalDialog.setMessage(content);
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do
                    }
                });
        normalDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do
                    }
                });
        // 显示
        normalDialog.show();
    }


    /**发送数据*/
    public class SendThread implements  Runnable{

        @Override
        public void run() {
            try {
                while(isOnline){
                    out.write(data);               //发送通信数组给无人机
                    Thread.sleep(5);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 连接
     * */
    public void btnConnect(View view){
        if( isOnline){
            isOnline=false;
            btnUnConnect();
        }else{
            //第四步：启动线程
            Thread t=new Thread(new MyThread());//指定要启动的线程
            t.start();                          //启动
            if(isOnline)
                showMessage("提示","连接成功,状态码（"+(isOnline)+"）");
            else  showMessage("提示","连接失败,状态码（"+(isOnline)+"）");
        }
    }

    /**断开连接*/
    public void btnUnConnect(){
        Button btnC=(Button)findViewById(R.id.btn_connect);
        btnC.setText("连接");
    }

    /**
     * 加油(飞机连续转动)
     * */
    public void btnRock(View view){
        if(isRock){
            isRock=false;
            btnStop();
        }else{
            isRock=true;
            isOnline=true;
            Thread t=new Thread(new SendThread());
            t.start();
            seekBar_throttleControl();
        }
    }

    /**停止*/
    public void btnStop(){
        isOnline=false;
//        Button btnC=(Button)findViewById(R.id.btn_connect);
//        btnC.setText("恢复");
    }

    /**拖动控制油门*/
    public void seekBar_throttleControl(){
        if(isRock) {
            throttleControl = (SeekBar) findViewById(R.id.seekBar);
            throttleControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                //拖动条改变时
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int speed = progress * 20;
                    data[3] = (byte) (speed >> 8); // 设置油门的高八位
                    data[4] = (byte) (speed & 0xff);// 设置油门的低八位
                }

                //拖动开始
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                //拖动停止
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }
}
