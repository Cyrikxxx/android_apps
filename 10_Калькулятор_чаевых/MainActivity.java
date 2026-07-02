package com.example.tips;

// Импорты
import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {

    EditText etBill;    // сумма счёта
    EditText etPeople;  // количество человек
    SeekBar sbTip;      // ползунок процента чаевых
    TextView tvPercent; // текст с выбранным процентом
    TextView tvResult;  // результат расчёта

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etBill = (EditText) findViewById(R.id.etBill);
        etPeople = (EditText) findViewById(R.id.etPeople);
        sbTip = (SeekBar) findViewById(R.id.sbTip);
        tvPercent = (TextView) findViewById(R.id.tvPercent);
        tvResult = (TextView) findViewById(R.id.tvResult);

        // SeekBar даёт значения 0..20; мы прибавляем 5, чтобы получить 5..25%
        sbTip.setMax(20);
        sbTip.setProgress(5); // старт: 10% (5+5)

        // Один и тот же обработчик пересчёта будем вызывать при любом изменении.
        // TextWatcher следит за изменением текста в полях.
        TextWatcher watcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                calc();
            }
            public void afterTextChanged(Editable s) {}
        };
        etBill.addTextChangedListener(watcher);
        etPeople.addTextChangedListener(watcher);

        // Слушатель ползунка: пересчитываем при перетаскивании
        sbTip.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                calc();
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        calc(); // первый расчёт при запуске
    }

    // Считает чаевые, общую сумму и долю на человека
    void calc() {
        int percent = sbTip.getProgress() + 5; // реальный процент 5..25
        tvPercent.setText("Чаевые: " + percent + "%");

        // Читаем сумму счёта (если пусто/ошибка — считаем 0)
        double bill = parse(etBill.getText().toString());
        // Читаем количество человек (минимум 1, чтобы не делить на ноль)
        int people = (int) parse(etPeople.getText().toString());
        if (people < 1) people = 1;

        double tip = bill * percent / 100.0;    // сумма чаевых
        double total = bill + tip;              // итого к оплате
        double perPerson = total / people;      // доля на человека

        // Округляем до 2 знаков и показываем
        tvResult.setText(
                "Чаевые: " + round(tip) + " руб.\n" +
                "Итого к оплате: " + round(total) + " руб.\n" +
                "На одного человека: " + round(perPerson) + " руб.");
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

    // Округление до 2 знаков
    double round(double x) {
        return Math.round(x * 100.0) / 100.0;
    }
}
