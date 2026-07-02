package com.example.weather;

// Импорты
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    EditText etCity;        // поле для ввода города
    Button btnSearch;       // искать погоду по городу
    Button btnMyLocation;   // погода по моему местоположению
    TextView tvResult;      // вывод погоды

    static final int REQ_LOCATION = 55;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etCity = (EditText) findViewById(R.id.etCity);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        btnMyLocation = (Button) findViewById(R.id.btnMyLocation);
        tvResult = (TextView) findViewById(R.id.tvResult);

        // Поиск по городу
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String city = etCity.getText().toString().trim();
                if (city.length() == 0) {
                    Toast.makeText(MainActivity.this, "Введите город", Toast.LENGTH_SHORT).show();
                    return;
                }
                loadByCity(city);
            }
        });

        // Погода по текущему местоположению
        btnMyLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                useMyLocation();
            }
        });
    }

    // Шаг 1: находим координаты города, затем грузим погоду
    void loadByCity(final String city) {
        tvResult.setText("Загрузка...");
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Геокодинг: имя города -> широта/долгота (API Open-Meteo, без ключа)
                    String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?count=1&name="
                            + URLEncoder.encode(city, "UTF-8");
                    JSONObject geo = new JSONObject(httpGet(geoUrl));
                    JSONArray results = geo.optJSONArray("results");
                    if (results == null || results.length() == 0) {
                        showText("Город не найден");
                        return;
                    }
                    JSONObject place = results.getJSONObject(0);
                    double lat = place.getDouble("latitude");
                    double lon = place.getDouble("longitude");
                    String name = place.getString("name");
                    loadForecast(lat, lon, name); // шаг 2
                } catch (Exception e) {
                    e.printStackTrace();
                    showText("Ошибка загрузки");
                }
            }
        }).start();
    }

    // Шаг 2: грузим текущую погоду и прогноз на 5 дней по координатам.
    // Этот метод уже вызывается из фонового потока.
    void loadForecast(final double lat, final double lon, final String name) {
        try {
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat
                    + "&longitude=" + lon
                    + "&current_weather=true"
                    + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum"
                    + "&forecast_days=5&timezone=auto";
            JSONObject root = new JSONObject(httpGet(url));

            // Текущая погода
            JSONObject cur = root.getJSONObject("current_weather");
            double t = cur.getDouble("temperature");

            // Прогноз по дням
            JSONObject daily = root.getJSONObject("daily");
            JSONArray days = daily.getJSONArray("time");
            JSONArray tmax = daily.getJSONArray("temperature_2m_max");
            JSONArray tmin = daily.getJSONArray("temperature_2m_min");
            JSONArray prec = daily.getJSONArray("precipitation_sum");

            StringBuilder sb = new StringBuilder();
            sb.append(name).append("\n");
            sb.append("Сейчас: ").append(t).append(" °C\n\n");
            sb.append("Прогноз на 5 дней:\n");
            for (int i = 0; i < days.length(); i++) {
                sb.append(days.getString(i)).append(": ")
                        .append(tmin.getDouble(i)).append("..").append(tmax.getDouble(i))
                        .append(" °C, осадки ").append(prec.getDouble(i)).append(" мм\n");
            }
            showText(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            showText("Ошибка загрузки прогноза");
        }
    }

    // Универсальный GET-запрос: возвращает ответ сервера как строку
    String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(10000);
        con.setReadTimeout(10000);
        BufferedReader br = new BufferedReader(
                new InputStreamReader(con.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        con.disconnect();
        return sb.toString();
    }

    // Обновляет текст на экране (всегда в главном потоке)
    void showText(final String s) {
        runOnUiThread(new Runnable() {
            public void run() {
                tvResult.setText(s);
            }
        });
    }

    // Пытается взять последнее известное местоположение и загрузить погоду
    void useMyLocation() {
        // Проверяем разрешение на геолокацию
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Просим разрешение у пользователя
            requestPermissions(new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        fetchLocationAndWeather();
    }

    // Сюда возвращается ответ на запрос разрешения
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather();
        } else {
            Toast.makeText(this, "Нужно разрешение на геолокацию", Toast.LENGTH_SHORT).show();
        }
    }

    // Берёт последнее известное местоположение и грузит погоду
    void fetchLocationAndWeather() {
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            // getLastKnownLocation возвращает последнюю сохранённую позицию (может быть null)
            Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc == null) {
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            if (loc == null) {
                Toast.makeText(this, "Местоположение недоступно, введите город", Toast.LENGTH_SHORT).show();
                return;
            }
            final double lat = loc.getLatitude();
            final double lon = loc.getLongitude();
            tvResult.setText("Загрузка...");
            // Сетевой запрос — в отдельном потоке
            new Thread(new Runnable() {
                public void run() {
                    loadForecast(lat, lon, "Моё местоположение");
                }
            }).start();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
}
