package com.example.todolist;

// Импорты
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Paint;               // нужен для зачёркивания текста
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    EditText etTask;    // поле ввода новой задачи
    Button btnAdd;      // кнопка добавления
    ListView lvTasks;   // список задач

    SharedPreferences prefs;            // хранилище
    JSONArray list = new JSONArray();   // задачи: каждый элемент = {text, done}
    TaskAdapter adapter;                // свой адаптер (умеет зачёркивать выполненные)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etTask = (EditText) findViewById(R.id.etTask);
        btnAdd = (Button) findViewById(R.id.btnAdd);
        lvTasks = (ListView) findViewById(R.id.lvTasks);

        prefs = getSharedPreferences("todo", MODE_PRIVATE);
        loadData(); // читаем сохранённые задачи

        // Ставим наш адаптер на список
        adapter = new TaskAdapter();
        lvTasks.setAdapter(adapter);

        // Кнопка "Добавить"
        btnAdd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addTask();
            }
        });

        // Короткое нажатие по задаче — отметить выполненной / снять отметку
        lvTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                toggleDone(pos);
            }
        });

        // Долгое нажатие по задаче — удалить её
        lvTasks.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                deleteTask(pos);
                return true; // true = событие обработано
            }
        });
    }

    // Добавляет новую задачу
    void addTask() {
        String t = etTask.getText().toString().trim();
        if (t.length() == 0) {
            Toast.makeText(this, "Введите текст задачи", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("text", t);       // текст задачи
            o.put("done", false);   // новая задача ещё не выполнена
            list.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        etTask.setText("");     // очищаем поле ввода
        save();                 // сохраняем
        adapter.notifyDataSetChanged(); // перерисовываем список
    }

    // Переключает "выполнено / не выполнено" у задачи по позиции
    void toggleDone(int pos) {
        try {
            JSONObject o = list.getJSONObject(pos);
            o.put("done", !o.getBoolean("done")); // меняем значение на противоположное
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
        adapter.notifyDataSetChanged();
    }

    // Удаляет задачу по позиции
    void deleteTask(int pos) {
        list.remove(pos);   // remove доступен начиная с API 19
        save();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Задача удалена", Toast.LENGTH_SHORT).show();
    }

    // Сохраняет список задач в SharedPreferences
    void save() {
        prefs.edit().putString("data", list.toString()).apply();
    }

    // Загружает список задач при запуске
    void loadData() {
        String s = prefs.getString("data", "[]");
        try {
            list = new JSONArray(s);
        } catch (Exception e) {
            list = new JSONArray();
        }
    }

    // Свой адаптер: рисует текст задачи и зачёркивает его, если задача выполнена
    class TaskAdapter extends BaseAdapter {
        public int getCount() {
            return list.length();
        }

        public Object getItem(int position) {
            return list.opt(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // Используем простой стандартный макет с одним TextView
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                        android.R.layout.simple_list_item_1, parent, false);
            }
            TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
            try {
                JSONObject o = list.getJSONObject(position);
                tv.setText(o.getString("text"));
                if (o.getBoolean("done")) {
                    // Включаем флаг зачёркивания текста
                    tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    // Убираем зачёркивание (важно из-за переиспользования View)
                    tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return convertView;
        }
    }
}
