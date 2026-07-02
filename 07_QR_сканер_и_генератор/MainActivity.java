package com.example.qr;

// Импорты
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    EditText etText;    // текст для генерации кода
    ImageView ivCode;   // сюда рисуем сгенерированный код
    Button btnGen;      // кнопка "Сгенерировать"
    Button btnScan;     // кнопка "Сканировать камерой"
    TextView tvScan;    // подпись про результат сканирования

    // Код, по которому запускали камеру (нужен, чтобы поймать результат)
    static final int REQ_CAMERA = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etText = (EditText) findViewById(R.id.etText);
        ivCode = (ImageView) findViewById(R.id.ivCode);
        btnGen = (Button) findViewById(R.id.btnGen);
        btnScan = (Button) findViewById(R.id.btnScan);
        tvScan = (TextView) findViewById(R.id.tvScan);

        // Кнопка генерации: строим картинку-код из введённого текста
        btnGen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String text = etText.getText().toString();
                if (text.length() == 0) {
                    Toast.makeText(MainActivity.this, "Введите текст", Toast.LENGTH_SHORT).show();
                    return;
                }
                Bitmap bmp = makeCode(text); // рисуем код
                ivCode.setImageBitmap(bmp);  // показываем на экране
            }
        });

        // Кнопка сканирования: открываем системное приложение камеры
        btnScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // ACTION_IMAGE_CAPTURE запускает камеру и возвращает снимок.
                // Отдельное разрешение CAMERA при таком способе НЕ требуется,
                // так как снимок делает системное приложение камеры.
                Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (i.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(i, REQ_CAMERA);
                } else {
                    Toast.makeText(MainActivity.this, "Камера недоступна", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Сюда возвращается результат работы камеры
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAMERA && resultCode == RESULT_OK && data != null) {
            // Получаем миниатюру снимка
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            if (photo != null) {
                ivCode.setImageBitmap(photo); // показываем сделанный снимок
                // Полноценное распознавание содержимого QR требует библиотеки (ZXing),
                // которая в этом проекте не используется. Поэтому показываем фото
                // и сообщаем об этом ограничении.
                tvScan.setText("Снимок сделан. Распознавание содержимого QR требует\n" +
                        "библиотеки декодирования (см. README).");
            }
        }
    }

    // Строит QR-ПОДОБНУЮ картинку из текста.
    // Это НЕ настоящий стандартный QR-код: клетки заполняются по простому
    // правилу на основе символов текста, плюс дорисованы угловые квадраты
    // для узнаваемого вида. Настоящий QR требует библиотеки (см. README).
    Bitmap makeCode(String text) {
        int cells = 25;         // сетка 25x25 клеток
        int cellSize = 20;      // размер одной клетки в пикселях
        int size = cells * cellSize;

        // Создаём пустое белое изображение
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);

        Paint black = new Paint();
        black.setColor(Color.BLACK);

        // Заполняем клетки: цвет зависит от кода символа и позиции
        for (int y = 0; y < cells; y++) {
            for (int x = 0; x < cells; x++) {
                // простое детерминированное правило "чёрная/белая клетка"
                int ch = text.charAt((x + y) % text.length());
                boolean fill = ((ch + x * 7 + y * 13) % 2) == 0;
                if (fill) {
                    canvas.drawRect(x * cellSize, y * cellSize,
                            (x + 1) * cellSize, (y + 1) * cellSize, black);
                }
            }
        }

        // Дорисовываем 3 угловых "глаза" как у настоящего QR (для вида)
        drawEye(canvas, black, 0, 0, cellSize);
        drawEye(canvas, black, (cells - 7) * cellSize, 0, cellSize);
        drawEye(canvas, black, 0, (cells - 7) * cellSize, cellSize);

        return bmp;
    }

    // Рисует один угловой квадрат-"глаз" 7x7 клеток
    void drawEye(Canvas c, Paint black, int left, int top, int cell) {
        Paint white = new Paint();
        white.setColor(Color.WHITE);
        // внешний чёрный квадрат
        c.drawRect(left, top, left + 7 * cell, top + 7 * cell, black);
        // белая рамка внутри
        c.drawRect(left + cell, top + cell, left + 6 * cell, top + 6 * cell, white);
        // чёрный центр
        c.drawRect(left + 2 * cell, top + 2 * cell, left + 5 * cell, top + 5 * cell, black);
    }
}
