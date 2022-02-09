package com.example.weatherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener{

    private RelativeLayout homeRl;
    private ProgressBar loadingPB;
    private TextView cityNameTV, temperatureTV, conditionTV;
    private RecyclerView weatherRV;
    private TextInputEditText cityEdt;
    private ImageView backIV, iconIV;
    private LinearProgressIndicator linearProgressIndicator;
    private ArrayList<WeatherRVModel> weatherRVModelArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private LocationManager locationManager;
    private int PERMISSION_CODE = 1;
    private int LOCATION_CODE = 1;
    private final String imageUrl = "https://images.unsplash.com/photo-1532074534361-bb09a38cf917?i%C3%97id=MnwxMJA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fH%C3%978&i%C3%97lib=rb-1.2.1&auto=format&fit=crop&w=334&q=80";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        homeRl = findViewById(R.id.idRLHome);
        loadingPB = findViewById(R.id.idPBLoading);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        weatherRV = findViewById(R.id.idRVWeather);
        cityEdt = findViewById(R.id.idEdtCity);
        backIV = findViewById(R.id.idIVBack);
        iconIV = findViewById(R.id.idIVIcon);
        linearProgressIndicator = findViewById(R.id.linearProgressIndicator);
        weatherRVModelArrayList = new ArrayList();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModelArrayList);
        weatherRV.setAdapter(weatherRVAdapter);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        } else {
            updateUI();
        }


        cityEdt.setOnTouchListener((v, event) -> {
            final int DRAWABLE_LEFT = 0;
            final int DRAWABLE_TOP = 1;
            final int DRAWABLE_RIGHT = 2;
            final int DRAWABLE_BOTTOM = 3;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (cityEdt.getRight() - cityEdt.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                    String city = cityEdt.getText().toString();
                    if (city.isEmpty()) {
                        Toast.makeText(MainActivity.this, "please enter city Name", Toast.LENGTH_SHORT).show();
                    } else {
                        getWeatherInfo(city);
                    }
                    return true;
                }
            }
            return false;
        });

    }

    private void updateUI() {

        if (isLocationEnabled()){
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Log.d("fsdfs", "location: "+location);
            if (location != null) {
                Log.d("sdfdsf", "Longitude: " + location.getLongitude() + "  Latitude" + location.getLatitude());
                validLocation(location);
            } else {
                //This is what you need:
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            }
        }
        else{
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
/*        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),LOCATION_CODE);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        onBackPressed();
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();*/

        final View permissionView = LayoutInflater.from(this).inflate(R.layout.update_dialog, null);
        final androidx.appcompat.app.AlertDialog permissionDialog = new androidx.appcompat.app.AlertDialog.Builder(this).create();

        TextView tv_no_thanks = permissionView.findViewById(R.id.tv_no_thanks);
        tv_no_thanks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                permissionDialog.dismiss();
            }
        });
        Button btnOK = permissionView.findViewById(R.id.btnOK);
        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),LOCATION_CODE);
                permissionDialog.dismiss();
            }
        });
        permissionDialog.setView(permissionView);
        permissionDialog.show();

        permissionDialog.setCancelable(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("dfsdfsdf", "requestCode: "+requestCode+" resultCode: "+resultCode);
        updateUI();
    }

    private boolean isLocationEnabled() {
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                updateUI();
            } else {
                Toast.makeText(this, "Please provide the permissions", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getCityName(double longitude, double latitude) {
        String cityName = "Not found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 10);

            for (Address adr : addresses) {
                if (adr != null) {
                    String city = adr.getLocality();

                    if (city != null && !city.equals("")) {
                        cityName = city;
                    } else {
                        Log.d("TAG", "CITY NOT FOUND");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }


    private void getWeatherInfo(String cityName) {
        //http://api.weatherapi.com/v1/forecast.json?key=44ca2d5bfdf740cc8d665434212611&q=Patna&days=1&aqi=no&alerts=no
        Log.d("sdfdsf", "getWeatherInfo: ");
        String url = "https://api.weatherapi.com/v1/forecast.json?key=44ca2d5bfdf740cc8d665434212611&q=" + cityName + "&days=1&aqi=no&alerts=no";
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        linearProgressIndicator.setVisibility(View.VISIBLE);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                cityNameTV.setText(cityName);
                linearProgressIndicator.setVisibility(View.GONE);
                loadingPB.setVisibility(View.GONE);
                homeRl.setVisibility(View.VISIBLE);
                weatherRVModelArrayList.clear();
                hideKeyboard(MainActivity.this);
                cityEdt.clearFocus();
                try {
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    temperatureTV.setText(temperature + "°C");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("https:".concat(conditionIcon)).into(iconIV);
                    conditionTV.setText(condition);
                    Picasso.get().load(imageUrl).into(backIV);
                    JSONObject forecastObj = response.getJSONObject("forecast");
                    JSONObject forecastO = forecastObj.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = forecastO.getJSONArray("hour");
                    for (int i = 0; i < hourArray.length(); i++) {
                        JSONObject hourObj = hourArray.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String img = hourObj.getJSONObject("condition").getString("icon");
                        String wind = hourObj.getString("wind_kph");
                        weatherRVModelArrayList.add(new WeatherRVModel(time, temper, img, wind));

                    }
                    weatherRVAdapter.notifyDataSetChanged();

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("sdfdsf", "onErrorResponse: " + error);
                linearProgressIndicator.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Please enter valid city name..", Toast.LENGTH_SHORT).show();

            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        validLocation(location);
    }

    private void validLocation(Location location) {
        String cityName = getCityName(location.getLongitude(), location.getLatitude());
        if (cityName.equals("not Not found"))
            Toast.makeText(this, "User City Not Found..", Toast.LENGTH_SHORT).show();
        else
            getWeatherInfo(cityName);
    }

    @Override
    public void onLocationChanged(@NonNull List<Location> locations) {
        LocationListener.super.onLocationChanged(locations);
    }

    @Override
    public void onFlushComplete(int requestCode) {
        LocationListener.super.onFlushComplete(requestCode);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LocationListener.super.onStatusChanged(provider, status, extras);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }
}
