package com.VCP_191202_Arduino;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import static com.VCP_191202_Arduino.Device_Main.mConnected;

public class Device_Boot extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_booting);
        startLoading();
    }

    private void startLoading() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mConnected){ // App과 Device가 연결 되었을 때
                    final Intent intent = new Intent(Device_Boot.this, Device_Main.class);
                    startActivity(intent);
                } else{ // App과 Device가 연결 되지 않았을 때
                    final Intent intent = new Intent(Device_Boot.this, Device_Scan.class);
                    startActivity(intent);
                }
                finish();
            }
        }, 2000);
    }

    @Override
    public void onBackPressed() {
        //초반 플래시 화면에서 넘어갈때 뒤로가기 버튼 못누르게 함
    }
}
