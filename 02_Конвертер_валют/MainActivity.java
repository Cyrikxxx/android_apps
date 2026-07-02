package com.example.currencyconverter;

// Импорты нужных классов
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;                  // для разбора ответа API (встроен в Android)

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;           // стандартный класс для HTTP-запросов
import java.net.URL;

public class MainActivity extends Activity {

    // Элементы интерфейса
    EditText etAmount;      // поле для ввода суммы в рублях
    RadioGroup rgCurrency;  // переключатель валюты (USD / EUR)
    Button btnConvert;      // кнопка "Конвертировать"
    TextView tvResult;      // текст с результатом

    // Курсы валют (сколько рублей стоит 1 доллар / 1 евро).
    // 0 означает "ещё не загрузили".
    double usd = 0;
    double eur = 0;

    // Адрес открытого API Центробанка РФ с курсами валют
    String URL_CBR = "https://www.cbr-xml-daily.ru/daily_json.js";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Показываем разметку экрана
        setContentView(R.layout.activity_main);

        // Находим элементы интерфейса по id
        etAmount = (EditText) findViewById(R.id.etAmount);
        rgCurrency = (RadioGroup) findViewById(R.id.rgCurrency);
        btnConvert = (Button) findViewById(R.id.btnConvert);
        tvResult = (TextView) findViewById(R.id.tvResult);

        // Сразу при запуске загружаем курсы из интернета
        loadRates();

        // Вешаем обработчик на кнопку — по клику считаем результат
        btnConvert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                convert();
            }
        });
    }

    // Считает результат конвертации по уже загруженным курсам
    void convert() {
        // Если курсы ещё не пришли — сообщаем и выходим
        if (usd == 0 || eur == 0) {
            Toast.makeText(this, "Курсы ещё не загружены", Toast.LENGTH_SHORT).show();
            return;
        }
        // Читаем сумму из поля
        String s = etAmount.getText().toString().trim();
        if (s.length() == 0) {
            Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show();
            return;
        }
        double rub;
        try {
            rub = Double.parseDouble(s); // текст -> число
        } catch (Exception e) {
            Toast.makeText(this, "Неверная сумма", Toast.LENGTH_SHORT).show();
            return;
        }

        double res;
        String cur;
        // Смотрим, какая кнопка выбрана в RadioGroup, и делим рубли на курс.
        // (В API Value = сколько рублей за 1 единицу валюты,
        //  поэтому рубли / курс = сумма в валюте.)
        if (rgCurrency.getCheckedRadioButtonId() == R.id.rbUsd) {
            res = rub / usd;
            cur = "USD";
        } else {
            res = rub / eur;
            cur = "EUR";
        }

        // Округляем до 2 знаков после запятой
        res = Math.round(res * 100.0) / 100.0;
        tvResult.setText(rub + " руб. = " + res + " " + cur);
    }

    // Загружает курсы валют из интернета.
    // ВАЖНО: сеть нельзя трогать в главном потоке, поэтому работаем в new Thread.
    void loadRates() {
        tvResult.setText("Загрузка курсов...");
        // Создаём отдельный поток для сетевого запроса
        new Thread(new Runnable() {
            public void run() {
                try {
                    // Открываем соединение по адресу API
                    URL url = new URL(URL_CBR);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");     // обычный GET-запрос
                    con.setConnectTimeout(10000);    // ждём подключения максимум 10 сек
                    con.setReadTimeout(10000);       // ждём ответа максимум 10 сек

                    // Читаем ответ построчно и собираем в одну строку
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    br.close();
                    con.disconnect();

                    // Разбираем JSON: корень -> Valute -> USD/EUR -> Value
                    JSONObject root = new JSONObject(sb.toString());
                    JSONObject valute = root.getJSONObject("Valute");
                    final double u = valute.getJSONObject("USD").getDouble("Value");
                    final double e = valute.getJSONObject("EUR").getDouble("Value");

                    // Обновлять интерфейс можно только в главном потоке,
                    // поэтому используем runOnUiThread
                    runOnUiThread(new Runnable() {
                        public void run() {
                            usd = u;
                            eur = e;
                            tvResult.setText("Курсы загружены. USD=" + u + ", EUR=" + e);
                        }
                    });
                } catch (final Exception ex) {
                    // Если что-то пошло не так (нет интернета и т.п.) — сообщаем
                    ex.printStackTrace();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvResult.setText("Ошибка загрузки курсов");
                            Toast.makeText(MainActivity.this,
                                    "Нет интернета?", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start(); // запускаем поток
    }
}
