package com.example.expensetracker;

// Импорты нужных классов
import android.app.Activity;
import android.app.DatePickerDialog;        // системный диалог выбора даты (календарь)
import android.content.SharedPreferences;   // хранилище "ключ-значение"
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

import org.json.JSONArray;                  // JSON встроен в Android, это не сторонняя библиотека
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    // Элементы интерфейса
    EditText etAmount;      // поле ввода суммы
    Spinner spCategory;     // список категорий
    Button btnDate;         // кнопка выбора даты (открывает календарь)
    Spinner spMonth;        // список месяцев (фильтр баланса)
    Button btnAdd;
    ListView lvExpenses;    // список всех трат
    TextView tvBalance;     // итоговый баланс за месяц

    SharedPreferences prefs;
    JSONArray list = new JSONArray();   // все траты как JSON-объекты

    // Выбранная пользователем дата расхода (по умолчанию — сегодня)
    int selYear, selMonth, selDay;

    ArrayList<String> lines = new ArrayList<String>(); // строки для ListView
    ArrayAdapter<String> adapter;

    String[] months = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Находим элементы интерфейса по id
        etAmount = (EditText) findViewById(R.id.etAmount);
        spCategory = (Spinner) findViewById(R.id.spCategory);
        btnDate = (Button) findViewById(R.id.btnDate);
        spMonth = (Spinner) findViewById(R.id.spMonth);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        lvExpenses = (ListView) findViewById(R.id.lvExpenses);
        tvBalance = (TextView) findViewById(R.id.tvBalance);

        prefs = getSharedPreferences("expenses", MODE_PRIVATE);

        // Заполняем Spinner категориями
        String[] cats = {"Еда", "Транспорт", "Развлечения", "Жилье", "Прочее"};
        spCategory.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, cats));

        // Заполняем Spinner месяцами и ставим по умолчанию текущий месяц
        spMonth.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, months));
        Calendar now = Calendar.getInstance();
        spMonth.setSelection(now.get(Calendar.MONTH));

        // Изначально выбранная дата расхода = сегодня
        selYear = now.get(Calendar.YEAR);
        selMonth = now.get(Calendar.MONTH);
        selDay = now.get(Calendar.DAY_OF_MONTH);
        btnDate.setText(formatDate(selDay, selMonth, selYear));

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines);
        lvExpenses.setAdapter(adapter);

        // Загружаем сохранённые траты и показываем их
        loadData();
        refreshList();
        updateBalance();

        // Кнопка даты открывает календарь
        btnDate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // Кнопка "Добавить" сохраняет новую трату
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addExpense();
            }
        });

        // При смене месяца пересчитываем баланс
        spMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateBalance();
            }
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // Показывает календарь и запоминает выбранную дату
    void showDatePicker() {
        // В конструктор передаём текущую дату, чтобы календарь открылся на ней
        DatePickerDialog dlg = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    // Вызовется, когда пользователь нажмёт "OK" в календаре
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        selYear = year;
                        selMonth = month;
                        selDay = day;
                        btnDate.setText(formatDate(day, month, year));
                    }
                },
                selYear, selMonth, selDay);
        dlg.show();
    }

    // Собирает дату в строку "дд.мм.гггг"
    String formatDate(int day, int month, int year) {
        Calendar c = Calendar.getInstance();
        c.set(year, month, day);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return sdf.format(c.getTime());
    }

    // Добавляет новую трату в список и сохраняет
    void addExpense() {
        String s = etAmount.getText().toString().trim();
        if (s.length() == 0) {
            Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show();
            return;
        }
        double a;
        try {
            a = Double.parseDouble(s); // текст -> число
        } catch (Exception e) {
            Toast.makeText(this, "Неверная сумма", Toast.LENGTH_SHORT).show();
            return;
        }

        String cat = spCategory.getSelectedItem().toString();
        String date = formatDate(selDay, selMonth, selYear);

        // Складываем все поля траты в JSON-объект и кладём в общий массив
        try {
            JSONObject o = new JSONObject();
            o.put("amount", a);
            o.put("category", cat);
            o.put("date", date);
            o.put("month", selMonth); // номер месяца (0-11) для фильтра баланса
            list.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }

        saveData();
        refreshList();
        updateBalance();
        etAmount.setText("");
    }

    // Перестраивает текстовый список трат для ListView
    void refreshList() {
        lines.clear();
        for (int i = 0; i < list.length(); i++) {
            try {
                JSONObject o = list.getJSONObject(i);
                lines.add(o.getString("date") + "  " + o.getString("category")
                        + "  -  " + o.getDouble("amount") + " руб.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged(); // список перерисуется
    }

    // Считает сумму трат за выбранный в фильтре месяц
    void updateBalance() {
        int m = spMonth.getSelectedItemPosition();
        double sum = 0;
        for (int i = 0; i < list.length(); i++) {
            try {
                JSONObject o = list.getJSONObject(i);
                // Складываем только траты выбранного месяца
                if (o.getInt("month") == m) {
                    sum += o.getDouble("amount");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tvBalance.setText("Итого за " + months[m] + ": " + sum + " руб.");
    }

    // Сохраняет весь массив трат как одну JSON-строку
    void saveData() {
        prefs.edit().putString("data", list.toString()).apply();
    }

    // Загружает массив трат при запуске
    void loadData() {
        String data = prefs.getString("data", "[]"); // "[]" — если ничего не сохранено
        try {
            list = new JSONArray(data);
        } catch (Exception e) {
            list = new JSONArray();
        }
    }
}
