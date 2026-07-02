package com.example.schedule;

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
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {

    Spinner spDay;                  // день недели для новой пары
    EditText etSubject, etTime, etRoom, etTeacher;
    Button btnAdd;
    Spinner spFilter;               // день недели для просмотра
    ListView lvLessons;

    SharedPreferences prefs;
    // Пара: {day, subject, time, room, teacher}
    JSONArray lessons = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    String[] daysOfWeek = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spDay = (Spinner) findViewById(R.id.spDay);
        etSubject = (EditText) findViewById(R.id.etSubject);
        etTime = (EditText) findViewById(R.id.etTime);
        etRoom = (EditText) findViewById(R.id.etRoom);
        etTeacher = (EditText) findViewById(R.id.etTeacher);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        spFilter = (Spinner) findViewById(R.id.spFilter);
        lvLessons = (ListView) findViewById(R.id.lvLessons);

        prefs = getSharedPreferences("schedule", MODE_PRIVATE);
        load();

        // Оба Spinner заполняем днями недели
        ArrayAdapter<String> daysAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, daysOfWeek);
        spDay.setAdapter(daysAdapter);
        spFilter.setAdapter(daysAdapter);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines);
        lvLessons.setAdapter(adapter);
        refresh();

        // Добавить пару
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addLesson();
            }
        });

        // При смене дня в фильтре обновляем список
        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refresh();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Долгое нажатие — удалить пару
        lvLessons.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                deleteByVisiblePos(pos);
                return true;
            }
        });
    }

    // Добавляет пару в расписание
    void addLesson() {
        String subject = etSubject.getText().toString().trim();
        if (subject.length() == 0) return;
        try {
            JSONObject o = new JSONObject();
            o.put("day", spDay.getSelectedItem().toString());
            o.put("subject", subject);
            o.put("time", etTime.getText().toString().trim());
            o.put("room", etRoom.getText().toString().trim());
            o.put("teacher", etTeacher.getText().toString().trim());
            lessons.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Очищаем поля (день оставляем)
        etSubject.setText("");
        etTime.setText("");
        etRoom.setText("");
        etTeacher.setText("");
        save();
        refresh();
    }

    // Показывает пары только выбранного в фильтре дня
    void refresh() {
        lines.clear();
        String day = spFilter.getSelectedItem() == null ? daysOfWeek[0]
                : spFilter.getSelectedItem().toString();
        for (int i = 0; i < lessons.length(); i++) {
            try {
                JSONObject o = lessons.getJSONObject(i);
                if (o.getString("day").equals(day)) {
                    lines.add(o.getString("time") + "  " + o.getString("subject")
                            + "\nауд. " + o.getString("room") + ", " + o.getString("teacher"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Удаляет пару по позиции в отфильтрованном списке
    void deleteByVisiblePos(int visiblePos) {
        String day = spFilter.getSelectedItem().toString();
        int count = -1;
        for (int i = 0; i < lessons.length(); i++) {
            try {
                if (lessons.getJSONObject(i).getString("day").equals(day)) {
                    count++;
                    if (count == visiblePos) {
                        lessons.remove(i);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        save();
        refresh();
    }

    // Сохраняет расписание в SharedPreferences
    void save() {
        prefs.edit().putString("data", lessons.toString()).apply();
    }

    // Загружает расписание при запуске
    void load() {
        String s = prefs.getString("data", "[]");
        try {
            lessons = new JSONArray(s);
        } catch (Exception e) {
            lessons = new JSONArray();
        }
    }
}
