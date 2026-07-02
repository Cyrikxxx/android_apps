package com.example.vacation;

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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {

    Spinner spRole;             // роль: Сотрудник / Администратор
    LinearLayout formEmployee;  // форма заявки (видна только сотруднику)
    EditText etName;
    Button btnStart, btnEnd, btnRequest;
    ListView lvRequests;

    SharedPreferences prefs;
    // Заявка: {name, start:"гггг-мм-дд", end, status:"Ожидает"/"Подтверждён"/"Отклонён"}
    JSONArray requests = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    String startDate = "", endDate = "";
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spRole = (Spinner) findViewById(R.id.spRole);
        formEmployee = (LinearLayout) findViewById(R.id.formEmployee);
        etName = (EditText) findViewById(R.id.etName);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnEnd = (Button) findViewById(R.id.btnEnd);
        btnRequest = (Button) findViewById(R.id.btnRequest);
        lvRequests = (ListView) findViewById(R.id.lvRequests);

        prefs = getSharedPreferences("vacation", MODE_PRIVATE);
        load();

        spRole.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Сотрудник", "Администратор"}));

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines);
        lvRequests.setAdapter(adapter);
        refresh();

        // Смена роли: сотруднику показываем форму, админу прячем
        spRole.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                boolean isEmployee = (position == 0);
                formEmployee.setVisibility(isEmployee ? View.VISIBLE : View.GONE);
                refresh();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Выбор даты начала
        btnStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickDate(true);
            }
        });
        // Выбор даты конца
        btnEnd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pickDate(false);
            }
        });

        // Подать заявку
        btnRequest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                request();
            }
        });

        // Нажатие по заявке: админ переключает статус (подтвердить/отклонить)
        lvRequests.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                if (spRole.getSelectedItemPosition() == 1) { // только администратор
                    cycleStatus(pos);
                }
            }
        });
    }

    // Открывает календарь для выбора начала или конца отпуска
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

    // Подаёт заявку с проверкой пересечений
    void request() {
        String name = etName.getText().toString().trim();
        if (name.length() == 0 || startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Заполните имя и даты", Toast.LENGTH_SHORT).show();
            return;
        }
        // Проверяем пересечение: сколько НЕ отклонённых заявок пересекается с периодом
        int overlaps = countOverlaps(startDate, endDate);
        if (overlaps >= 2) {
            Toast.makeText(this, "На этот период уже 2 отпуска — пересечение!",
                    Toast.LENGTH_LONG).show();
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("start", startDate);
            o.put("end", endDate);
            o.put("status", "Ожидает");
            requests.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
        refresh();
        Toast.makeText(this, "Заявка подана", Toast.LENGTH_SHORT).show();
    }

    // Считает, сколько не отклонённых заявок пересекаются с [s, e]
    int countOverlaps(String s, String e) {
        int count = 0;
        try {
            long ns = sdf.parse(s).getTime();
            long ne = sdf.parse(e).getTime();
            for (int i = 0; i < requests.length(); i++) {
                JSONObject o = requests.getJSONObject(i);
                if (o.getString("status").equals("Отклонён")) continue;
                long os = sdf.parse(o.getString("start")).getTime();
                long oe = sdf.parse(o.getString("end")).getTime();
                // Периоды пересекаются, если начало одного <= конца другого и наоборот
                if (ns <= oe && os <= ne) {
                    count++;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return count;
    }

    // Переключает статус заявки по кругу: Ожидает -> Подтверждён -> Отклонён -> ...
    void cycleStatus(int pos) {
        try {
            JSONObject o = requests.getJSONObject(pos);
            String st = o.getString("status");
            if (st.equals("Ожидает")) st = "Подтверждён";
            else if (st.equals("Подтверждён")) st = "Отклонён";
            else st = "Ожидает";
            o.put("status", st);
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
        refresh();
    }

    // Обновляет список заявок
    void refresh() {
        lines.clear();
        for (int i = 0; i < requests.length(); i++) {
            try {
                JSONObject o = requests.getJSONObject(i);
                lines.add(o.getString("name") + ": " + o.getString("start")
                        + " — " + o.getString("end") + "\nСтатус: " + o.getString("status"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Сохраняет заявки в SharedPreferences
    void save() {
        prefs.edit().putString("data", requests.toString()).apply();
    }

    // Загружает заявки при запуске
    void load() {
        String s = prefs.getString("data", "[]");
        try {
            requests = new JSONArray(s);
        } catch (Exception e) {
            requests = new JSONArray();
        }
    }
}
