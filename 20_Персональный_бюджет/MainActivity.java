package com.example.budget;

// Импорты
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {

    Spinner spType, spCategory;
    EditText etAmount;
    Button btnAdd;
    TextView tvBalance;
    ListView lvTx;
    ImageView ivPie;

    SharedPreferences prefs;
    // Транзакция: {type:"Доход"/"Расход", category, amount, month}
    JSONArray tx = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    String[] types = {"Доход", "Расход"};
    String[] categories = {"Продукты", "Транспорт", "Развлечения", "Зарплата", "Прочее"};
    // Цвета для секторов круговой диаграммы (по категориям)
    int[] colors = {Color.rgb(244, 67, 54), Color.rgb(33, 150, 243),
            Color.rgb(255, 193, 7), Color.rgb(76, 175, 80), Color.rgb(156, 39, 176)};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spType = (Spinner) findViewById(R.id.spType);
        spCategory = (Spinner) findViewById(R.id.spCategory);
        etAmount = (EditText) findViewById(R.id.etAmount);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        tvBalance = (TextView) findViewById(R.id.tvBalance);
        lvTx = (ListView) findViewById(R.id.lvTx);
        ivPie = (ImageView) findViewById(R.id.ivPie);

        prefs = getSharedPreferences("budget", MODE_PRIVATE);
        load();

        spType.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, types));
        spCategory.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, categories));

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines);
        lvTx.setAdapter(adapter);
        refresh();

        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addTx();
            }
        });

        lvTx.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                tx.remove(pos);
                save();
                refresh();
                return true;
            }
        });
    }

    // Добавляет транзакцию
    void addTx() {
        double a = parse(etAmount.getText().toString());
        if (a <= 0) {
            Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("type", spType.getSelectedItem().toString());
            o.put("category", spCategory.getSelectedItem().toString());
            o.put("amount", a);
            o.put("month", Calendar.getInstance().get(Calendar.MONTH));
            tx.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        etAmount.setText("");
        save();
        refresh();
    }

    // Обновляет список, баланс и круговую диаграмму
    void refresh() {
        lines.clear();
        double income = 0, expense = 0;
        for (int i = 0; i < tx.length(); i++) {
            try {
                JSONObject o = tx.getJSONObject(i);
                double a = o.getDouble("amount");
                String type = o.getString("type");
                lines.add(type + ": " + o.getString("category") + " — " + a + " руб.");
                if (type.equals("Доход")) income += a;
                else expense += a;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
        tvBalance.setText("Баланс: " + round(income - expense) + " руб.\n"
                + "Доходы: " + round(income) + ", Расходы: " + round(expense));
        drawPie();
    }

    // Рисует круговую диаграмму РАСХОДОВ по категориям за текущий месяц
    void drawPie() {
        int size = 400;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);

        int month = Calendar.getInstance().get(Calendar.MONTH);

        // Считаем сумму расходов по каждой категории
        // LinkedHashMap хранит категории в том же порядке, что и массив categories
        Map<String, Double> sums = new LinkedHashMap<String, Double>();
        for (String c : categories) sums.put(c, 0.0);
        double total = 0;
        for (int i = 0; i < tx.length(); i++) {
            try {
                JSONObject o = tx.getJSONObject(i);
                if (o.getString("type").equals("Расход") && o.getInt("month") == month) {
                    String cat = o.getString("category");
                    double a = o.getDouble("amount");
                    sums.put(cat, sums.get(cat) + a);
                    total += a;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (total == 0) {
            ivPie.setImageBitmap(bmp); // расходов нет — пустой белый квадрат
            return;
        }

        RectF rect = new RectF(20, 20, size - 20, size - 20);
        Paint p = new Paint();
        p.setAntiAlias(true);
        float start = 0; // угол начала сектора
        int ci = 0;
        for (String cat : categories) {
            double val = sums.get(cat);
            if (val > 0) {
                float sweep = (float) (val / total * 360.0); // угол сектора
                p.setColor(colors[ci % colors.length]);
                canvas.drawArc(rect, start, sweep, true, p); // рисуем сектор
                start += sweep;
            }
            ci++;
        }
        ivPie.setImageBitmap(bmp);
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

    // Округление до 2 знаков
    double round(double x) {
        return Math.round(x * 100.0) / 100.0;
    }

    // Сохраняет транзакции в SharedPreferences
    void save() {
        prefs.edit().putString("data", tx.toString()).apply();
    }

    // Загружает транзакции при запуске
    void load() {
        String s = prefs.getString("data", "[]");
        try {
            tx = new JSONArray(s);
        } catch (Exception e) {
            tx = new JSONArray();
        }
    }
}
