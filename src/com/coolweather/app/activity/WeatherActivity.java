package com.coolweather.app.activity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.coolweather.app.service.AutoUpdateService;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;
import com.example.coolweather.R;

public class WeatherActivity extends Activity implements View.OnClickListener {

    private LinearLayout weatherInfoLayout;
    private View weatherBackground;
    private TextView cityNameText;
    private TextView publishText;
    private TextView weatherDespText;
    private TextView temp1Text;
    private TextView temp2Text;
    private TextView currentDateText;
    private Button switchCity;
    private Button refreshWeather;
    private ValueAnimator backgroundAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.weather_layout);
        weatherInfoLayout = (LinearLayout) findViewById(R.id.weather_info_layout);
        weatherBackground = findViewById(R.id.weather_background);
        cityNameText = (TextView) findViewById(R.id.city_name);
        publishText = (TextView) findViewById(R.id.publish_text);
        weatherDespText = (TextView) findViewById(R.id.weather_desp);
        temp1Text = (TextView) findViewById(R.id.temp1);
        temp2Text = (TextView) findViewById(R.id.temp2);
        currentDateText = (TextView) findViewById(R.id.current_data);
        switchCity = (Button) findViewById(R.id.switch_city);
        refreshWeather = (Button) findViewById(R.id.refresh_weather);

        String countyCode = getIntent().getStringExtra("county_code");
        String countyName = getIntent().getStringExtra("county_name");
        if (!TextUtils.isEmpty(countyName)) {
            publishText.setText("同步中...");
            weatherInfoLayout.setVisibility(View.INVISIBLE);
            cityNameText.setVisibility(View.INVISIBLE);
            queryWeatherByCityName(countyName);
        } else if (!TextUtils.isEmpty(countyCode)) {
            showWeather();
        } else {
            showWeather();
        }

        switchCity.setOnClickListener(this);
        refreshWeather.setOnClickListener(this);
        startBackgroundAnimation();
    }

    @Override
    protected void onDestroy() {
        if (backgroundAnimator != null) {
            backgroundAnimator.cancel();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.switch_city:
            Intent intent = new Intent(this, ChooseAreaActivity.class);
            intent.putExtra("from_weather_activity", true);
            startActivity(intent);
            finish();
            break;

        case R.id.refresh_weather:
            publishText.setText("同步中...");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String cityName = prefs.getString("city_name", "");
            if (!TextUtils.isEmpty(cityName)) {
                queryWeatherByCityName(cityName);
            } else {
                publishText.setText("暂无城市");
            }
            break;
        default:
            break;
        }
    }

    private void queryWeatherByCityName(String cityName) {
        String encodedCityName;
        try {
            encodedCityName = URLEncoder.encode(cityName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedCityName = cityName;
        }
        String address = "https://wttr.in/" + encodedCityName + "?format=j1";
        queryFromServer(address);
    }

    private void queryFromServer(final String address) {
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {

            @Override
            public void onFinish(String response) {
                final boolean success = Utility.handleFreeWeatherResponse(WeatherActivity.this, response);
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (success) {
                            showWeather();
                        } else {
                            publishText.setText("解析失败");
                        }
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        publishText.setText("同步失败");
                    }
                });
            }
        });
    }

    private void showWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        cityNameText.setText(prefs.getString("city_name", ""));
        temp1Text.setText(prefs.getString("temp1", ""));
        temp2Text.setText(prefs.getString("temp2", ""));
        weatherDespText.setText(prefs.getString("weather_desp", ""));
        publishText.setText("今天 " + prefs.getString("publish_time", "--:--") + " 发布");
        currentDateText.setText(prefs.getString("current_date", ""));
        weatherInfoLayout.setVisibility(View.VISIBLE);
        cityNameText.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    private void startBackgroundAnimation() {
        final int dayBlue = 0xFF4F8DFD;
        final int duskPurple = 0xFF6C63FF;
        backgroundAnimator = ValueAnimator.ofFloat(0f, 1f);
        backgroundAnimator.setDuration(4500);
        backgroundAnimator.setRepeatMode(ValueAnimator.REVERSE);
        backgroundAnimator.setRepeatCount(ValueAnimator.INFINITE);
        backgroundAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = animation.getAnimatedFraction();
                int color = (Integer) new ArgbEvaluator().evaluate(fraction, dayBlue, duskPurple);
                weatherBackground.setBackgroundColor(color);
            }
        });
        backgroundAnimator.start();
    }
}
