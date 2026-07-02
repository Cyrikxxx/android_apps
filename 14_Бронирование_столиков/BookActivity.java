package com.example.restaurant;

// Импорты
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class BookActivity extends Activity {

    TextView tvName;
    Spinner spDay, spTime;
    Button btnBook;

    String restaurant;              // название ресторана (пришло с предыдущего экрана)
    SharedPreferences prefs;

    // Возможные времена (слоты) и дни на выбор
    String[] allSlots = {"12:00", "14:00", "16:00", "18:00", "20:00"};
    ArrayList<String> days = new ArrayList<String>();       // ближайшие 5 дней
    ArrayList<String> freeSlots = new ArrayList<String>();  // свободные слоты выбранного дня

    ArrayAdapter<String> timeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book);

        tvName = (TextView) findViewById(R.id.tvName);
        spDay = (Spinner) findViewById(R.id.spDay);
        spTime = (Spinner) findViewById(R.id.spTime);
        btnBook = (Button) findViewById(R.id.btnBook);

        restaurant = getIntent().getStringExtra("restaurant");
        tvName.setText(restaurant);

        prefs = getSharedPreferences("booking", MODE_PRIVATE);

        // Готовим список ближайших 5 дней
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar c = Calendar.getInstance();
        for (int i = 0; i < 5; i++) {
            days.add(sdf.format(c.getTime()));
            c.add(Calendar.DAY_OF_YEAR, 1); // следующий день
        }
        spDay.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, days));

        timeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, freeSlots);
        spTime.setAdapter(timeAdapter);

        // При смене дня пересчитываем свободные слоты
        spDay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateFreeSlots();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Кнопка "Забронировать"
        btnBook.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                book();
            }
        });
    }

    // Пересчитывает свободные слоты для выбранного дня (убирает уже занятые)
    void updateFreeSlots() {
        freeSlots.clear();
        String day = (String) spDay.getSelectedItem();
        JSONArray bookings = load();
        for (String slot : allSlots) {
            boolean busy = false;
            // Проверяем, нет ли уже брони на этот ресторан + день + время
            for (int i = 0; i < bookings.length(); i++) {
                try {
                    JSONObject o = bookings.getJSONObject(i);
                    if (o.getString("restaurant").equals(restaurant)
                            && o.getString("day").equals(day)
                            && o.getString("time").equals(slot)) {
                        busy = true;
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!busy) freeSlots.add(slot); // слот свободен
        }
        timeAdapter.notifyDataSetChanged();
    }

    // Бронирует выбранный слот
    void book() {
        if (freeSlots.size() == 0 || spTime.getSelectedItem() == null) {
            Toast.makeText(this, "Нет свободного времени на этот день", Toast.LENGTH_SHORT).show();
            return;
        }
        String day = (String) spDay.getSelectedItem();
        String time = (String) spTime.getSelectedItem();
        JSONArray bookings = load();
        try {
            JSONObject o = new JSONObject();
            o.put("restaurant", restaurant);
            o.put("day", day);
            o.put("time", time);
            bookings.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        prefs.edit().putString("data", bookings.toString()).apply();
        Toast.makeText(this, "Столик забронирован!", Toast.LENGTH_SHORT).show();
        updateFreeSlots(); // обновляем — этот слот стал занятым
    }

    // Читает все брони из хранилища
    JSONArray load() {
        String s = prefs.getString("data", "[]");
        try {
            return new JSONArray(s);
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}
