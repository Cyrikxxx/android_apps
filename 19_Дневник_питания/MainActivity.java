package com.example.calories;

// Импорты
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    Spinner spFood;
    EditText etGrams;
    Button btnAdd;
    TextView tvTotals;
    ListView lvMeals;
    EditText etWeight;
    Button btnWeight;
    ImageView ivGraph;

    SharedPreferences prefs;
    JSONArray meals = new JSONArray();    // {name, grams, kcal, p, f, c, date}
    JSONArray weights = new JSONArray();  // {date, weight}

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    // Мини-база продуктов (на 100 г): название, ккал, белки, жиры, углеводы
    String[] foodNames = {"Куриная грудка", "Рис", "Яйцо", "Банан", "Овсянка", "Творог"};
    int[] foodKcal = {113, 130, 155, 89, 88, 121};
    double[] foodP = {23.6, 2.7, 12.7, 1.1, 3.0, 18.0};
    double[] foodF = {1.9, 0.3, 10.9, 0.3, 1.7, 5.0};
    double[] foodC = {0.4, 28.0, 0.7, 22.8, 15.0, 3.3};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spFood = (Spinner) findViewById(R.id.spFood);
        etGrams = (EditText) findViewById(R.id.etGrams);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        tvTotals = (TextView) findViewById(R.id.tvTotals);
        lvMeals = (ListView) findViewById(R.id.lvMeals);
        etWeight = (EditText) findViewById(R.id.etWeight);
        btnWeight = (Button) findViewById(R.id.btnWeight);
        ivGraph = (ImageView) findViewById(R.id.ivGraph);

        prefs = getSharedPreferences("calories", MODE_PRIVATE);
        load();

        spFood.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, foodNames));

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines);
        lvMeals.setAdapter(adapter);
        refresh();
        drawGraph();

        // Добавить съеденный продукт
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addMeal();
            }
        });

        // Записать вес
        btnWeight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addWeight();
            }
        });

        // Долгое нажатие — удалить запись еды
        lvMeals.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                deleteTodayMeal(pos);
                return true;
            }
        });
    }

    // Возвращает сегодняшнюю дату в виде "гггг-мм-дд"
    String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
    }

    // Добавляет съеденный продукт с пересчётом на введённые граммы
    void addMeal() {
        double grams = parse(etGrams.getText().toString());
        if (grams <= 0) {
            Toast.makeText(this, "Введите граммы", Toast.LENGTH_SHORT).show();
            return;
        }
        int idx = spFood.getSelectedItemPosition();
        double k = grams / 100.0; // множитель относительно 100 г
        try {
            JSONObject o = new JSONObject();
            o.put("name", foodNames[idx]);
            o.put("grams", grams);
            o.put("kcal", foodKcal[idx] * k);
            o.put("p", foodP[idx] * k);
            o.put("f", foodF[idx] * k);
            o.put("c", foodC[idx] * k);
            o.put("date", today());
            meals.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        etGrams.setText("");
        save();
        refresh();
    }

    // Обновляет список еды за сегодня и считает сумму БЖУ и калорий
    void refresh() {
        lines.clear();
        double kcal = 0, p = 0, f = 0, c = 0;
        String t = today();
        for (int i = 0; i < meals.length(); i++) {
            try {
                JSONObject o = meals.getJSONObject(i);
                if (o.getString("date").equals(t)) {
                    lines.add(o.getString("name") + " " + (int) o.getDouble("grams") + " г — "
                            + round(o.getDouble("kcal")) + " ккал");
                    kcal += o.getDouble("kcal");
                    p += o.getDouble("p");
                    f += o.getDouble("f");
                    c += o.getDouble("c");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
        tvTotals.setText("За сегодня: " + round(kcal) + " ккал\n"
                + "Белки: " + round(p) + " г, Жиры: " + round(f) + " г, Углеводы: " + round(c) + " г");
    }

    // Удаляет запись еды по позиции в списке сегодняшнего дня
    void deleteTodayMeal(int visiblePos) {
        String t = today();
        int count = -1;
        for (int i = 0; i < meals.length(); i++) {
            try {
                if (meals.getJSONObject(i).getString("date").equals(t)) {
                    count++;
                    if (count == visiblePos) {
                        meals.remove(i);
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

    // Записывает вес на сегодня и перерисовывает график
    void addWeight() {
        double w = parse(etWeight.getText().toString());
        if (w <= 0) {
            Toast.makeText(this, "Введите вес", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("date", today());
            o.put("weight", w);
            weights.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        etWeight.setText("");
        save();
        drawGraph();
        Toast.makeText(this, "Вес записан", Toast.LENGTH_SHORT).show();
    }

    // Рисует график изменения веса (линия) на Bitmap
    void drawGraph() {
        int w = 600, h = 300;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        Paint axis = new Paint();
        axis.setColor(Color.GRAY);
        canvas.drawLine(40, 10, 40, h - 30, axis);
        canvas.drawLine(40, h - 30, w - 10, h - 30, axis);

        int n = weights.length();
        if (n < 2) {
            ivGraph.setImageBitmap(bmp);
            return;
        }
        double maxW = 1;
        for (int i = 0; i < n; i++) {
            double wt = weights.optJSONObject(i).optDouble("weight", 0);
            if (wt > maxW) maxW = wt;
        }
        Paint line = new Paint();
        line.setColor(Color.rgb(76, 175, 80));
        line.setStrokeWidth(4);
        float stepX = (float) (w - 60) / (n - 1);
        for (int i = 0; i < n - 1; i++) {
            double w1 = weights.optJSONObject(i).optDouble("weight", 0);
            double w2 = weights.optJSONObject(i + 1).optDouble("weight", 0);
            float x1 = 40 + stepX * i, x2 = 40 + stepX * (i + 1);
            float y1 = (float) ((h - 30) - w1 / maxW * (h - 60));
            float y2 = (float) ((h - 30) - w2 / maxW * (h - 60));
            canvas.drawLine(x1, y1, x2, y2, line);
        }
        ivGraph.setImageBitmap(bmp);
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

    // Округление до 1 знака
    double round(double x) {
        return Math.round(x * 10.0) / 10.0;
    }

    // Сохраняет еду и вес в SharedPreferences
    void save() {
        prefs.edit()
                .putString("meals", meals.toString())
                .putString("weights", weights.toString())
                .apply();
    }

    // Загружает еду и вес при запуске
    void load() {
        try {
            meals = new JSONArray(prefs.getString("meals", "[]"));
            weights = new JSONArray(prefs.getString("weights", "[]"));
        } catch (Exception e) {
            meals = new JSONArray();
            weights = new JSONArray();
        }
    }
}
