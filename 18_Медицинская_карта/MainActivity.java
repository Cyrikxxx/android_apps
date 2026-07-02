package com.example.medcard;

// Импорты
import android.app.Activity;
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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {

    // Лекарства
    EditText etMedName, etDosage, etTime;
    Button btnAddMed;
    ListView lvMeds;
    // Аллергии / хронические
    EditText etAllergy;
    Button btnAddAllergy;
    ListView lvAllergies;

    SharedPreferences prefs;
    JSONArray meds = new JSONArray();       // {name, dosage, time}
    JSONArray allergies = new JSONArray();  // строки

    ArrayList<String> medLines = new ArrayList<String>();
    ArrayList<String> allergyLines = new ArrayList<String>();
    ArrayAdapter<String> medAdapter, allergyAdapter;

    static final String CHANNEL_ID = "med_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Находим элементы интерфейса по id
        etMedName = (EditText) findViewById(R.id.etMedName);
        etDosage = (EditText) findViewById(R.id.etDosage);
        etTime = (EditText) findViewById(R.id.etTime);
        btnAddMed = (Button) findViewById(R.id.btnAddMed);
        lvMeds = (ListView) findViewById(R.id.lvMeds);
        etAllergy = (EditText) findViewById(R.id.etAllergy);
        btnAddAllergy = (Button) findViewById(R.id.btnAddAllergy);
        lvAllergies = (ListView) findViewById(R.id.lvAllergies);

        // Загружаем данные, готовим канал и разрешение на уведомления
        prefs = getSharedPreferences("med", MODE_PRIVATE);
        load();
        createChannel();
        askNotificationPermission();

        // Адаптеры для двух списков (лекарства и аллергии)
        medAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, medLines);
        allergyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, allergyLines);
        lvMeds.setAdapter(medAdapter);
        lvAllergies.setAdapter(allergyAdapter);
        refresh();

        // Добавить лекарство
        btnAddMed.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addMed();
            }
        });

        // Добавить аллергию/хроническое
        btnAddAllergy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String a = etAllergy.getText().toString().trim();
                if (a.length() == 0) return;
                allergies.put(a);
                etAllergy.setText("");
                save();
                refresh();
            }
        });

        // Долгое нажатие — удалить лекарство
        lvMeds.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                meds.remove(pos);
                save();
                refresh();
                return true;
            }
        });

        // Долгое нажатие — удалить аллергию
        lvAllergies.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                allergies.remove(pos);
                save();
                refresh();
                return true;
            }
        });
    }

    // Добавляет лекарство и показывает уведомление-напоминание
    void addMed() {
        String name = etMedName.getText().toString().trim();
        if (name.length() == 0) {
            Toast.makeText(this, "Введите название лекарства", Toast.LENGTH_SHORT).show();
            return;
        }
        String dosage = etDosage.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        try {
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("dosage", dosage);
            o.put("time", time);
            meds.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        etMedName.setText("");
        etDosage.setText("");
        etTime.setText("");
        save();
        refresh();
        showNotification("Приём лекарства: " + name, "Доза: " + dosage + ", время: " + time);
    }

    // Обновляет оба списка
    void refresh() {
        medLines.clear();
        for (int i = 0; i < meds.length(); i++) {
            try {
                JSONObject o = meds.getJSONObject(i);
                medLines.add(o.getString("name") + " — " + o.getString("dosage")
                        + " в " + o.getString("time"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        allergyLines.clear();
        for (int i = 0; i < allergies.length(); i++) {
            allergyLines.add(allergies.optString(i));
        }
        medAdapter.notifyDataSetChanged();
        allergyAdapter.notifyDataSetChanged();
    }

    // Создаёт канал уведомлений (обязательно с Android 8.0 / API 26)
    void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "Лекарства", NotificationManager.IMPORTANCE_DEFAULT);
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

    // Сохраняет оба списка (лекарства и аллергии) в SharedPreferences
    void save() {
        prefs.edit()
                .putString("meds", meds.toString())
                .putString("allergies", allergies.toString())
                .apply();
    }

    // Загружает оба списка при запуске
    void load() {
        try {
            meds = new JSONArray(prefs.getString("meds", "[]"));
            allergies = new JSONArray(prefs.getString("allergies", "[]"));
        } catch (Exception e) {
            meds = new JSONArray();
            allergies = new JSONArray();
        }
    }
}
