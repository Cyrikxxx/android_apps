package com.example.planner;

// Импорты
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    CalendarView calendar;  // календарь для выбора даты
    EditText etTask;        // текст задачи
    Button btnTime;         // выбор времени
    Button btnAdd;          // добавить задачу
    TextView tvDate;        // подпись выбранной даты
    ListView lvTasks;       // задачи выбранного дня

    SharedPreferences prefs;
    // Задачи: {date:"гггг-мм-дд", time:"чч:мм", text}
    JSONArray tasks = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    String selectedDate;    // выбранная в календаре дата
    int selHour = 9, selMin = 0; // выбранное время (по умолчанию 9:00)

    static final String CHANNEL_ID = "planner_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calendar = (CalendarView) findViewById(R.id.calendar);
        etTask = (EditText) findViewById(R.id.etTask);
        btnTime = (Button) findViewById(R.id.btnTime);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        tvDate = (TextView) findViewById(R.id.tvDate);
        lvTasks = (ListView) findViewById(R.id.lvTasks);

        prefs = getSharedPreferences("planner", MODE_PRIVATE);
        load();

        // Создаём канал уведомлений (нужно с Android 8.0)
        createChannel();
        // Запрашиваем разрешение на уведомления (нужно с Android 13)
        askNotificationPermission();

        // Изначально выбрана сегодняшняя дата
        selectedDate = dateToStr(calendar.getDate());
        tvDate.setText("Задачи на: " + selectedDate);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, lines);
        lvTasks.setAdapter(adapter);
        refresh();

        // При выборе дня в календаре запоминаем дату и обновляем список
        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            public void onSelectedDayChange(CalendarView view, int year, int month, int day) {
                // month здесь 0-11, добавляем 1 для читаемого вида
                selectedDate = String.format(Locale.getDefault(),
                        "%04d-%02d-%02d", year, month + 1, day);
                tvDate.setText("Задачи на: " + selectedDate);
                refresh();
            }
        });

        // Выбор времени задачи
        btnTime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TimePickerDialog dlg = new TimePickerDialog(MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            public void onTimeSet(TimePicker view, int h, int m) {
                                selHour = h;
                                selMin = m;
                                btnTime.setText(String.format(Locale.getDefault(),
                                        "Время: %02d:%02d", h, m));
                            }
                        }, selHour, selMin, true);
                dlg.show();
            }
        });

        // Добавить задачу
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addTask();
            }
        });

        // Долгое нажатие — удалить задачу выбранного дня
        lvTasks.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                deleteByVisiblePos(pos);
                return true;
            }
        });
    }

    // Превращает миллисекунды (calendar.getDate()) в строку "гггг-мм-дд"
    String dateToStr(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(millis);
    }

    // Добавляет новую задачу на выбранную дату и время
    void addTask() {
        String t = etTask.getText().toString().trim();
        if (t.length() == 0) {
            Toast.makeText(this, "Введите текст задачи", Toast.LENGTH_SHORT).show();
            return;
        }
        String time = String.format(Locale.getDefault(), "%02d:%02d", selHour, selMin);
        try {
            JSONObject o = new JSONObject();
            o.put("date", selectedDate);
            o.put("time", time);
            o.put("text", t);
            tasks.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        etTask.setText("");
        save();
        refresh();
        // Показываем уведомление-напоминание об этой задаче
        showNotification("Новая задача на " + selectedDate + " " + time, t);
    }

    // Обновляет список задач ТОЛЬКО выбранного дня
    void refresh() {
        lines.clear();
        for (int i = 0; i < tasks.length(); i++) {
            try {
                JSONObject o = tasks.getJSONObject(i);
                if (o.getString("date").equals(selectedDate)) {
                    lines.add(o.getString("time") + "  " + o.getString("text"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Удаляет задачу по позиции в видимом (отфильтрованном) списке
    void deleteByVisiblePos(int visiblePos) {
        int count = -1;
        for (int i = 0; i < tasks.length(); i++) {
            try {
                JSONObject o = tasks.getJSONObject(i);
                if (o.getString("date").equals(selectedDate)) {
                    count++;
                    if (count == visiblePos) {
                        tasks.remove(i); // нашли нужную — удаляем из общего массива
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

    // Создаёт канал уведомлений (обязательно с Android 8.0 / API 26)
    void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "Напоминания", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    // Запрашивает разрешение на показ уведомлений (нужно с Android 13 / API 33)
    void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
            }
        }
    }

    // Показывает уведомление
    void showNotification(String title, String text) {
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // id уведомления делаем разным, чтобы они не затирали друг друга
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
