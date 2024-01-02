package com.example.rollpics;

// MainActivity.java
// MainActivity.java

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.net.ssl.HttpsURLConnection;


class WeatherDataProcessor {

    public static HashMap<String, TemperatureData> processWeatherData(String jsonData) {


        HashMap<String, TemperatureData> temperatureMap = new HashMap<>();


        try {
            JSONObject jsonObject = new JSONObject(jsonData);

            JSONArray weatherList = jsonObject.getJSONArray("list");
            for (int i = 0; i < weatherList.length(); i++) {
                JSONObject weatherData = weatherList.getJSONObject(i);
                String dateTime = weatherData.getString("dt_txt").split(" ")[0]; // Extract date

                JSONObject main = weatherData.getJSONObject("main");
                double tempMax = main.getDouble("temp_max");
                double tempMin = main.getDouble("temp_min");

                if (!temperatureMap.containsKey(dateTime)) {
                    temperatureMap.put(dateTime, new TemperatureData(tempMax, tempMin));
                } else {
                    TemperatureData existingData = temperatureMap.get(dateTime);
                    if (tempMax > existingData.getMaxTemperature()) {
                        existingData.setMaxTemperature(tempMax);
                    }
                    if (tempMin < existingData.getMinTemperature()) {
                        existingData.setMinTemperature(tempMin);
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return temperatureMap;
    }
}

class TemperatureData {
    private double maxTemperature;
    private double minTemperature;

    public TemperatureData(double maxTemperature, double minTemperature) {
        this.maxTemperature = maxTemperature;
        this.minTemperature = minTemperature;
    }

    public double getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(double maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public double getMinTemperature() {
        return minTemperature;
    }

    public void setMinTemperature(double minTemperature) {
        this.minTemperature = minTemperature;
    }
}

public class MainActivity extends AppCompatActivity {
    private static final int ALARM_ID = 123; // Unique ID for the alarm
    private ImageView imageView;
    private TextView dateTimeWeatherTextView,newsTextView;

    public static String weatherInfo = "";

    public static String newsText="news here!";

    private String city = "Urumqi";

    private int currentImageIndex = 0;
    private ArrayList<Uri> imageUris = new ArrayList<>();

    private Handler handler = new Handler();


    // Add your Bing Search V7 subscription key to your environment variables.
//    static String subscriptionKey = "2e9a173275c149afa04280289568227c";
//
//    // Add your Bing Search V7 endpoint to your environment variables.
//    static String endpoint = "https://api.bing.microsoft.com/bing/v7.0/news/search";
//
//    static String searchTerm = "international news";

    public MainActivity() throws Exception {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make the app run in full-screen mode
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        imageView = findViewById(R.id.imageView);
        dateTimeWeatherTextView = findViewById(R.id.dateTimeWeatherTextView);

        newsTextView = findViewById(R.id.newsTextView);

        newsTextView.setText(newsText);

        // Open the gallery to let the user select images
        openGallery();

        // Start the image slideshow
        startImageSlideshow();

        // Update date, time, and weather information

        //timehandler = new Handler(Looper.getMainLooper());

        // 启动线程定时更新
        startUpdateThread();

        setMidnightAlarm();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //startUpdateNewsThread();



    }

    @SuppressLint("ScheduleExactAlarm")
    private void setMidnightAlarm() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        // Use setExact on Android 6.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

//    private  void startUpdateNewsThread(){
//
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (true) {
//                    try {
//                        // 在主线程中更新UI
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                try {
//                                    SearchResults result = searchNews(searchTerm);
//                                    newsTextView.setText(prettify(result.jsonResponse));
//                                } catch (Exception e) {
//                                    throw new RuntimeException(e);
//                                }
//                            }
//                        });
//                        // 这个休眠时间用于更新DateTimeWeatherTextView
//                        Thread.sleep(6000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();
//
//        //new Thread(new FetchNewsTask("newsthread")).start();
//        //newsTextView.setText(MainActivity.newsText);
//
//        //FetchNewsTask newsthread= new FetchNewsTask("newsthread");
//        //newsthread.start();
//
//        //new Thread(new FetchNewsTask()).start();
//
//        //newsTextView.setText("MainActivity.newsText");
//    }
    private void startUpdateThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // 在主线程中更新UI
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    updateDateTimeWeatherTextView();
                                } catch (ParseException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                        // 这个休眠时间用于更新DateTimeWeatherTextView
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void updateDateTimeWeatherTextView() throws ParseException {


        // 获取当前日期和时间
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日 HH时mm分", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        String currentDateTime = sdf.format(new Date());

        // 更新TextView的内容
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            dateTimeWeatherTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }
            dateTimeWeatherTextView.setText(currentDateTime + "      " + weatherInfo);
        //dateTimeWeatherTextView.setText(timeweather);
    }

    private void openGallery() {
        // Use Intent to query the device's image content provider
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // User has successfully selected images from the gallery
            Uri selectedImageUri = data.getData();
            imageUris = getAllImagesUris(); // Get all images from the device
            if (!imageUris.isEmpty()) {
                // Start slideshow if images are available
                startImageSlideshow();
            }
        }
    }

    private ArrayList<Uri> getAllImagesUris() {
        ArrayList<Uri> imageUris = new ArrayList<>();

        // Get all images from the device's external storage
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                imageUris.add(Uri.parse("file://" + imagePath));
            }
            cursor.close();
        }

        return imageUris;
    }

    private void startImageSlideshow() {
        final int delayMillis = 10000; // 5 seconds

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Display the next image
                showNextImage();
                handler.postDelayed(this, delayMillis);
            }
        }, delayMillis);
    }

    private void showNextImage() {
        if (!imageUris.isEmpty()) {
            currentImageIndex = (currentImageIndex + 1) % imageUris.size();
            imageView.setImageURI(imageUris.get(currentImageIndex));
        }
    }


//    public SearchResults searchNews(String searchQuery) throws IOException {
//        // Construct URL of search request (endpoint + query string)
//        SearchResults results;
//        URL url = new URL(endpoint + "?q=" +  URLEncoder.encode("Microsoft", "UTF-8"));
//        HttpsURLConnection connection = (HttpsURLConnection)url.openConnection();
//        connection.setRequestProperty("Ocp-Apim-Subscription-Key", subscriptionKey);
//        int responseCode = connection.getResponseCode();
//
//        if (responseCode == HttpURLConnection.HTTP_OK) {
//            // Receive JSON body
//            InputStream stream = connection.getInputStream();
//            Scanner scanner = new Scanner(stream);
//            String response = scanner.useDelimiter("\\A").next();
//
//            // Construct result object for return
//            results = new SearchResults(new HashMap<String, String>(), response);
//
//            // Extract Bing-related HTTP headers
//            Map<String, List<String>> headers = connection.getHeaderFields();
//            for (String header : headers.keySet()) {
//                if (header == null) continue;      // may have null key
//                if (header.startsWith("BingAPIs-") || header.startsWith("X-MSEdge-")) {
//                    results.relevantHeaders.put(header, headers.get(header).get(0));
//                }
//            }
//
//            scanner.close();
//            stream.close();
//        }else {
//                throw new IOException("HTTP request failed with response code: " + responseCode);
//
//            }
//
//            return results;
//    }
//
//
//    // Pretty-printer for JSON; uses GSON parser to parse and re-serialize
//    public static String prettify(String json_text) {
//        JsonObject json = JsonParser.parseString(json_text).getAsJsonObject();
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        return gson.toJson(json);
//    }
//
//
//    class SearchResults {
//        HashMap<String, String> relevantHeaders;
//        String jsonResponse;
//
//        SearchResults(HashMap<String, String> headers, String json) {
//            relevantHeaders = headers;
//            jsonResponse = json;
//        }
//    }
//
//    private class NewsUpdateTask extends AsyncTask<Void, Void, SearchResults> {
//        @Override
//        protected SearchResults doInBackground(Void... params) {
//            try {
//                return searchNews(searchTerm);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        @Override
//        protected void onPostExecute(SearchResults results) {
//            super.onPostExecute(results);
//            newsTextView.setText(prettify(results.jsonResponse));
//        }
//    }
//
//    private void startUpdateNewsThread() {
//        Timer timer = new Timer();
//        timer.scheduleAtFixedRate(new TimerTask() {
//            @Override
//            public void run() {
//                new NewsUpdateTask().execute();
//            }
//        }, 0, 6000);
//    }


}


