package com.VCP_191202_Final;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;


public class WarningPopUp extends Activity {
    TextView Warning;
    private Vibrator vibrator;

    ///notification bar 띄우는 기능을 위한 class 선언
    NotificationManager notificationManager;
    PendingIntent intent;

    ///Wake lock 기능 class 선언
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyApplication Device_data = (MyApplication) getApplication();
        String Device_data_st = Device_data.getGlobalValue_data();

        // 타이틀바 없애기
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_warning_pop_up);

        // UI 객체 생성
        Warning = (TextView) findViewById(R.id.Warning);

        // 문구 나타내기
        Warning.setText("차량 온도가 너무 높습니다!");

        //휴대폰이 Sleep일 경우 화면을 켜는 동작
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Warning pop 실행 시 vibration 기능
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(1000); // 1초간 진동



        ///Notification Bar 띄우는 작업 실행
        intent = PendingIntent.getActivity(this, 0,
                //Notification Bar 클릭 시 Device_Main으로 이동
                new Intent(getApplicationContext(), com.VCP_191202_Final.Device_Main.class), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this)


                .setSmallIcon(R.drawable.ic_launcher) // 아이콘 설정하지 않으면 오류남
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentTitle("온도가 너무 높습니다") // 제목 설정
                .setContentText("차량온도 : " + Device_data_st) // 내용 설정
                .setTicker("한줄 출력") // 상태바에 표시될 한줄 출력
                .setAutoCancel(true)
                .setContentIntent(intent);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0, builder.build());
    }


    // '확인' 버튼 클릭, 버튼을 클릭하면 1분 안에는 WarningPopUp이 다시 뜨지 않음, 개발 중에는 편의를 위한 3초 딜레이 지정
    public void OK_Button(View view){
        // DelayTime을 위한 전역변수 설정
        MyApplication S_Time_1min = (MyApplication)getApplication();
        MyApplication OK_1min = (MyApplication)getApplication();
        MyApplication OK_10min = (MyApplication)getApplication();

        // 버튼을 누르자 마자 DelayTime을 위한 시스템 시간을 기록
        long StartTime_1min_value = SystemClock.elapsedRealtime();
        S_Time_1min.setGlobalTime_1(StartTime_1min_value);

        // '10분간 대기' 클릭 때와 DelayTime을 다르게 하기위한 전역변수값 지정
        OK_1min.setGlobalValue_1("1min_set_on");
        OK_10min.setGlobalValue_10("10min_set_off");

        // 팝업창을 닫음
        finish();
    }

    // 10분간 대기 버튼 클릭, 버튼을 클릭하면 10분 안에는 WarningPopUp이 다시 뜨지 않음, 개발 중에는 편의를 위한 7초 딜레이 지정
    public void OK_Button_10(View view){
        // DelayTime을 위한 전역변수 설정
        MyApplication S_Time_10min = (MyApplication)getApplication();
        MyApplication OK_1min = (MyApplication)getApplication();
        MyApplication OK_10min = (MyApplication)getApplication();

        // 버튼을 누르자 마자 DelayTime을 위한 시스템 시간을 기록
        long StartTime_10min_value = SystemClock.elapsedRealtime();
        S_Time_10min.setGlobalTime_10(StartTime_10min_value);

        // '확인' 버튼 클릭 때와 DelayTime을 다르게 하기위한 전역변수값 지정
        OK_1min.setGlobalValue_1("1min_set_off");
        OK_10min.setGlobalValue_10("10min_set_on");

        // 팝업창을 닫음
        finish();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        //바깥 레이어를 클릭시 닫히지 않게
        return event.getAction() != MotionEvent.ACTION_OUTSIDE;
    }

    @Override
    public void onBackPressed(){
        //안드로이드 백버튼 막기
    }
}