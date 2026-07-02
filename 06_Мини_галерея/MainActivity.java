package com.example.gallery;

// Импорты
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public class MainActivity extends Activity {

    // Массив картинок галереи. Здесь взяты системные картинки Android,
    // чтобы проект компилировался сразу. Чтобы поставить свои — добавьте
    // файлы в res/drawable и замените ссылки на R.drawable.имя_файла.
    public static int[] images = {
            android.R.drawable.ic_menu_gallery,
            android.R.drawable.ic_menu_camera,
            android.R.drawable.ic_menu_compass,
            android.R.drawable.ic_menu_myplaces,
            android.R.drawable.star_big_on,
            android.R.drawable.ic_menu_report_image
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridView grid = (GridView) findViewById(R.id.grid);
        // Ставим свой адаптер, который рисует картинки в ячейках сетки
        grid.setAdapter(new ImageAdapter());

        // По нажатию на картинку открываем экран просмотра и передаём номер картинки
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Intent i = new Intent(MainActivity.this, DetailActivity.class);
                i.putExtra("index", pos); // передаём индекс выбранной картинки
                startActivity(i);
            }
        });
    }

    // Адаптер для сетки: на каждую ячейку создаёт ImageView с картинкой
    class ImageAdapter extends BaseAdapter {
        public int getCount() {
            return images.length;
        }

        public Object getItem(int position) {
            return images[position];
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView iv;
            if (convertView == null) {
                iv = new ImageView(MainActivity.this);
                // Задаём размер ячейки и как вписывать картинку
                iv.setLayoutParams(new GridView.LayoutParams(250, 250));
                iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                iv.setPadding(8, 8, 8, 8);
            } else {
                iv = (ImageView) convertView;
            }
            iv.setImageResource(images[position]);
            return iv;
        }
    }
}
