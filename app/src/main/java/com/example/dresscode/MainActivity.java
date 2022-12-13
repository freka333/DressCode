package com.example.dresscode;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    EditText cityText;
    TextView feelsLikeText, humidityValueText, windValueText, weatherDescriptionText;
    Button getLocationButton, forecastButton;
    ImageView weatherIcon, dressImage;
    String iconURL;
    static String gender = "woman";

    private final String url = "https://api.openweathermap.org/data/2.5/weather";
    private final String appid = BuildConfig.OPENWEATHERMAPAPIKEY;
    DecimalFormat df = new DecimalFormat("#.#");

    FusedLocationProviderClient fusedLocationProviderClient;
    private final static int REQUEST_CODE = 100;

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
        setContentView(R.layout.activity_main);

        String currentDate = new SimpleDateFormat("yyyy.MM.dd EEEE", Locale.getDefault()).format(new Date());

        TextView textViewDate = findViewById(R.id.dateTextView);
        textViewDate.setText(currentDate);

        getLocationButton = findViewById(R.id.getLocationButton);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }

        cityText = findViewById(R.id.cityInputText);
        feelsLikeText = findViewById(R.id.feelsLikeTextView);
        humidityValueText = findViewById(R.id.humidityValueTextView);
        windValueText = findViewById(R.id.windValueTextView);
        weatherDescriptionText = findViewById(R.id.weatherDescriptionTextView);
        weatherIcon = findViewById(R.id.weatherImageView);
        Picasso.get().load(iconURL).into(weatherIcon);
        dressImage = findViewById(R.id.dressImageView);
        forecastButton = findViewById(R.id.forecastButton);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Objects.requireNonNull(mSensorManager).registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        mAccel = 10f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        forecastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ForecastActivity.class);
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
                        gender = (gender == "man") ? "woman" : "man";
                        setClothingImage(feelsLikeText, dressImage);
                        Log.d(TAG, "Shake event detected");
                    }
                }
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    @Override
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

    private void setWeatherIcon() {
        weatherIcon = findViewById(R.id.weatherImageView);
        Picasso.get().load(iconURL).into(weatherIcon);
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location location = task.getResult();
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(
                                location.getLatitude(), location.getLongitude(), 1
                        );
                        if(CityLocation.city.isEmpty()) {
                            CityLocation.city = addresses.get(0).getLocality();
                        }
                        cityText.setText(CityLocation.city);
                        getWeather(cityText.getText().toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    CityLocation.city = "Budapest";
                    cityText.setText(CityLocation.city);
                    getWeather(cityText.getText().toString());
                }
            }
        });
    }

    static void setClothingImage(TextView tempText, ImageView dress) {
        Integer[] clothes = DressIcons.clothes;
        String tempString = tempText.getText().toString().trim();
        tempString = tempString.substring(0, tempString.length() - 2);
        double temp = Double.parseDouble(tempString);
        int value;
        if(temp < 5 ) value = gender == "woman" ? 3 : 11;
        else if(temp < 10) value = gender == "woman" ? 1 : 9;
        else if(temp < 15) value = gender == "woman" ? 0 : 8;
        else if(temp < 20) value = gender == "woman" ? 6 : 14;
        else if(temp < 25) value = gender == "woman" ? 4 : 12;
        else if(temp < 30) value = gender == "woman" ? 7 : 15;
        else value = gender == "woman" ? 2 : 10;
        dress.setImageResource(clothes[value]);
    }

    public void getWeather(String city) {
        String tempURL = url + "?q=" + city + "&appid=" + appid + "&units=metric";
        StringRequest stringRequest = new StringRequest(Request.Method.POST, tempURL, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray jsonArray = jsonResponse.getJSONArray("weather");
                    JSONObject jsonObjectWeather = jsonArray.getJSONObject(0);
                    String description = jsonObjectWeather.getString("description");
                    String icon = jsonObjectWeather.getString("icon");
                    JSONObject jsonObjectMain = jsonResponse.getJSONObject("main");
                    double feelsLike = jsonObjectMain.getDouble("feels_like");
                    int humidity = jsonObjectMain.getInt("humidity");
                    JSONObject jsonObjectWind = jsonResponse.getJSONObject("wind");
                    String wind = jsonObjectWind.getString("speed");
                    feelsLikeText.setText(df.format(feelsLike) + "°C");
                    humidityValueText.setText(humidity + "%");
                    windValueText.setText(wind + "m/s");
                    weatherDescriptionText.setText(description);
                    iconURL = "http://openweathermap.org/img/wn/" + icon + "@2x.png";
                    setWeatherIcon();
                    setClothingImage(feelsLikeText, dressImage);
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