package com.example.water;

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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    EditText etWeight, etAge;
    Spinner spActivity;
    Button btnCalc, btnGlass;
    TextView tvNorm, tvProgress;
    ListView lvWeek;

    SharedPreferences prefs;
    int norm = 0;   // дневная норма в мл
    // Выпитое по дням храним в prefs по ключу "ml_гггг-мм-дд"

    ArrayList<String> weekLines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    String[] activityLevels = {"Низкая", "Средняя", "Высокая"};
    // Прибавка к норме в зависимости от активности (мл)
    int[] activityBonus = {0, 350, 700};

    static final String CHANNEL_ID = "water_channel";
    static final int GLASS = 250; // объём стакана

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etWeight = (EditText) findViewById(R.id.etWeight);
        etAge = (EditText) findViewById(R.id.etAge);
        spActivity = (Spinner) findViewById(R.id.spActivity);
        btnCalc = (Button) findViewById(R.id.btnCalc);
        btnGlass = (Button) findViewById(R.id.btnGlass);
        tvNorm = (TextView) findViewById(R.id.tvNorm);
        tvProgress = (TextView) findViewById(R.id.tvProgress);
        lvWeek = (ListView) findViewById(R.id.lvWeek);

        prefs = getSharedPreferences("water", MODE_PRIVATE);
        norm = prefs.getInt("norm", 0); // ранее посчитанная норма
        createChannel();
        askNotificationPermission();

        spActivity.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, activityLevels));

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, weekLines);
        lvWeek.setAdapter(adapter);

        if (norm > 0) tvNorm.setText("Ваша норма: " + norm + " мл");
        updateProgress();
        updateWeek();

        // Рассчитать норму
        btnCalc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                calcNorm();
            }
        });

        // Отметить выпитый стакан
        btnGlass.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                drinkGlass();
            }
        });
    }

    // Возвращает сегодняшнюю дату в виде "гггг-мм-дд"
    String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
    }

    // Считает дневную норму воды
    void calcNorm() {
        double weight = parse(etWeight.getText().toString());
        if (weight <= 0) {
            Toast.makeText(this, "Введите вес", Toast.LENGTH_SHORT).show();
            return;
        }
        // Базовая формула: 30 мл на 1 кг веса + прибавка за активность
        int bonus = activityBonus[spActivity.getSelectedItemPosition()];
        norm = (int) (weight * 30) + bonus;
        prefs.edit().putInt("norm", norm).apply();
        tvNorm.setText("Ваша норма: " + norm + " мл");
        updateProgress();
    }

    // Добавляет один стакан к сегодняшнему объёму
    void drinkGlass() {
        if (norm <= 0) {
            Toast.makeText(this, "Сначала рассчитайте норму", Toast.LENGTH_SHORT).show();
            return;
        }
        String key = "ml_" + today();
        int drunk = prefs.getInt(key, 0) + GLASS;
        prefs.edit().putInt(key, drunk).apply();
        updateProgress();
        updateWeek();
        // Напоминание "пить воду" (демонстрация; реальные напоминания
        // каждые 2 часа нужно планировать через AlarmManager — см. README)
        showNotification("Не забывайте пить воду!", "Выпито сегодня: " + drunk + " мл");
    }

    // Обновляет прогресс за сегодня
    void updateProgress() {
        int drunk = prefs.getInt("ml_" + today(), 0);
        int percent = norm > 0 ? drunk * 100 / norm : 0;
        tvProgress.setText("Сегодня выпито: " + drunk + " мл из " + norm
                + " мл (" + percent + "%)");
    }

    // Показывает статистику за последние 7 дней
    void updateWeek() {
        weekLines.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar c = Calendar.getInstance();
        for (int d = 0; d < 7; d++) {
            String day = sdf.format(c.getTime());
            int ml = prefs.getInt("ml_" + day, 0);
            weekLines.add(day + ": " + ml + " мл");
            c.add(Calendar.DAY_OF_YEAR, -1); // на день назад
        }
        adapter.notifyDataSetChanged();
    }

    // Создаёт канал уведомлений (обязательно с Android 8.0 / API 26)
    void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID,
                    "Вода", NotificationManager.IMPORTANCE_DEFAULT);
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
        nm.notify(1, b.build());
    }

    // Безопасно превращает текст в число (пустая строка -> 0)
    double parse(String s) {
        s = s.trim();
        if (s.length() == 0) return 0;
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0;
        }
    }
}
