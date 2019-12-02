package com.VCP_191202_Final;
// AndroidManifest.xml <Application> 내 android:name=".MyApplication" 코드 추가 필수

import android.app.Application;

public class MyApplication extends Application {
    // 4개의 전역변수를 쓰고 싶으면 4개의 함수그룹을 사용해야 한다
    private long Time_1;
    public long getGlobalTime_1(){
        return Time_1;
    }
    public void setGlobalTime_1(long mTime_1){
        this.Time_1 = mTime_1;
    }
    private long Time_10;
    public long getGlobalTime_10(){
        return Time_10;
    }
    public void setGlobalTime_10(long mTime_10){
        this.Time_10 = mTime_10;
    }
    private String Value_1;
    public String getGlobalValue_1(){
        return Value_1;
    }
    public void setGlobalValue_1(String mValue_1){
        this.Value_1 = mValue_1;
    }
    private String Value_10;
    public String getGlobalValue_10(){
        return Value_10;
    }
    public void setGlobalValue_10(String mValue_10){
        this.Value_10 = mValue_10;
    }
    private String Device_data;
    public String getGlobalValue_data(){ return Device_data; }
    public void setGlobalValue_data(String mDevice_data){ this.Device_data = mDevice_data; }
}