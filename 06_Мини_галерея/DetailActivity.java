package com.example.gallery;

// Импорты
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class DetailActivity extends Activity {

    ImageView big;      // большая картинка
    int index;          // какой сейчас показываем номер картинки

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        big = (ImageView) findViewById(R.id.big);
        Button btnPrev = (Button) findViewById(R.id.btnPrev);
        Button btnNext = (Button) findViewById(R.id.btnNext);

        // Забираем номер картинки, который передали с предыдущего экрана
        index = getIntent().getIntExtra("index", 0);
        show(); // показываем эту картинку

        // Кнопка "предыдущая"
        btnPrev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                index--;
                // если ушли левее первой — переходим на последнюю (по кругу)
                if (index < 0) {
                    index = MainActivity.images.length - 1;
                }
                show();
            }
        });

        // Кнопка "следующая"
        btnNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                index++;
                // если ушли правее последней — на первую (по кругу)
                if (index >= MainActivity.images.length) {
                    index = 0;
                }
                show();
            }
        });
    }

    // Показывает картинку с текущим индексом
    void show() {
        big.setImageResource(MainActivity.images[index]);
        setTitle("Фото " + (index + 1) + " из " + MainActivity.images.length);
    }
}
