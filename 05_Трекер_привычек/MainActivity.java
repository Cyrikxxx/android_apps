package com.example.habits;

// Импорты
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    EditText etName;    // название новой привычки
    Button btnAdd;      // добавить привычку
    ListView lvHabits;  // список привычек

    SharedPreferences prefs;
    // Каждая привычка: {name, dates:[ "2026-07-01", ... ]} — даты, когда выполнено
    JSONArray habits = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>(); // строки для отображения
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = (EditText) findViewById(R.id.etName);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        lvHabits = (ListView) findViewById(R.id.lvHabits);

        prefs = getSharedPreferences("habits", MODE_PRIVATE);
        load();

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, lines);
        lvHabits.setAdapter(adapter);
        refresh();

        // Добавление новой привычки
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addHabit();
            }
        });

        // Тап по привычке — отметить/снять выполнение за сегодня
        lvHabits.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                toggleToday(pos);
            }
        });

        // Долгое нажатие — удалить привычку
        lvHabits.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                habits.remove(pos);
                save();
                refresh();
                return true;
            }
        });
    }

    // Возвращает сегодняшнюю дату в виде "гггг-мм-дд"
    String today() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }

    // Возвращает дату N дней назад в том же формате
    String dayAgo(int n) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -n); // сдвигаем дату назад
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(c.getTime());
    }

    // Добавляет новую привычку
    void addHabit() {
        String n = etName.getText().toString().trim();
        if (n.length() == 0) {
            Toast.makeText(this, "Введите название", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("name", n);
            o.put("dates", new JSONArray()); // пока ни одного выполнения
            habits.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        etName.setText("");
        save();
        refresh();
    }

    // Отмечает/снимает выполнение привычки за сегодня
    void toggleToday(int pos) {
        try {
            JSONObject o = habits.getJSONObject(pos);
            JSONArray dates = o.getJSONArray("dates");
            String t = today();
            int found = -1;
            // Ищем, есть ли уже сегодняшняя дата в списке
            for (int i = 0; i < dates.length(); i++) {
                if (dates.getString(i).equals(t)) {
                    found = i;
                    break;
                }
            }
            if (found >= 0) {
                dates.remove(found);    // была отметка — убираем
            } else {
                dates.put(t);           // не было — добавляем
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
        refresh();
    }

    // Пересобирает строки списка: название + процент за неделю + шкала 7 дней
    void refresh() {
        lines.clear();
        for (int i = 0; i < habits.length(); i++) {
            try {
                JSONObject o = habits.getJSONObject(i);
                JSONArray dates = o.getJSONArray("dates");

                // Строим шкалу за последние 7 дней (от 6 дней назад до сегодня)
                StringBuilder scale = new StringBuilder();
                int doneCount = 0;
                for (int d = 6; d >= 0; d--) {
                    String day = dayAgo(d);
                    if (contains(dates, day)) {
                        scale.append("✓");
                        doneCount++;
                    } else {
                        scale.append("·");
                    }
                }
                int percent = doneCount * 100 / 7; // процент выполнения за неделю
                lines.add(o.getString("name") + "\nНеделя: " + percent
                        + "%   " + scale.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Проверяет, есть ли строка day в массиве dates
    boolean contains(JSONArray dates, String day) {
        for (int i = 0; i < dates.length(); i++) {
            if (dates.optString(i).equals(day)) {
                return true;
            }
        }
        return false;
    }

    // Сохраняет привычки в SharedPreferences
    void save() {
        prefs.edit().putString("data", habits.toString()).apply();
    }

    // Загружает привычки при запуске
    void load() {
        String s = prefs.getString("data", "[]");
        try {
            habits = new JSONArray(s);
        } catch (Exception e) {
            habits = new JSONArray();
        }
    }
}
