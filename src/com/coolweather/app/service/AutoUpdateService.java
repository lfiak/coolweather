package com.coolweather.app.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.coolweather.app.receiver.AutoUpdateReceiver;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public class AutoUpdateService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateWeather();
            }
        }).start();

        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int eightHours = 8 * 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + eightHours;
        Intent i = new Intent(this, AutoUpdateReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, 0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String cityName = prefs.getString("city_name", "");
        if (TextUtils.isEmpty(cityName)) {
            return;
        }

        String encodedCityName;
        try {
            encodedCityName = URLEncoder.encode(cityName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedCityName = cityName;
        }

        String address = "https://wttr.in/" + encodedCityName + "?format=j1";
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                Utility.handleFreeWeatherResponse(AutoUpdateService.this, response);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }
}
