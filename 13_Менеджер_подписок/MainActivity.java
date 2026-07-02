package com.example.subs;

// Импорты
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    EditText etName, etCost;
    Button btnDate, btnAdd;
    Spinner spPeriod;
    TextView tvTotal;
    ListView lvSubs;

    SharedPreferences prefs;
    // Подписка: {name, cost, date:"гггг-мм-дд", period:"Месяц"/"Год"}
    JSONArray subs = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    int selY, selM, selD; // выбранная дата списания

    static final String CHANNEL_ID = "subs_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = (EditText) findViewById(R.id.etName);
        etCost = (EditText) findViewById(R.id.etCost);
        btnDate = (Button) findViewById(R.id.btnDate);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        spPeriod = (Spinner) findViewById(R.id.spPeriod);
        tvTotal = (TextView) findViewById(R.id.tvTotal);
        lvSubs = (ListView) findViewById(R.id.lvSubs);

        prefs = getSharedPreferences("subs", MODE_PRIVATE);
        load();
        createChannel();
        askNotificationPermission();

        // Заполняем Spinner периодами
        String[] periods = {"Месяц", "Год"};
        spPeriod.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, periods));

        // По умолчанию дата списания = сегодня
        Calendar now = Calendar.getInstance();
        selY = now.get(Calendar.YEAR);
        selM = now.get(Calendar.MONTH);
        selD = now.get(Calendar.DAY_OF_MONTH);
        btnDate.setText(dateStr());

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines);
        lvSubs.setAdapter(adapter);
        refresh();
        checkUpcoming(); // проверяем ближайшие списания и шлём уведомление

        // Выбор даты списания
        btnDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new DatePickerDialog(MainActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            public void onDateSet(DatePicker view, int y, int m, int d) {
                                selY = y; selM = m; selD = d;
                                btnDate.setText(dateStr());
                            }
                        }, selY, selM, selD).show();
            }
        });

        // Добавить подписку
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addSub();
            }
        });

        // Долгое нажатие — удалить
        lvSubs.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                subs.remove(pos);
                save();
                refresh();
                return true;
            }
        });
    }

    // Дата списания в виде строки "гггг-мм-дд"
    String dateStr() {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", selY, selM + 1, selD);
    }

    // Добавляет новую подписку
    void addSub() {
        String n = etName.getText().toString().trim();
        String c = etCost.getText().toString().trim();
        if (n.length() == 0 || c.length() == 0) {
            Toast.makeText(this, "Введите название и стоимость", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("name", n);
            o.put("cost", Double.parseDouble(c));
            o.put("date", dateStr());
            o.put("period", spPeriod.getSelectedItem().toString());
            subs.put(o);
        } catch (Exception e) {
            Toast.makeText(this, "Неверная стоимость", Toast.LENGTH_SHORT).show();
            return;
        }
        etName.setText("");
        etCost.setText("");
        save();
        refresh();
    }

    // Обновляет список и считает суммарные траты в месяц/год
    void refresh() {
        lines.clear();
        double perMonth = 0, perYear = 0;
        for (int i = 0; i < subs.length(); i++) {
            try {
                JSONObject o = subs.getJSONObject(i);
                double cost = o.getDouble("cost");
                String period = o.getString("period");
                lines.add(o.getString("name") + " — " + cost + " руб. / " + period
                        + "\nСледующее списание: " + o.getString("date"));
                // Приводим всё к месячным и годовым тратам
                if (period.equals("Месяц")) {
                    perMonth += cost;
                    perYear += cost * 12;
                } else {
                    perMonth += cost / 12.0;
                    perYear += cost;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
        tvTotal.setText("В месяц: " + round(perMonth) + " руб.   В год: " + round(perYear) + " руб.");
    }

    // Округление до 2 знаков
    double round(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    // Проверяет подписки, у которых списание в ближайшие 3 дня, и шлёт уведомление
    void checkUpcoming() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            long now = System.currentTimeMillis();
            long threeDays = 3L * 24 * 60 * 60 * 1000; // 3 дня в миллисекундах
            for (int i = 0; i < subs.length(); i++) {
                JSONObject o = subs.getJSONObject(i);
                long date = sdf.parse(o.getString("date")).getTime();
                long diff = date - now;
                // если списание в промежутке от сейчас до +3 дней — предупреждаем
                if (diff >= 0 && diff <= threeDays) {
                    showNotification("Скоро списание: " + o.getString("name"),
                            o.getDouble("cost") + " руб. " + o.getString("date"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Создаёт канал уведомлений (обязательно с Android 8.0 / API 26)
    void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "Подписки", NotificationManager.IMPORTANCE_DEFAULT);
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

    // Сохраняет подписки в SharedPreferences
    void save() {
        prefs.edit().putString("data", subs.toString()).apply();
    }

    // Загружает подписки при запуске
    void load() {
        String s = prefs.getString("data", "[]");
        try {
            subs = new JSONArray(s);
        } catch (Exception e) {
            subs = new JSONArray();
        }
    }
}
