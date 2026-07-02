package com.example.trips;

// Импорты
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    // Элементы интерфейса
    EditText etCity, etGoal;
    Button btnStart, btnEnd, btnCreate, btnDeleteTrip;
    Spinner spTrip;         // выбор поездки
    ListView lvTasks;       // задачи выбранной поездки

    SharedPreferences prefs;
    // Поездка: {city, start, end, goal, tasks:[{text, done}]}
    JSONArray trips = new JSONArray();

    ArrayList<String> tripTitles = new ArrayList<String>();  // для Spinner
    ArrayAdapter<String> tripAdapter;

    ArrayList<String> taskLines = new ArrayList<String>();   // для ListView задач
    ArrayAdapter<String> taskAdapter;

    String startDate = "", endDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Находим элементы интерфейса по id
        etCity = (EditText) findViewById(R.id.etCity);
        etGoal = (EditText) findViewById(R.id.etGoal);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnEnd = (Button) findViewById(R.id.btnEnd);
        btnCreate = (Button) findViewById(R.id.btnCreate);
        btnDeleteTrip = (Button) findViewById(R.id.btnDeleteTrip);
        spTrip = (Spinner) findViewById(R.id.spTrip);
        lvTasks = (ListView) findViewById(R.id.lvTasks);

        // Открываем хранилище и загружаем сохранённые поездки
        prefs = getSharedPreferences("trips", MODE_PRIVATE);
        load();

        // Адаптер для Spinner со списком поездок
        tripAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, tripTitles);
        spTrip.setAdapter(tripAdapter);

        // Адаптер для списка задач выбранной поездки
        taskAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, taskLines);
        lvTasks.setAdapter(taskAdapter);

        refreshTripList();

        // При выборе поездки в Spinner показываем её задачи
        spTrip.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshTasks();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Выбор дат
        btnStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickDate(true);
            }
        });
        btnEnd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickDate(false);
            }
        });

        // Создать поездку
        btnCreate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createTrip();
            }
        });

        // Удалить выбранную поездку
        btnDeleteTrip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                deleteTrip();
            }
        });

        // Тап по задаче — отметить выполненной / снять отметку
        lvTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                toggleTask(pos);
            }
        });
    }

    // Открывает календарь для выбора даты
    void pickDate(final boolean isStart) {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int y, int m, int d) {
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
                if (isStart) {
                    startDate = date;
                    btnStart.setText("С: " + date);
                } else {
                    endDate = date;
                    btnEnd.setText("По: " + date);
                }
            }
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }

    // Создаёт поездку и автоматически формирует список задач
    void createTrip() {
        String city = etCity.getText().toString().trim();
        String goal = etGoal.getText().toString().trim();
        if (city.length() == 0 || startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Введите город и даты", Toast.LENGTH_SHORT).show();
            return;
        }
        // Стандартный набор задач для командировки
        String[] autoTasks = {
                "Забронировать отель в " + city,
                "Купить билеты (туда и обратно)",
                "Оформить командировочное удостоверение",
                "Собрать документы и деньги",
                "Цель поездки: " + (goal.isEmpty() ? "уточнить" : goal)
        };
        try {
            JSONObject trip = new JSONObject();
            trip.put("city", city);
            trip.put("start", startDate);
            trip.put("end", endDate);
            trip.put("goal", goal);
            // Формируем массив задач, каждая — {text, done:false}
            JSONArray tasks = new JSONArray();
            for (String t : autoTasks) {
                JSONObject task = new JSONObject();
                task.put("text", t);
                task.put("done", false);
                tasks.put(task);
            }
            trip.put("tasks", tasks);
            trips.put(trip);
        } catch (Exception e) {
            e.printStackTrace();
        }
        etCity.setText("");
        etGoal.setText("");
        save();
        refreshTripList();
        // Выбираем только что созданную поездку
        spTrip.setSelection(tripTitles.size() - 1);
        Toast.makeText(this, "Поездка создана, задачи добавлены", Toast.LENGTH_SHORT).show();
    }

    // Удаляет выбранную поездку
    void deleteTrip() {
        int pos = spTrip.getSelectedItemPosition();
        if (pos < 0 || pos >= trips.length()) return;
        trips.remove(pos);
        save();
        refreshTripList();
    }

    // Отмечает задачу выполненной / снимает отметку
    void toggleTask(int taskPos) {
        int tripPos = spTrip.getSelectedItemPosition();
        if (tripPos < 0 || tripPos >= trips.length()) return;
        try {
            JSONObject trip = trips.getJSONObject(tripPos);
            JSONObject task = trip.getJSONArray("tasks").getJSONObject(taskPos);
            task.put("done", !task.getBoolean("done"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
        refreshTasks();
    }

    // Обновляет Spinner со списком поездок
    void refreshTripList() {
        tripTitles.clear();
        for (int i = 0; i < trips.length(); i++) {
            try {
                JSONObject t = trips.getJSONObject(i);
                tripTitles.add(t.getString("city") + " (" + t.getString("start")
                        + " — " + t.getString("end") + ")");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tripAdapter.notifyDataSetChanged();
        refreshTasks();
    }

    // Показывает задачи выбранной поездки (с галочкой ✓ у выполненных)
    void refreshTasks() {
        taskLines.clear();
        int pos = spTrip.getSelectedItemPosition();
        if (pos >= 0 && pos < trips.length()) {
            try {
                JSONArray tasks = trips.getJSONObject(pos).getJSONArray("tasks");
                for (int i = 0; i < tasks.length(); i++) {
                    JSONObject task = tasks.getJSONObject(i);
                    String mark = task.getBoolean("done") ? "✓ " : "☐ ";
                    taskLines.add(mark + task.getString("text"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        taskAdapter.notifyDataSetChanged();
    }

    // Сохраняет поездки в SharedPreferences
    void save() {
        prefs.edit().putString("data", trips.toString()).apply();
    }

    // Загружает поездки при запуске
    void load() {
        String s = prefs.getString("data", "[]");
        try {
            trips = new JSONArray(s);
        } catch (Exception e) {
            trips = new JSONArray();
        }
    }
}
