package com.example.restaurant;

// Импорты
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class MainActivity extends Activity {

    ListView lvRest;
    Button btnMyBookings;

    // Список ресторанов (статические данные)
    String[] restaurants = {
            "Пиццерия «Маргарита»",
            "Суши-бар «Сакура»",
            "Кофейня «Утро»",
            "Стейк-хаус «Гриль»",
            "Веранда «Сад»"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvRest = (ListView) findViewById(R.id.lvRest);
        btnMyBookings = (Button) findViewById(R.id.btnMyBookings);

        // Показываем список ресторанов
        lvRest.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, restaurants));

        // По нажатию на ресторан открываем экран бронирования
        lvRest.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Intent i = new Intent(MainActivity.this, BookActivity.class);
                i.putExtra("restaurant", restaurants[pos]); // передаём выбранный ресторан
                startActivity(i);
            }
        });

        // Кнопка "Мои бронирования"
        btnMyBookings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, BookingsActivity.class));
            }
        });
    }
}
