package com.example.plants;

// Импорты
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

    Spinner spCatalog;      // выбор растения из справочника
    TextView tvCare;        // информация об уходе
    EditText etName, etInterval;
    Button btnPhoto, btnAdd;
    ImageView ivPhoto;
    ListView lvMyPlants;

    static final int REQ_PHOTO = 77;

    SharedPreferences prefs;
    // Мои растения: {name, interval(дней), lastWatered:"гггг-мм-дд"}
    JSONArray plants = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    // Справочник: 20 комнатных растений и уход за ними
    String[] catalogNames = {
            "Фикус", "Кактус", "Орхидея", "Алоэ", "Спатифиллум",
            "Драцена", "Замиокулькас", "Фиалка", "Герань", "Хлорофитум",
            "Монстера", "Сансевиерия", "Бегония", "Каланхоэ", "Плющ",
            "Толстянка", "Азалия", "Пальма", "Папоротник", "Гибискус"
    };
    String[] catalogCare = {
            "Полив 1 раз в неделю, яркий рассеянный свет, 18-24°C, подкормка раз в месяц.",
            "Полив редкий (раз в 2 недели), яркий свет, 15-30°C, подкормка редко.",
            "Полив погружением раз в неделю, рассеянный свет, 18-25°C, спец. удобрение.",
            "Полив раз в 10 дней, яркий свет, 18-26°C, подкормка редко.",
            "Полив 2 раза в неделю, полутень, 18-23°C, подкормка раз в 2 недели.",
            "Полив умеренный, рассеянный свет, 18-25°C, подкормка раз в месяц.",
            "Полив редкий, свет любой, 18-26°C, подкормка редко.",
            "Полив в поддон, рассеянный свет, 18-24°C, подкормка раз в 2 недели.",
            "Полив умеренный, яркий свет, 15-22°C, подкормка раз в месяц.",
            "Полив обильный, рассеянный свет, 15-25°C, подкормка раз в месяц.",
            "Полив умеренный, рассеянный свет, 20-27°C, опрыскивание, подкормка раз в месяц.",
            "Полив очень редкий, любой свет, 18-27°C, подкормка редко.",
            "Полив умеренный, рассеянный свет, 18-23°C, подкормка раз в 2 недели.",
            "Полив редкий, яркий свет, 18-27°C, подкормка раз в месяц.",
            "Полив умеренный, полутень, 15-23°C, подкормка раз в месяц.",
            "Полив редкий, яркий свет, 18-25°C, подкормка редко.",
            "Полив обильный, рассеянный свет, 15-20°C, подкормка раз в 2 недели.",
            "Полив умеренный, яркий свет, 18-25°C, подкормка раз в месяц.",
            "Полив обильный, тень/полутень, 18-22°C, опрыскивание, подкормка раз в месяц.",
            "Полив обильный, яркий свет, 18-25°C, подкормка раз в 2 недели."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spCatalog = (Spinner) findViewById(R.id.spCatalog);
        tvCare = (TextView) findViewById(R.id.tvCare);
        etName = (EditText) findViewById(R.id.etName);
        etInterval = (EditText) findViewById(R.id.etInterval);
        btnPhoto = (Button) findViewById(R.id.btnPhoto);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        ivPhoto = (ImageView) findViewById(R.id.ivPhoto);
        lvMyPlants = (ListView) findViewById(R.id.lvMyPlants);

        prefs = getSharedPreferences("plants", MODE_PRIVATE);
        load();

        spCatalog.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, catalogNames));

        // При выборе растения в справочнике показываем уход за ним
        spCatalog.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tvCare.setText(catalogCare[position]);
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines);
        lvMyPlants.setAdapter(adapter);
        refresh();

        // Выбор фото из галереи
        btnPhoto.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // ACTION_GET_CONTENT открывает системный выбор картинки.
                // Отдельное разрешение на память при этом не требуется.
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                startActivityForResult(i, REQ_PHOTO);
            }
        });

        // Добавить своё растение
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addPlant();
            }
        });

        // Долгое нажатие — удалить растение
        lvMyPlants.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                plants.remove(pos);
                save();
                refresh();
                return true;
            }
        });

        // Короткое нажатие — "полить" (обновить дату последнего полива)
        lvMyPlants.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                waterPlant(pos);
            }
        });
    }

    // Показывает выбранное фото
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PHOTO && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            ivPhoto.setImageURI(uri); // показываем картинку по её URI
        }
    }

    // Возвращает сегодняшнюю дату в виде "гггг-мм-дд"
    String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
    }

    // Добавляет своё растение
    void addPlant() {
        String name = etName.getText().toString().trim();
        int interval = (int) parse(etInterval.getText().toString());
        if (name.length() == 0 || interval <= 0) {
            Toast.makeText(this, "Введите название и интервал полива", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("name", name);
            o.put("interval", interval);
            o.put("lastWatered", today());
            plants.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        etName.setText("");
        etInterval.setText("");
        save();
        refresh();
    }

    // Отмечает полив растения сегодня
    void waterPlant(int pos) {
        try {
            plants.getJSONObject(pos).put("lastWatered", today());
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
        refresh();
        Toast.makeText(this, "Полито сегодня", Toast.LENGTH_SHORT).show();
    }

    // Обновляет список моих растений и показывает, какие пора полить
    void refresh() {
        lines.clear();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        long now = System.currentTimeMillis();
        for (int i = 0; i < plants.length(); i++) {
            try {
                JSONObject o = plants.getJSONObject(i);
                int interval = o.getInt("interval");
                long last = sdf.parse(o.getString("lastWatered")).getTime();
                // Сколько дней прошло с последнего полива
                long days = (now - last) / (24L * 60 * 60 * 1000);
                String status = (days >= interval) ? "  ⚠ ПОРА ПОЛИТЬ!" : "  (ок)";
                lines.add(o.getString("name") + " — полив раз в " + interval
                        + " дн.\nПоследний полив: " + o.getString("lastWatered") + status);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
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

    // Сохраняет мои растения в SharedPreferences
    void save() {
        prefs.edit().putString("data", plants.toString()).apply();
    }

    // Загружает мои растения при запуске
    void load() {
        String s = prefs.getString("data", "[]");
        try {
            plants = new JSONArray(s);
        } catch (Exception e) {
            plants = new JSONArray();
        }
    }
}
