/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.VCP_191202_Final;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import android.widget.Toast;
import static com.VCP_191202_Final.BluetoothLeService.ACTION_DATA_AVAILABLE;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */

/*
 * 받는 데이터의 형식
 * =  00000   +  0  +  00
 * [seat정보]+[+/-]+[온도]
 *
 * seat='0' -->empty
 * seat='1' -->occupied
 *
 * 부호='1' --> 영상
 * 부호='0' --> 영하
 * * */

public class Device_Main extends Activity {

    // --------------------------------------------------------------------
    private final static String TAG = Device_Main.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView Show_alarm_temp; //알람 설정온도
    private TextView Show_AC_temp; //에어컨 설정온도
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    public static boolean mConnected = false;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;
    private Vibrator vibrator;

    Switch MainSwitch; // Alarm ON/OFF Switch
    Switch ACSwitch; // Air Conditioner ON/OFF Switch

    TextView SEAT1, SEAT2, SEAT3, SEAT4, SEAT5; // 각각의 자리에 대한 변수. // 다른 클래스(Seaton.java)에서 사용하기 위해서는 반드시 public static 선언
    String OK_1min_value, OK_10min_value;


    int SEAT1bgColor,SEAT2bgColor,SEAT3bgColor, SEAT4bgColor, SEAT5bgColor; // 좌석 색깔을 위한 변수
    long EndTime, StartTime_10min_value, StartTime_1min_value, DelayTime_1min_value, DelayTime_10min_value; // WarningPopUp에 Delay를 주기 위한 변수
    int color_skyblue = Color.rgb(58, 204, 255); //Occupied Seat의 색정보
    int color_yellow = Color.rgb(255,247,132); //Empty Seat의 색정보
    int time_3s = 3000;
    int time_60s = 60000;


    int dot_check_int; //수신한 데이터의 dot(.)위치 식별에 사용할 변수

    // Pop up alarm information and default
    int alarm_temp = 20;
    String alarm_temp_st ="20";
    public char[] alarm_temp_arr ;
    public int bias_temp;

    //Air Conditioner information and default
    public int AC_temp;
    public String AC_Temp_st;
    public String AC_Display;
    public String Send_AC_Temp;


    public String temp="";
    public int PopupBtn_Checked=0;
    public int check;


    public String viewdata; //수신된 Data로 정의할 String변수
    public char[] data_arr; //수신된 Data를 arr로 정의할 char[]변수
    // --------------------------------------------------------------------
    public final static UUID HM_RX_TX =
            UUID.fromString(Sample_UUID.HM_RX_TX);
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";


    // --------------------------------------------------------------------
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };



    // --------------------------------------------------------------------
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();


            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                ACSwitch.setEnabled(false);


            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                ACSwitch.setEnabled(false);
                clearUI();

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                if (mConnected) {
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                    ACSwitch.setEnabled(true);
                }

            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                ACSwitch.setEnabled(true);
                viewdata=intent.getStringExtra(mBluetoothLeService.EXTRA_DATA);
                data_arr = viewdata.toCharArray();
//                Toast.makeText(Device_Main.this, viewdata, Toast.LENGTH_LONG).show();

                if(data_arr.length>9){
                    dot_check();
                    Seat_state();
                    Warning_popup();
                }
                else mDataField.setText("NO DATA");
            }
        }
    };

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    private void dot_check() {
        for (dot_check_int = 9; dot_check_int < data_arr.length; dot_check_int++) {
            if (data_arr[dot_check_int] == '.')
                break;
        }
        if (dot_check_int == data_arr.length) {
            mDataField.setText("NO DATA");
            temp = "NO DATA";
        } else
            displayData();
    }

    //mDatafield(layout의 온도표시창에 전달)에 수신한 Data를 display하는 method
    private void displayData() {
        int Arduino = data_arr.length-2;
        int RISC_V = data_arr.length;
        temp="";
        if ((data_arr[8] == '+')||(data_arr[8] == '-')){
            for (int i=9; i<RISC_V; i++) {
                if ((('0' <= data_arr[i]) && (data_arr[i] <= '9'))||(data_arr[i]=='.')){
                    temp += Character.toString(data_arr[i]);
                } else{
                    temp = "NO DATA";
                    break;
                }
            }
            if (temp != "NO DATA"){
                if (data_arr[8] == '+'){
                    temp += "℃";
                    mDataField.setText(temp);
                } else {
                    temp += "℃";
                    mDataField.setText("-"+temp);
                }
            } else
                mDataField.setText(temp);
        } else
            mDataField.setText("NO DATA");
    }


    // --------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_main);

        // --------------------------------------------------------------------
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mConnectionState = (TextView) findViewById(R.id.connection_state); // Sets up UI references.
        mDataField = (TextView) findViewById(R.id.data_value); // is serial present?
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // --------------------------------------------------------------------
        // 각각의 자리에 대한 변수 지정
        SEAT1 = (TextView) findViewById(R.id.seat1);
        SEAT2 = (TextView) findViewById(R.id.seat2);
        SEAT3 = (TextView) findViewById(R.id.seat3);
        SEAT4 = (TextView) findViewById(R.id.seat4);
        SEAT5 = (TextView) findViewById(R.id.seat5);

        // --------------------------------------------------------------------
        // Warning pop up ON/OFF switch
        MainSwitch = (Switch) findViewById(R.id.MainSwitch);
        MainSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mConnected) {
                    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(100); // 0.1초간 진동
                    if (isChecked)
                        Toast.makeText(Device_Main.this, "Warning Alarm is turned on.", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(Device_Main.this, "Warning Alarm is turned off.", Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(Device_Main.this, "Device is not connected.", Toast.LENGTH_SHORT).show();
            }
        });

        // --------------------------------------------------------------------
        // Pop up alarm default
        Show_alarm_temp = (TextView) findViewById(R.id.Show_alarm_temp);
        Show_alarm_temp.setText("Setting warning temp : "+alarm_temp+"℃");
        alarm_temp_arr = alarm_temp_st.toCharArray();
        bias_temp = alarm_temp_arr.length;

        // --------------------------------------------------------------------
        // A/C temp setting default
        Show_AC_temp = (TextView) findViewById(R.id.Show_AC_temp);
        ACSwitch = (Switch) findViewById(R.id.ACSwitch);

        // A/C ON,OFF 스위치 클릭
        ACSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mConnected) {
                    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(100); // 0.1초간 진동
                    if (isChecked) {
                        Toast.makeText(Device_Main.this, "Air Conditioner is turned on.", Toast.LENGTH_SHORT).show();
                        AC_temp = 25;
                        AC_Temp_st = Integer.toString(AC_temp);
//                        Send_AC_Temp = Integer.toString(AC_temp);
                        Show_AC_temp.setText("Setting A/C temp : " + AC_Temp_st + "℃");
                        AC_Display = "on";
                        AC_Display.getBytes();
                        characteristicTX.setValue(AC_Display);
                        mBluetoothLeService.writeCharacteristic(characteristicTX);
                    } else {
                        Toast.makeText(Device_Main.this, "Air Conditioner is turned off.", Toast.LENGTH_SHORT).show();
                        Show_AC_temp.setText("Setting A/C temp : " + "--");
                        AC_Display = "off";
                        AC_Display.getBytes();
                        characteristicTX.setValue(AC_Display);
                        mBluetoothLeService.writeCharacteristic(characteristicTX);
                    }
                } else
                    Toast.makeText(Device_Main.this, "Device is not connected.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_device_control, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --------------------------------------------------------------------
    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    // --------------------------------------------------------------------
    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, Sample_UUID.lookup(uuid, unknownServiceString));

            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    //received data의 1~5번째 인자 정보를 통해 좌석 정보를 업데이트하는 함수
    private void Seat_state(){
        if (data_arr[0] == 'N')SEAT1.setBackgroundResource(R.color.yellow);
        else if (data_arr[0] == 'Y') SEAT1.setBackgroundResource(R.color.skyblue);
        else SEAT1.setBackgroundResource(R.color.grey);

        if (data_arr[1] == 'N') SEAT2.setBackgroundResource(R.color.yellow);
        else if (data_arr[1] == 'Y') SEAT2.setBackgroundResource(R.color.skyblue);
        else SEAT2.setBackgroundResource(R.color.grey);

        if (data_arr[2] == 'N') SEAT3.setBackgroundResource(R.color.yellow);
        else if (data_arr[2] == 'Y') SEAT3.setBackgroundResource(R.color.skyblue);
        else SEAT3.setBackgroundResource(R.color.grey);

        if (data_arr[3] == 'N') SEAT4.setBackgroundResource(R.color.yellow);
        else if (data_arr[3] == 'Y') SEAT4.setBackgroundResource(R.color.skyblue);
        else SEAT4.setBackgroundResource(R.color.grey);

        if (data_arr[4] == 'N') SEAT5.setBackgroundResource(R.color.yellow);
        else if (data_arr[4] == 'Y') SEAT5.setBackgroundResource(R.color.skyblue);
        else SEAT5.setBackgroundResource(R.color.grey);
    }


    // --------------------------------------------------------------------
    // 특정 조건에서 Warning alarm을 나타냄
    private void Warning_popup() {
        // 좌석 상태 즉, 배경색을 가져오기 위한 변수 지정
        ColorDrawable color_SEAT1 = (ColorDrawable) SEAT1.getBackground();
        ColorDrawable color_SEAT2 = (ColorDrawable) SEAT2.getBackground();
        ColorDrawable color_SEAT3 = (ColorDrawable) SEAT3.getBackground();
        ColorDrawable color_SEAT4 = (ColorDrawable) SEAT4.getBackground();
        ColorDrawable color_SEAT5 = (ColorDrawable) SEAT5.getBackground();
        SEAT1bgColor = color_SEAT1.getColor();
        SEAT2bgColor = color_SEAT2.getColor();
        SEAT3bgColor = color_SEAT3.getColor();
        SEAT4bgColor = color_SEAT4.getColor();
        SEAT5bgColor = color_SEAT5.getColor();

        // Delay를 주기 위해 WarningPopUp에서 지정한 전역변수와 변수값을 가져온다
        MyApplication S_Time_1min = (MyApplication) getApplication();
        MyApplication S_Time_10min = (MyApplication) getApplication();
        MyApplication OK_1min = (MyApplication) getApplication();
        MyApplication OK_10min = (MyApplication) getApplication();
        MyApplication Device_data = (MyApplication) getApplication();

        // 첫번째 PopUp전 조건문을 통과하기 위한 초기설정
        DelayTime_1min_value = 0;
        DelayTime_10min_value = 0;

        // 함수에 들어오자 마자 시스템 시간이 변수에 저장
        EndTime = SystemClock.elapsedRealtime();

        // WarningPopUp에서 지정한 값을 변수에 저장
        OK_1min_value = OK_1min.getGlobalValue_1();
        OK_10min_value = OK_10min.getGlobalValue_10();


        // '확인' 버튼과 '10분간 대기' 버튼을 눌렀을 때 각각 다른 DelayTime을 받기 위한 조건문
        if ((OK_1min_value == "1min_set_on") && (OK_10min_value == "10min_set_off")) {
            StartTime_1min_value = S_Time_1min.getGlobalTime_1();
            DelayTime_1min_value = EndTime - StartTime_1min_value;
        } else if ((OK_1min_value == "1min_set_off") && (OK_10min_value == "10min_set_on")) {
            StartTime_10min_value = S_Time_10min.getGlobalTime_10();
            DelayTime_10min_value = EndTime - StartTime_10min_value;
        }

        if ((PopupBtn_Checked == 0) || ((DelayTime_1min_value > time_3s) && (OK_1min_value == "1min_set_on")) || ((DelayTime_10min_value > time_60s) && (OK_10min_value == "10min_set_on"))) { // WarningPopUp의 '확인' 버튼을 누른 후 3초 뒤, or '10분간 대기' 버튼을 누른 후 7초 뒤에 다시 팝업이 뜬다
            if (MainSwitch.isChecked()) { // 스위치가 On 되어 있을 경우만 팝업창을 뜨게 한다.
                if (SEAT1bgColor == color_yellow) {
                    if ((SEAT2bgColor == color_skyblue) || (SEAT3bgColor == color_skyblue) || (SEAT4bgColor == color_skyblue) || (SEAT5bgColor == color_skyblue)) { // 좌석에 한명이라도 타 있을 경우만 팝업창이 뜬다
                        if ((data_arr[8] == '+')&&(temp!="NO DATA")) {
                            if (dot_check_int - 9 > bias_temp) {
                                Intent intent = new Intent(this, WarningPopUp.class);
                                startActivity(intent);
                                Device_data.setGlobalValue_data(temp);
                                PopupBtn_Checked = 1;
                            }
                            else if (dot_check_int - 9 == bias_temp) {
                                for (check = 0; check < bias_temp; check++) {
                                    if (data_arr[dot_check_int - bias_temp + check] > alarm_temp_arr[check]) {
                                        Intent intent = new Intent(this, WarningPopUp.class);
                                        startActivity(intent);
                                        Device_data.setGlobalValue_data(temp);
                                        PopupBtn_Checked = 1;
                                        break;
                                    }
                                    else if (data_arr[dot_check_int - bias_temp + check] < alarm_temp_arr[check])
                                        break;
                                }
                                if (check == bias_temp) {
                                    Intent intent = new Intent(this, WarningPopUp.class);
                                    startActivity(intent);
                                    Device_data.setGlobalValue_data(temp);
                                    PopupBtn_Checked = 1;
                                }
                            } else
                                PopupBtn_Checked = 0;
                        }
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------------
    // Popup alarm 조절 버튼
    public void Popup_plus_btn(View view){
        if (mConnected) {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(40); // 0.1초간 진동
            alarm_temp += 5;
            alarm_temp_st = Integer.toString(alarm_temp);
            Show_alarm_temp.setText("Setting warning temp : " + alarm_temp_st + "℃");
            alarm_temp_arr = alarm_temp_st.toCharArray();
            bias_temp = alarm_temp_arr.length;
        } else
            Toast.makeText(Device_Main.this, "Device is not connected.", Toast.LENGTH_SHORT).show();
    }

    public void Popup_minus_btn(View view){
        if (mConnected) {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(40); // 0.1초간 진동
            if (alarm_temp >= 5) {
                alarm_temp -= 5;
                alarm_temp_st = Integer.toString(alarm_temp);
                Show_alarm_temp.setText("Setting warning temp : " + alarm_temp_st + "℃");
                alarm_temp_arr = alarm_temp_st.toCharArray();
                bias_temp = alarm_temp_arr.length;
            } else
                Toast.makeText(Device_Main.this, "Temperature must be higher than 0℃.", Toast.LENGTH_LONG).show();
        } else
            Toast.makeText(Device_Main.this, "Device is not connected.", Toast.LENGTH_SHORT).show();
    }

    // --------------------------------------------------------------------
    // A/C temp setting 조절 버튼
    public void AC_plus_btn(View view){
        if (mConnected) {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(40); // 0.1초간 진동
            if (ACSwitch.isChecked()) {
                if (AC_temp < 30) {
                    AC_temp += 1;
                    AC_Temp_st = Integer.toString(AC_temp);
                    Show_AC_temp.setText("Setting A/C temp : " + AC_Temp_st + "℃");
                } else
                    Toast.makeText(Device_Main.this, "Temperature must be lower than 30℃.", Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(Device_Main.this, "Air Conditioner is turned off. Turn on the A/C.", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(Device_Main.this, "Device is not connected.", Toast.LENGTH_SHORT).show();
    }

    public void AC_minus_btn(View view){
        if (mConnected){
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(40); // 0.1초간 진동
            if (ACSwitch.isChecked()) {
                if (AC_temp > 18) {
                    AC_temp -= 1;
                    AC_Temp_st = Integer.toString(AC_temp);
                    Show_AC_temp.setText("Setting A/C temp : " + AC_Temp_st + "℃");
                } else
                    Toast.makeText(Device_Main.this, "Temperature must be higher than 18℃.", Toast.LENGTH_LONG).show();
            } else
                Toast.makeText(Device_Main.this, "Air Conditioner is turned off. Turn on the A/C.", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(Device_Main.this, "Device is not connected.", Toast.LENGTH_SHORT).show();
    }

    public void AC_OK_btn(View view){
        if (mConnected){
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(40); // 0.1초간 진동
            if (ACSwitch.isChecked()) {
                Send_AC_Temp = Integer.toString(AC_temp);
                Send_AC_Temp+=".";
                Send_AC_Temp.getBytes();
                characteristicTX.setValue(Send_AC_Temp);
                mBluetoothLeService.writeCharacteristic(characteristicTX);
                Toast.makeText(Device_Main.this, "Air Conditioner temperature is set.", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(Device_Main.this, "Air Conditioner is turned off. Turn on the A/C.", Toast.LENGTH_SHORT).show();
        } else
            Toast.makeText(Device_Main.this, "Device is not connected.", Toast.LENGTH_SHORT).show();
    }

}