package com.example.workout;

// Импорты
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    EditText etExercise, etSets, etReps, etWeight;
    Button btnAdd, btnRest;
    TextView tvTimer;
    ListView lvHistory;
    ImageView ivGraph;

    SharedPreferences prefs;
    // Запись тренировки: {exercise, sets, reps, weight, date}
    JSONArray log = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    CountDownTimer restTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etExercise = (EditText) findViewById(R.id.etExercise);
        etSets = (EditText) findViewById(R.id.etSets);
        etReps = (EditText) findViewById(R.id.etReps);
        etWeight = (EditText) findViewById(R.id.etWeight);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        btnRest = (Button) findViewById(R.id.btnRest);
        tvTimer = (TextView) findViewById(R.id.tvTimer);
        lvHistory = (ListView) findViewById(R.id.lvHistory);
        ivGraph = (ImageView) findViewById(R.id.ivGraph);

        prefs = getSharedPreferences("workout", MODE_PRIVATE);
        load();

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines);
        lvHistory.setAdapter(adapter);
        refresh();

        // Добавить запись тренировки
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addEntry();
            }
        });

        // Таймер отдыха между подходами (60 секунд)
        btnRest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startRest();
            }
        });

        // Долгое нажатие — удалить запись
        lvHistory.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                log.remove(pos);
                save();
                refresh();
                return true;
            }
        });
    }

    // Добавляет запись выполненного упражнения
    void addEntry() {
        String ex = etExercise.getText().toString().trim();
        if (ex.length() == 0) {
            Toast.makeText(this, "Введите упражнение", Toast.LENGTH_SHORT).show();
            return;
        }
        String date = new SimpleDateFormat("dd.MM", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        try {
            JSONObject o = new JSONObject();
            o.put("exercise", ex);
            o.put("sets", parseInt(etSets.getText().toString()));
            o.put("reps", parseInt(etReps.getText().toString()));
            o.put("weight", parseInt(etWeight.getText().toString()));
            o.put("date", date);
            log.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        etWeight.setText("");
        save();
        refresh();
    }

    // Безопасно читает целое число из текста
    int parseInt(String s) {
        s = s.trim();
        if (s.length() == 0) return 0;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    // Обновляет список истории и перерисовывает график
    void refresh() {
        lines.clear();
        for (int i = 0; i < log.length(); i++) {
            try {
                JSONObject o = log.getJSONObject(i);
                lines.add(o.getString("date") + "  " + o.getString("exercise")
                        + ": " + o.getInt("sets") + "x" + o.getInt("reps")
                        + " по " + o.getInt("weight") + " кг");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
        drawGraph();
    }

    // Рисует простой линейный график веса по всем записям (на Bitmap)
    void drawGraph() {
        int w = 600, h = 300;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);

        Paint axis = new Paint();
        axis.setColor(Color.GRAY);
        // Рисуем оси
        canvas.drawLine(40, 10, 40, h - 30, axis);       // ось Y
        canvas.drawLine(40, h - 30, w - 10, h - 30, axis); // ось X

        int n = log.length();
        if (n < 2) return; // для линии нужно минимум 2 точки

        // Ищем максимальный вес для масштаба
        int maxW = 1;
        for (int i = 0; i < n; i++) {
            int wt = log.optJSONObject(i).optInt("weight", 0);
            if (wt > maxW) maxW = wt;
        }

        Paint line = new Paint();
        line.setColor(Color.rgb(33, 150, 243));
        line.setStrokeWidth(4);

        // Рисуем линию между соседними точками веса
        float stepX = (float) (w - 60) / (n - 1);
        for (int i = 0; i < n - 1; i++) {
            int w1 = log.optJSONObject(i).optInt("weight", 0);
            int w2 = log.optJSONObject(i + 1).optInt("weight", 0);
            float x1 = 40 + stepX * i;
            float x2 = 40 + stepX * (i + 1);
            float y1 = (h - 30) - (float) w1 / maxW * (h - 60);
            float y2 = (h - 30) - (float) w2 / maxW * (h - 60);
            canvas.drawLine(x1, y1, x2, y2, line);
        }
        ivGraph.setImageBitmap(bmp);
    }

    // Запускает таймер отдыха на 60 секунд
    void startRest() {
        if (restTimer != null) restTimer.cancel(); // сбросить старый, если был
        restTimer = new CountDownTimer(60000, 1000) {
            public void onTick(long millis) {
                tvTimer.setText("Отдых: " + (millis / 1000) + " сек");
            }
            public void onFinish() {
                tvTimer.setText("Отдых окончен — вперёд!");
            }
        };
        restTimer.start();
    }

    // Сохраняет журнал тренировок в SharedPreferences
    void save() {
        prefs.edit().putString("data", log.toString()).apply();
    }

    // Загружает журнал при запуске
    void load() {
        String s = prefs.getString("data", "[]");
        try {
            log = new JSONArray(s);
        } catch (Exception e) {
            log = new JSONArray();
        }
    }
}
