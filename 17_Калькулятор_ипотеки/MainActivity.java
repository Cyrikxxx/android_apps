package com.example.loan;

// Импорты
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {

    EditText etSum, etRate, etTerm;
    Button btnCalc;
    TextView tvResult;
    ListView lvSchedule;

    ArrayList<String> lines = new ArrayList<String>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etSum = (EditText) findViewById(R.id.etSum);
        etRate = (EditText) findViewById(R.id.etRate);
        etTerm = (EditText) findViewById(R.id.etTerm);
        btnCalc = (Button) findViewById(R.id.btnCalc);
        tvResult = (TextView) findViewById(R.id.tvResult);
        lvSchedule = (ListView) findViewById(R.id.lvSchedule);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines);
        lvSchedule.setAdapter(adapter);

        btnCalc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                calc();
            }
        });
    }

    // Считает аннуитетный платёж, переплату и график
    void calc() {
        double sum = parse(etSum.getText().toString());   // сумма кредита
        double rate = parse(etRate.getText().toString()); // годовая ставка, %
        int months = (int) parse(etTerm.getText().toString()); // срок в месяцах

        if (sum <= 0 || rate <= 0 || months <= 0) {
            Toast.makeText(this, "Заполните все поля числами", Toast.LENGTH_SHORT).show();
            return;
        }

        // Месячная процентная ставка (в долях)
        double i = rate / 100.0 / 12.0;

        // Формула аннуитетного платежа:
        // payment = S * i * (1+i)^n / ((1+i)^n - 1)
        double pow = Math.pow(1 + i, months);
        double payment = sum * i * pow / (pow - 1);

        double totalPaid = payment * months; // всего выплачено
        double overpay = totalPaid - sum;     // переплата (проценты банку)

        // Сравнение с досрочным погашением: платим на 10% больше каждый месяц
        double extraPayment = payment * 1.10;
        int monthsWithExtra = monthsToRepay(sum, i, extraPayment);
        double totalExtra = extraPayment * monthsWithExtra;
        double overpayExtra = totalExtra - sum;

        tvResult.setText(
                "Ежемесячный платёж: " + round(payment) + " руб.\n" +
                "Всего выплатите: " + round(totalPaid) + " руб.\n" +
                "Переплата: " + round(overpay) + " руб.\n\n" +
                "Если платить +10% (" + round(extraPayment) + " руб.):\n" +
                "срок = " + monthsWithExtra + " мес., переплата = " + round(overpayExtra) + " руб.");

        buildSchedule(sum, i, payment, months);
    }

    // Считает, за сколько месяцев погасится кредит при увеличенном платеже
    int monthsToRepay(double debt, double i, double payment) {
        int m = 0;
        // Гоняем цикл, пока долг не станет <= 0 (ограничим 1000 итераций на всякий случай)
        while (debt > 0 && m < 1000) {
            double interest = debt * i;       // проценты за месяц
            debt = debt + interest - payment; // новый остаток долга
            m++;
        }
        return m;
    }

    // Строит график платежей по месяцам (проценты, тело долга, остаток)
    void buildSchedule(double debt, double i, double payment, int months) {
        lines.clear();
        for (int m = 1; m <= months; m++) {
            double interest = debt * i;         // часть платежа на проценты
            double principal = payment - interest; // часть на погашение тела
            debt = debt - principal;             // остаток долга
            if (debt < 0) debt = 0;
            lines.add("Мес " + m + ": проценты " + round(interest)
                    + ", долг " + round(principal) + ", остаток " + round(debt));
        }
        adapter.notifyDataSetChanged();
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
