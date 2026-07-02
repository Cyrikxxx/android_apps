package com.example.restaurant;

// Импорты
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class BookingsActivity extends Activity {

    ListView lvBookings;
    SharedPreferences prefs;
    JSONArray bookings = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookings);

        lvBookings = (ListView) findViewById(R.id.lvBookings);
        prefs = getSharedPreferences("booking", MODE_PRIVATE);
        load();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines);
        lvBookings.setAdapter(adapter);
        refresh();

        // Долгое нажатие — отменить бронь
        lvBookings.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                bookings.remove(pos);
                prefs.edit().putString("data", bookings.toString()).apply();
                refresh();
                Toast.makeText(BookingsActivity.this, "Бронь отменена", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    // Обновляет список бронирований
    void refresh() {
        lines.clear();
        for (int i = 0; i < bookings.length(); i++) {
            try {
                JSONObject o = bookings.getJSONObject(i);
                lines.add(o.getString("restaurant") + "\n"
                        + o.getString("day") + " в " + o.getString("time"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Загружает брони при запуске
    void load() {
        String s = prefs.getString("data", "[]");
        try {
            bookings = new JSONArray(s);
        } catch (Exception e) {
            bookings = new JSONArray();
        }
    }
}
