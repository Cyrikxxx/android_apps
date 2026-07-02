package com.example.dictionary;

// Импорты
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends Activity {

    // Поля формы добавления
    EditText etWord, etTranslation, etExample;
    Button btnAdd;
    ListView lvWords;

    // Элементы режима "карточки"
    TextView tvCard;    // текст карточки
    Button btnFlip;     // перевернуть карточку
    Button btnNext;     // следующая карточка

    SharedPreferences prefs;
    // Слова: {word, translation, example}
    JSONArray words = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    int cardIndex = 0;      // какая карточка показывается
    boolean showTranslation = false; // false = лицевая сторона (слово)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etWord = (EditText) findViewById(R.id.etWord);
        etTranslation = (EditText) findViewById(R.id.etTranslation);
        etExample = (EditText) findViewById(R.id.etExample);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        lvWords = (ListView) findViewById(R.id.lvWords);
        tvCard = (TextView) findViewById(R.id.tvCard);
        btnFlip = (Button) findViewById(R.id.btnFlip);
        btnNext = (Button) findViewById(R.id.btnNext);

        prefs = getSharedPreferences("dict", MODE_PRIVATE);
        load();

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, lines);
        lvWords.setAdapter(adapter);
        refreshList();
        showCard();

        // Добавление слова
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addWord();
            }
        });

        // Долгое нажатие по слову — удалить
        lvWords.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                words.remove(pos);
                save();
                refreshList();
                showCard();
                return true;
            }
        });

        // Перевернуть карточку (слово <-> перевод с примером)
        btnFlip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showTranslation = !showTranslation;
                showCard();
            }
        });

        // Следующая карточка
        btnNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (words.length() == 0) return;
                cardIndex = (cardIndex + 1) % words.length(); // по кругу
                showTranslation = false; // новая карточка — снова лицевой стороной
                showCard();
            }
        });
    }

    // Добавляет новое слово
    void addWord() {
        String w = etWord.getText().toString().trim();
        String t = etTranslation.getText().toString().trim();
        String ex = etExample.getText().toString().trim();
        if (w.length() == 0 || t.length() == 0) {
            Toast.makeText(this, "Введите слово и перевод", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("word", w);
            o.put("translation", t);
            o.put("example", ex);
            words.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Очищаем поля
        etWord.setText("");
        etTranslation.setText("");
        etExample.setText("");
        save();
        refreshList();
        showCard();
    }

    // Обновляет список слов
    void refreshList() {
        lines.clear();
        for (int i = 0; i < words.length(); i++) {
            try {
                JSONObject o = words.getJSONObject(i);
                lines.add(o.getString("word") + " — " + o.getString("translation"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Показывает текущую карточку (лицо или оборот)
    void showCard() {
        if (words.length() == 0) {
            tvCard.setText("Нет слов.\nДобавьте первое слово выше.");
            return;
        }
        // Если удалили последнее — не выходим за границы
        if (cardIndex >= words.length()) cardIndex = 0;
        try {
            JSONObject o = words.getJSONObject(cardIndex);
            if (showTranslation) {
                // Оборотная сторона: перевод и пример
                String ex = o.getString("example");
                tvCard.setText(o.getString("translation")
                        + (ex.length() > 0 ? "\n\nПример: " + ex : ""));
            } else {
                // Лицевая сторона: само слово
                tvCard.setText(o.getString("word"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Сохраняет слова в SharedPreferences
    void save() {
        prefs.edit().putString("data", words.toString()).apply();
    }

    // Загружает слова при запуске
    void load() {
        String s = prefs.getString("data", "[]");
        try {
            words = new JSONArray(s);
        } catch (Exception e) {
            words = new JSONArray();
        }
    }
}
