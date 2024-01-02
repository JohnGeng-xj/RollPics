package com.example.rollpics;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MyService extends Service {
    private static final String TAG = "MyService";
    private Handler timehandler;
    private Handler handler = new Handler();
    //public static String weatherInfo="";
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Your task to be executed at midnight
        //Log.d(TAG, "Task started");
        timehandler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 使用openweathermap.org的API密钥和城市ID来发送请求获取天气预报数据
                String apiKey = "5a6ba72340fe27ca84040dbd8549dfbf";
                String cityId = "Urumqi"; // 北京的城市ID
                String apiUrl = "http://api.openweathermap.org/data/2.5/forecast?q=" + cityId + "&appid=" + apiKey + "&units=metric&lang=zh_cn";

                // 发送网络请求获取天气预报数据
                try {
                    URL url = new URL(apiUrl);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }


                    // 将获取到的天气预报数据发送给Handler处理
                    Message message = new Message();
                    //message.obj = result.toString();
                    message.obj = WeatherDataProcessor.processWeatherData(String.valueOf(result));
                    handler.sendMessage(message);

                    urlConnection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // 创建一个Handler来处理从线程发送过来的天气预报数据
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                //String result = (String) msg.obj;
                HashMap<String, TemperatureData> result = (HashMap<String, TemperatureData>) msg.obj;

                TreeMap<String, TemperatureData> sortedTemperatureMap = new TreeMap<>(result);


                // 示例：遍历 TreeMap 并打印每天的最高温度和最低温度
                int i=0;
                for (Map.Entry<String, TemperatureData> entry : sortedTemperatureMap.entrySet()) {
                    i+=1;
                    if(i>3) break;
                    String date = entry.getKey();
                    TemperatureData temperatureData = entry.getValue();
                    double maxTemp = temperatureData.getMaxTemperature();
                    double minTemp = temperatureData.getMinTemperature();

                    Date mydate = null;
                    try {
                        mydate = dateFormat.parse(date);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd");
                    String datePart = dateFormatter.format(mydate);

                    //Log.d("WeatherData", "Date: " + date + ", Max Temp: " + maxTemp + ", Min Temp: " + minTemp);
                    MainActivity.weatherInfo = MainActivity.weatherInfo + (datePart + "日: " + (int) maxTemp + "℃/" + (int) minTemp + "℃    ");
                    //dateTimeWeatherTextView.append( weatherInfo);
                    //timeweather=weatherInfo;
                }
            }
        };

        // Stop the service after the task is completed
        stopSelf();
        return START_NOT_STICKY;
    }
}
