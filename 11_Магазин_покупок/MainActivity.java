package com.example.shop;

// Импорты
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {

    ListView lvProducts;
    Button btnCart;

    // Список товаров (захардкожен). Параллельные массивы: название и цена.
    String[] names = {"Хлеб", "Молоко", "Яблоки", "Кофе", "Шоколад", "Сыр"};
    int[] prices = {40, 70, 120, 300, 90, 250};

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lvProducts = (ListView) findViewById(R.id.lvProducts);
        btnCart = (Button) findViewById(R.id.btnCart);

        prefs = getSharedPreferences("shop", MODE_PRIVATE);

        // Ставим свой адаптер, который рисует строку товара с кнопкой "Купить"
        lvProducts.setAdapter(new ProductAdapter());

        // Кнопка "Корзина" открывает второй экран
        btnCart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CartActivity.class));
            }
        });
    }

    // Добавляет товар в корзину (в SharedPreferences)
    void addToCart(int pos) {
        JSONArray cart = loadCart();
        try {
            JSONObject o = new JSONObject();
            o.put("name", names[pos]);
            o.put("price", prices[pos]);
            cart.put(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        prefs.edit().putString("cart", cart.toString()).apply();
        Toast.makeText(this, names[pos] + " добавлен в корзину", Toast.LENGTH_SHORT).show();
    }

    // Читает корзину из хранилища
    JSONArray loadCart() {
        String s = prefs.getString("cart", "[]");
        try {
            return new JSONArray(s);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    // Адаптер: одна строка = название, цена и кнопка "Купить"
    class ProductAdapter extends BaseAdapter {
        public int getCount() {
            return names.length;
        }

        public Object getItem(int position) {
            return names[position];
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // Раздуваем свою строку item_product.xml
                convertView = getLayoutInflater().inflate(R.layout.item_product, parent, false);
            }
            TextView tvName = (TextView) convertView.findViewById(R.id.tvName);
            Button btnBuy = (Button) convertView.findViewById(R.id.btnBuy);

            tvName.setText(names[position] + " — " + prices[position] + " руб.");

            // По кнопке добавляем именно этот товар в корзину
            btnBuy.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    addToCart(position);
                }
            });
            return convertView;
        }
    }
}
