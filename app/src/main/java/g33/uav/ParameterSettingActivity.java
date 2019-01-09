package g33.uav;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import  g33.uav.CircleProgressView;

public class ParameterSettingActivity extends AppCompatActivity {

    private CircleProgressView circle_progress_left,circle_progress_center,circle_progress_right;
    private TextView tv_progress_left,tv_progress_center,tv_progress_right;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parameter_setting);
        circle_progress_left = (CircleProgressView) findViewById(R.id.circle_progress_left);
        circle_progress_center = (CircleProgressView) findViewById(R.id.circle_progress_center);
        circle_progress_right = (CircleProgressView) findViewById(R.id.circle_progress_right);
        tv_progress_left = (TextView) findViewById(R.id.tv_progress_left);
        tv_progress_center = (TextView) findViewById(R.id.tv_progress_center);
        tv_progress_right = (TextView) findViewById(R.id.tv_progress_right);



        //开锁执行动画效果
        circle_progress_left.startAnimProgress(1500, 0);
        circle_progress_center.startAnimProgress(500, 0);
        circle_progress_right.startAnimProgress(250, 0);
        //监听进度条进度
        circle_progress_left.setOnAnimProgressListener(new CircleProgressView.OnAnimProgressListener() {
            @Override
            public void valueUpdate(int progress) {
                tv_progress_left.setText(String.valueOf(progress));
            }
        });
        circle_progress_center.setOnAnimProgressListener(new CircleProgressView.OnAnimProgressListener() {
            @Override
            public void valueUpdate(int progress) {
                tv_progress_center.setText(String.valueOf(progress));
            }
        });
        circle_progress_right.setOnAnimProgressListener(new CircleProgressView.OnAnimProgressListener() {
            @Override
            public void valueUpdate(int progress) {
                tv_progress_right.setText(String.valueOf(progress));
            }
        });
    }

    public void Back(View view){
        Intent intent =new Intent(ParameterSettingActivity.this,MainActivity.class);
        startActivity(intent);

    }

}
