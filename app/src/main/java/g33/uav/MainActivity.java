package g33.uav;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public byte[] data=new byte[34];           //定义通信数组
    public OutputStream out;                   //定义输出流

    public int x34=0;       //油门初始值
    public int x56=1500;    //航向值 中值：1500
    public int x78=1500;    //横滚值 中值：1500
    public int x910=1870;   //俯仰值：测试平稳是1870

    /**无人机连接状态*/
    public boolean isOnline=false;
    public boolean isRock=false;

    /**无人机操控控制*/
    private SeekBar throttleControl;    //油门控制
    private TextView tvshowThrottleContorlData;
    private Button btnConnect;
    private TextView tvshowHangXiang;
    private TextView tvshowHengGun;
    private TextView tvshowFuYang;
    private TextView tvshowYouMen;
    private RockerView roker;

    /**摇杆是否复位*/
    public boolean isFinish_yaogan=true;

    /**自定义样式*/
    private CircleProgressView circle_progress_left,circle_progress_center,circle_progress_right;
    private TextView tv_progress_left,tv_progress_center,tv_progress_right;


    /**界面控件初始化*/
    public void initElements(){
        btnConnect=(Button)findViewById(R.id.btn_connect);
        throttleControl = (SeekBar) findViewById(R.id.seekBar);
        throttleControl.setMax(980);
        throttleControl.setEnabled(false);
        tvshowThrottleContorlData=(TextView)findViewById(R.id.tv_showThrottleContorlData);
        tvshowYouMen=(TextView)findViewById(R.id.textViewYouMen);
        tvshowYouMen.setText("油门值："+x34);

        tvshowHangXiang=(TextView)findViewById(R.id.textViewHangXiang);
        tvshowHangXiang.setText("航向值："+x56);

        tvshowHengGun=(TextView)findViewById(R.id.textViewHengGun);
        tvshowHengGun.setText("横滚值："+x78);

        tvshowFuYang=(TextView)findViewById(R.id.textViewFuYang);
        tvshowFuYang.setText("俯仰值："+x910);
        roker=(RockerView) findViewById(R.id.yaogan);
        initRokerview();
        initCircleProgress();
//        Thread t=new Thread(new ThreadTrokerview());
//        t.start();
    }

    public void initCircleProgress(){
        circle_progress_left = (CircleProgressView) findViewById(R.id.circle_progress_left);
        circle_progress_center = (CircleProgressView) findViewById(R.id.circle_progress_center);
        circle_progress_right = (CircleProgressView) findViewById(R.id.circle_progress_right);
        tv_progress_left = (TextView) findViewById(R.id.tv_progress_left);
        tv_progress_center = (TextView) findViewById(R.id.tv_progress_center);
        tv_progress_right = (TextView) findViewById(R.id.tv_progress_right);



        //监听进度条进度
//        circle_progress_left.setOnAnimProgressListener(new CircleProgressView.OnAnimProgressListener() {
//            @Override
//            public void valueUpdate(int progress) {
//                tv_progress_left.setText(String.valueOf(progress));
//            }
//        });
//        circle_progress_center.setOnAnimProgressListener(new CircleProgressView.OnAnimProgressListener() {
//            @Override
//            public void valueUpdate(int progress) {
//                tv_progress_center.setText(String.valueOf(progress));
//            }
//        });
//        circle_progress_right.setOnAnimProgressListener(new CircleProgressView.OnAnimProgressListener() {
//            @Override
//            public void valueUpdate(int progress) {
//                tv_progress_right.setText(String.valueOf(progress));
//            }
//        });
    }

    public void initData(){
        data[0]=(byte) 0xAA;   //协议固定数据
        data[1]=(byte) 0xC0;   //协议固定数据
        data[2]=(byte) 0x1C;   //协议固定数据
        //设置油门值，控制上下方向
        data[3]=(byte) (x34>>8);  //设置油门的高八位
        data[4]=(byte) (x34&0xff);//设置油门的低八位

        data[5]=(byte) (x56>>8);  //航向高八位
        data[6]=(byte) (x56&0xff);//航向低八位

        data[7]=(byte) (x78>>8);  //横滚高八位
        data[8]=(byte) (x78&0xff);//横滚低八位

        data[9]=(byte) (x910>>8);  //俯仰高八位
        data[10]=(byte) (x910&0xff);//俯仰低八位

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

    public  void getPermissions(){
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

        initData(); //初始化数据
        getPermissions();
        initElements();

        WebView webView=(WebView)findViewById(R.id.top_center_web);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        //确保跳转到另一个网页时仍然在当前WebView显示
        webView.loadUrl("http://www.baidu.com");

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
                //isOnline=socket.isConnected();
                //3.4 通过socket输出流发送数据
                out=socket.getOutputStream();//获得输出流
            } catch (IOException e) {                //连接失败
                e.printStackTrace();
            }
        }
    }

    /**基础弹窗提示*/
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

    public void showLoading(){

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
            btnConnect.setText("连接");
//            showMessage("提示","连接已断开！");
            Toast.makeText(MainActivity.this,"连接已断开!",Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(MainActivity.this,"正在连接无人机,请稍等...",Toast.LENGTH_LONG).show();
            //第四步：启动线程
            Thread t=new Thread(new MyThread());//指定要启动的线程
            t.start();                          //启动
//            try {
//                t.join();
                if(isOnline){
                    btnConnect.setText("断开");
                    Toast.makeText(MainActivity.this,"无人机连接成功！",Toast.LENGTH_SHORT).show();
//                    showMessage("提示","连接成功!");
                }
                else {
                    Toast.makeText(MainActivity.this,"无人机连接失败！",Toast.LENGTH_SHORT).show();
//                    showMessage("提示","连接失败!");
                }
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//                showMessage("提示","线程join执行异常："+e.getMessage());
//            }
        }
    }

    /**断开连接*/
    public void btnUnConnect(){
        btnConnect.setText("连接");
    }

    /**
     * 启动
     * */
    public void btnRock(View view){
        if(!isOnline){
            Toast.makeText(MainActivity.this,"请先连接无人机！",Toast.LENGTH_SHORT).show();
//            showMessage("提示","请先连接！");
        }else{
            if(isRock){
                isRock=false;
                btnStop();
                Toast.makeText(MainActivity.this,"已停止！",Toast.LENGTH_SHORT).show();
//                showMessage("提示","已经停止！");
            }else{
                    isRock=true;
                    Thread t=new Thread(new SendThread());
                    t.start();
                    throttleControl.setEnabled(true);
                    seekBar_throttleControl();
//                    showMessage("提示","启动成功！");
                    Toast.makeText(MainActivity.this,"启动成功!",Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**停止*/
    public void btnStop(){

//        Button btnC=(Button)findViewById(R.id.btn_connect);
//        btnC.setText("恢复");
    }



    /**
     * 按钮 油门控制
     * @param flag ：true=加油门，false=减油门
     * */
    public void btn_throttleContorl(boolean flag){
        if (isRock)
        {
          if(flag){ //加油门
              if(x34<=980){
                  x34+=20;
                  data[3]=(byte) (x34>>8);  //设置油门的高八位
                  data[4]=(byte) (x34&0xff);//设置油门的低八位
              }else{
                  Toast.makeText(MainActivity.this,"油门已经加到最大！",Toast.LENGTH_SHORT).show();
//                  showMessage("提示","油门已经加到最大!");
              }
                //实时显示数据
              tvshowThrottleContorlData.setText("油门值："+x34);
              tvshowYouMen.setText("油门值："+x34);

          }else{ //减油门
              if(x34>=20){
                  x34-=20;
                  data[3]=(byte) (x34>>8);  //设置油门的高八位
                  data[4]=(byte) (x34&0xff);//设置油门的低八位
              }else{
                  Toast.makeText(MainActivity.this,"无人机已经熄火了！",Toast.LENGTH_SHORT).show();
//                  showMessage("油门提示","无人机已经熄火了!");
              }
              //实时显示数据
              tvshowThrottleContorlData.setText("油门值："+x34);
              tvshowYouMen.setText("油门值："+x34);
          }
          throttleControl.setProgress(x34);
        }
    }

    /**按钮 油门加速*/
    public void btn_throttleContorl_add(View view){
        btn_throttleContorl(true);
    }

    /**按钮 油门减速*/
    public void btn_throttleContorl_reduce(View view){
        btn_throttleContorl(false);
    }

    /**拖动控制油门*/
    public void seekBar_throttleControl(){
        if(isRock) {
            throttleControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                //拖动条改变时
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    x34=progress;
                    data[3] = (byte) (x34 >> 8); // 设置油门的高八位
                    data[4] = (byte) (x34 & 0xff);// 设置油门的低八位
                    //实时显示数据
                    tvshowThrottleContorlData.setText("油门值："+x34);
                    tvshowYouMen.setText("油门值："+x34);
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


    /**向前飞*/
    public void btn_qian(View view){
        if(x910<2800){
            x910+=10;
            data[9]=(byte) (x910>>8);  //俯仰高八位
            data[10]=(byte) (x910&0xff);//俯仰低八位
            //TODO：TextView 显示
            tvshowFuYang.setText("俯仰值："+x910);
        }
    }

    /**向后飞*/
    public void btn_hou(View view){
        if(x910>50){
            x910-=10;
            data[9]=(byte) (x910>>8);  //俯仰高八位
            data[10]=(byte) (x910&0xff);//俯仰低八位
            //TODO：TextView 显示
            tvshowFuYang.setText("俯仰值："+x910);
        }
    }

    /**左旋转*/
    public void btn_leftRotate(View view){
        if(x56<=2800){
            x56+=10;
            data[5]=(byte) (x56>>8);  //航向高八位
            data[6]=(byte) (x56&0xff);//航向低八位
        }else{
            Toast.makeText(MainActivity.this,"您的飞机要逆时针自转了呀~",Toast.LENGTH_SHORT).show();
        }
        tvshowHangXiang.setText(x56);
    }

    /**右旋转*/
    public void btn_rightRotate(View view){
        if(x56>=200){
            x56-=10;
            data[5]=(byte) (x56>>8);  //航向高八位
            data[6]=(byte) (x56&0xff);//航向低八位
        }else{
            Toast.makeText(MainActivity.this,"您的飞机要顺时针自转了呀~",Toast.LENGTH_SHORT).show();
        }
        tvshowHangXiang.setText(x56);
    }


    /**摇杆*/
    public void initRokerview(){
        //找到RockerView控件
//        RockerView roker=(RockerView) findViewById(R.id.yaogan);
        //实时监测摇动方向
        roker.setOnShakeListener(RockerView.DirectionMode.DIRECTION_8, new RockerView.OnShakeListener() {
            //开始摇动时要执行的代码写在本方法里
            @Override
            public void onStart() {

            }
            //结束摇动时要执行的代码写在本方法里
            @Override
            public void onFinish() {
                //Toast.makeText(MainActivity.this, "已复位", Toast.LENGTH_SHORT).show();
            }
            //摇动方向时要执行的代码写在本方法里
            @Override
            public void direction(RockerView.Direction direction) {
                if (direction == RockerView.Direction.DIRECTION_CENTER){
//                    tv.setText("中心");
                    leftFly();
                }else if (direction == RockerView.Direction.DIRECTION_DOWN){
//                    tv.setText("下");
                    leftFly();
                    System.out.printf("下！~~~~~");
                    Log.i("flag", "向下！！！！！！！！！！！");
                }else if (direction == RockerView.Direction.DIRECTION_LEFT){
//                    tv.setText("左");
                    leftFly();
                }else if (direction == RockerView.Direction.DIRECTION_UP){
//                    tv.setText("上");
                    leftFly();
                }else if (direction == RockerView.Direction.DIRECTION_RIGHT){
//                    tv.setText("右");
                    leftFly();
                }else if (direction == RockerView.Direction.DIRECTION_DOWN_LEFT){
//                    tv.setText("左下");
                    leftFly();
                }else if (direction == RockerView.Direction.DIRECTION_DOWN_RIGHT){
//                    tv.setText("右下");
                    leftFly();
                }else if (direction == RockerView.Direction.DIRECTION_UP_LEFT){
//                    tv.setText("左上");
                    leftFly();
                }else if (direction == RockerView.Direction.DIRECTION_UP_RIGHT){
//                    tv.setText("右上");
                    leftFly();
                }
            }
        });
    }

    public class ThreadTrokerview implements Runnable{

        @Override
        public void run() {
            initRokerview();
        }
    }

    public void  leftFly(){
        if(x78<=2800){
            x78+=1;
            data[7]=(byte) (x78>>8);  //横滚高八位
            data[8]=(byte) (x78&0xff);//横滚低八位
        }
        else{
            Toast.makeText(MainActivity.this,"您的飞机快要旋转了呀~",Toast.LENGTH_SHORT).show();
        }
        tvshowHengGun.setText("横滚值："+x78);
    }
    public void  rightFly(){
        if(x78>200) {
            x78-=5;
            data[7]=(byte) (x78>>8);  //横滚高八位
            data[8]=(byte) (x78&0xff);//横滚低八位

        }else{
            Toast.makeText(MainActivity.this,"您的飞机快要旋转了呀~",Toast.LENGTH_SHORT).show();
        }
        tvshowHengGun.setText("横滚值："+x78);
    }
    //设置界面：

    /**设置界面按钮*/
    public  void btnSettings(View view){
        fadeToggleSetting();
        settingProgress();
    }
    public void settingProgress(){
        //开锁执行动画效果
        circle_progress_left.startAnimProgress(x78, 0);
        circle_progress_center.startAnimProgress(x56, 0);
        circle_progress_right.startAnimProgress(x910, 0);
        tv_progress_left.setText(String.valueOf(x78));
        tv_progress_center.setText(String.valueOf(x56));
        tv_progress_right.setText(String.valueOf(x910));

    }
    /**取消按钮单击事件*/
    public  void btnCancel(View view){
        fadeToggleSetting();
    }

    /**设置界面显示与隐藏*/
    public void fadeToggleSetting(){
        ConstraintLayout cl=(ConstraintLayout)findViewById(R.id.panelSettings);
        if(cl.getVisibility()==View.GONE){
            cl.setVisibility(View.VISIBLE);
        }else{
            cl.setVisibility(View.GONE);
        }
    }

    public void addRockValue(View view){
        setRockValue(true);
    }

    public  void subtractRockValue(View view){
        setRockValue(false);
    }

    public void setRockValue(boolean isAdd){
        if(isAdd){
        if(x78<=2800){
            x78+=1;
            data[7]=(byte) (x78>>8);  //横滚高八位
            data[8]=(byte) (x78&0xff);//横滚低八位
            circle_progress_left.startAnimProgress(x78, 0);
            tv_progress_left.setText(String.valueOf(x78));
        }
        else {
            Toast.makeText(MainActivity.this, "您的飞机快要旋转了呀~", Toast.LENGTH_SHORT).show();
        }
        }
        else{
            if(x78>200) {
                x78-=1;
                data[7]=(byte) (x78>>8);  //横滚高八位
                data[8]=(byte) (x78&0xff);//横滚低八位
                circle_progress_left.startAnimProgress(x78, 0);
                tv_progress_left.setText(String.valueOf(x78));
            }else{
                Toast.makeText(MainActivity.this,"您的飞机快要旋转了呀~",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public  void setPitchValue(boolean isAdd){
        if(isAdd){
            if(x910<2800){
                x910+=10;
                tvshowFuYang.setText("俯仰值："+x910);
            }
        }else{
            if(x910>50){
                x910-=10;
                data[9]=(byte) (x910>>8);  //俯仰高八位
                data[10]=(byte) (x910&0xff);//俯仰低八位
                tvshowFuYang.setText("俯仰值："+x910);
            }
        }

    }


}
