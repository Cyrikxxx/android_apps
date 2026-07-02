package com.example.shop;

// Импорты
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class CartActivity extends Activity {

    ListView lvCart;
    TextView tvTotal;

    SharedPreferences prefs;
    JSONArray cart = new JSONArray();

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        lvCart = (ListView) findViewById(R.id.lvCart);
        tvTotal = (TextView) findViewById(R.id.tvTotal);

        prefs = getSharedPreferences("shop", MODE_PRIVATE);
        load();

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, lines);
        lvCart.setAdapter(adapter);
        refresh();

        // Долгое нажатие — удалить позицию из корзины
        lvCart.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                cart.remove(pos);
                save();
                refresh();
                Toast.makeText(CartActivity.this, "Удалено", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    // Обновляет список и общую стоимость
    void refresh() {
        lines.clear();
        int total = 0;
        for (int i = 0; i < cart.length(); i++) {
            try {
                JSONObject o = cart.getJSONObject(i);
                lines.add(o.getString("name") + " — " + o.getInt("price") + " руб.");
                total += o.getInt("price"); // суммируем цену
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        adapter.notifyDataSetChanged();
        tvTotal.setText("Итого: " + total + " руб.");
    }

    // Сохраняет корзину в SharedPreferences
    void save() {
        prefs.edit().putString("cart", cart.toString()).apply();
    }

    // Загружает корзину при запуске
    void load() {
        String s = prefs.getString("cart", "[]");
        try {
            cart = new JSONArray(s);
        } catch (Exception e) {
            cart = new JSONArray();
        }
    }
}
