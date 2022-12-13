package com.example.dresscode;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Objects;

public class ForecastActivity extends AppCompatActivity {
    String city = CityLocation.city;
    Button getLocationButton, backButton;
    EditText cityText;
    TextView dateTextView1, dateTextView2, dateTextView3, tempTextView1, tempTextView2, tempTextView3;
    ImageView dressImageView1, dressImageView2, dressImageView3;

    private final String url = "https://api.openweathermap.org/data/2.5/forecast";
    private final String appid = "9f45d6cc53f4977df479768a4ad9b107";
    DecimalFormat df = new DecimalFormat("#.#");

    private SensorManager mSensorManager;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    private long mLastShakeTime;
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
    private static final float SHAKE_THRESHOLD = 3.25f; // m/S**2

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);

        cityText = findViewById(R.id.cityInputText);
        cityText.setText(city);
        getLocationButton = findViewById(R.id.getLocationButton);
        dateTextView1 = findViewById(R.id.dateTextView1);
        tempTextView1 = findViewById(R.id.tempTextView1);
        dateTextView2 = findViewById(R.id.dateTextView2);
        tempTextView2 = findViewById(R.id.tempTextView2);
        dateTextView3 = findViewById(R.id.dateTextView3);
        tempTextView3 = findViewById(R.id.tempTextView3);
        dressImageView1 = findViewById(R.id.dressImageView1);
        dressImageView2 = findViewById(R.id.dressImageView2);
        dressImageView3 = findViewById(R.id.dressImageView3);
        backButton = findViewById(R.id.backButton);

        getWeather(city);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Objects.requireNonNull(mSensorManager).registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 10f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ForecastActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
    private static final String TAG = "MyActivity";

    private final SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long curTime = System.currentTimeMillis();
                if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    double acceleration = Math.sqrt(Math.pow(x, 2) +
                            Math.pow(y, 2) +
                            Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;

                    if (acceleration > SHAKE_THRESHOLD) {
                        mLastShakeTime = curTime;
                        MainActivity.gender = (MainActivity.gender == "man") ? "woman" : "man";
                        MainActivity.setClothingImage(tempTextView1, dressImageView1);
                        MainActivity.setClothingImage(tempTextView2, dressImageView2);
                        MainActivity.setClothingImage(tempTextView3, dressImageView3);
                        Log.d(TAG, "Shake event detected");
                    }
                }
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };@Override
    protected void onResume() {
        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        super.onResume();
    }
    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        super.onPause();
    }

    public void getWeather(String city) {
        String tempURL = url + "?q=" + city + "&cnt=4&appid=" + appid + "&units=metric";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, tempURL, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray jsonArray = jsonResponse.getJSONArray("list");
                    JSONObject jsonObject1 = jsonArray.getJSONObject(1);
                    JSONObject jsonObject2 = jsonArray.getJSONObject(2);
                    JSONObject jsonObject3 = jsonArray.getJSONObject(3);
                    String date1 = jsonObject1.getString("dt_txt");
                    String date2 = jsonObject2.getString("dt_txt");
                    String date3 = jsonObject3.getString("dt_txt");
                    JSONObject jsonObjectMain1 = jsonObject1.getJSONObject("main");
                    double feelsLike1 = jsonObjectMain1.getDouble("feels_like");
                    tempTextView1.setText(df.format(feelsLike1) + "°C");
                    JSONObject jsonObjectMain2 = jsonObject2.getJSONObject("main");
                    double feelsLike2 = jsonObjectMain2.getDouble("feels_like");
                    tempTextView2.setText(df.format(feelsLike2) + "°C");
                    JSONObject jsonObjectMain3 = jsonObject3.getJSONObject("main");
                    double feelsLike3 = jsonObjectMain3.getDouble("feels_like");
                    tempTextView3.setText(df.format(feelsLike3) + "°C");
                    dateTextView1.setText(date1.substring(0, date1.length() - 3));
                    dateTextView2.setText(date2.substring(0, date2.length() - 3));
                    dateTextView3.setText(date3.substring(0, date3.length() - 3));
                    MainActivity.setClothingImage(tempTextView1, dressImageView1);
                    MainActivity.setClothingImage(tempTextView2, dressImageView2);
                    MainActivity.setClothingImage(tempTextView3, dressImageView3);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), error.toString().trim(), Toast.LENGTH_SHORT).show();
            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        requestQueue.add(stringRequest);
    }

    public void getWeatherDetails(View view) {
        CityLocation.city = cityText.getText().toString().trim();
        getWeather(CityLocation.city);
    }
}