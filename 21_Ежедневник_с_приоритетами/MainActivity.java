package com.example.diary;

// Импорты
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class MainActivity extends Activity {

    EditText etTask;
    Spinner spPriority;
    Button btnTime, btnAdd;
    ListView lvTasks;

    SharedPreferences prefs;
    // Задача: {text, priority(0..2), time}
    JSONArray tasks = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    int selHour = 9, selMin = 0;
    // Приоритеты: индекс 0 — высокий (важнее всего)
    String[] priorities = {"Высокий", "Средний", "Низкий"};

    static final String CHANNEL_ID = "diary_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etTask = (EditText) findViewById(R.id.etTask);
        spPriority = (Spinner) findViewById(R.id.spPriority);
        btnTime = (Button) findViewById(R.id.btnTime);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        lvTasks = (ListView) findViewById(R.id.lvTasks);

        prefs = getSharedPreferences("diary", MODE_PRIVATE);
        load();
        createChannel();
        askNotificationPermission();

        spPriority.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, priorities));

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines);
        lvTasks.setAdapter(adapter);
        refresh();

        // Выбор времени
        btnTime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            public void onTimeSet(TimePicker view, int h, int m) {
                                selHour = h; selMin = m;
                                btnTime.setText(String.format(Locale.getDefault(),
                                        "Время: %02d:%02d", h, m));
                            }
                        }, selHour, selMin, true).show();
            }
        });

        // Добавить задачу
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addTask();
            }
        });

        // Долгое нажатие — удалить (по совпадению текста, т.к. список отсортирован)
        lvTasks.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                deleteByText(lines.get(pos));
                return true;
            }
        });
    }

    // Добавляет задачу
    void addTask() {
        String t = etTask.getText().toString().trim();
        if (t.length() == 0) {
            Toast.makeText(this, "Введите задачу", Toast.LENGTH_SHORT).show();
            return;
        }
        String time = String.format(Locale.getDefault(), "%02d:%02d", selHour, selMin);
        int pr = spPriority.getSelectedItemPosition();
        try {
            JSONObject o = new JSONObject();
            o.put("text", t);
            o.put("priority", pr);
            o.put("time", time);
            tasks.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        etTask.setText("");
        save();
        refresh();
        showNotification("Напоминание на " + time, priorities[pr] + " приоритет: " + t);
    }

    // Пересобирает список, ОТСОРТИРОВАННЫЙ по приоритету (сначала высокий)
    void refresh() {
        // Собираем задачи в обычный список объектов, чтобы отсортировать
        ArrayList<JSONObject> items = new ArrayList<JSONObject>();
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject o = tasks.optJSONObject(i);
            if (o != null) items.add(o);
        }
        // Сортируем по полю priority (0 — высокий — идёт первым)
        Collections.sort(items, new Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                return a.optInt("priority") - b.optInt("priority");
            }
        });

        lines.clear();
        for (JSONObject o : items) {
            lines.add("[" + priorities[o.optInt("priority")] + "] "
                    + o.optString("time") + "  " + o.optString("text"));
        }
        adapter.notifyDataSetChanged();
    }

    // Удаляет задачу, чья отображаемая строка совпадает
    void deleteByText(String line) {
        for (int i = 0; i < tasks.length(); i++) {
            JSONObject o = tasks.optJSONObject(i);
            if (o == null) continue;
            String s = "[" + priorities[o.optInt("priority")] + "] "
                    + o.optString("time") + "  " + o.optString("text");
            if (s.equals(line)) {
                tasks.remove(i);
                break;
            }
        }
        save();
        refresh();
    }

    // Создаёт канал уведомлений (обязательно с Android 8.0 / API 26)
    void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "Ежедневник", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    // Запрашивает разрешение на уведомления (нужно с Android 13 / API 33)
    void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
            }
        }
    }

    // Показывает уведомление на экране
    void showNotification(String title, String text) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify((int) System.currentTimeMillis(), b.build());
    }

    // Сохраняет задачи в SharedPreferences
    void save() {
        prefs.edit().putString("data", tasks.toString()).apply();
    }

    // Загружает задачи при запуске
    void load() {
        String s = prefs.getString("data", "[]");
        try {
            tasks = new JSONArray(s);
        } catch (Exception e) {
            tasks = new JSONArray();
        }
    }
}
