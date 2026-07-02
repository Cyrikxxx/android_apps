package com.example.quiztimer;

// Импорты нужных классов
import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;    // стандартный таймер обратного отсчёта
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainActivity extends Activity {

    // Элементы интерфейса
    TextView tvQuestion;    // текст вопроса
    TextView tvTimer;       // сколько секунд осталось
    TextView tvNumber;      // "Вопрос N из 5"
    RadioGroup rgAnswers;   // группа вариантов ответа
    RadioButton rb0, rb1, rb2, rb3; // четыре варианта
    Button btnNext;         // кнопка "Ответить / Далее"

    // Тексты вопросов
    String[] questions = {
            "Столица Франции?",
            "Сколько будет 2 + 2 * 2?",
            "Какой язык используется для Android (в этом курсе)?",
            "Сколько байт в одном килобайте?",
            "Какая планета ближе всего к Солнцу?"
    };

    // Варианты ответов: для каждого вопроса по 4 строки
    String[][] answers = {
            {"Лондон", "Париж", "Берлин", "Мадрид"},
            {"8", "6", "4", "16"},
            {"Python", "C#", "Java", "Swift"},
            {"1000", "512", "1024", "256"},
            {"Венера", "Земля", "Марс", "Меркурий"}
    };

    // Индекс правильного ответа для каждого вопроса (0-3)
    int[] correct = {1, 1, 2, 2, 3};

    int current = 0;            // номер текущего вопроса (с 0)
    int score = 0;             // количество правильных ответов
    CountDownTimer timer;      // ссылка на активный таймер (чтобы можно было отменить)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Находим элементы интерфейса
        tvQuestion = (TextView) findViewById(R.id.tvQuestion);
        tvTimer = (TextView) findViewById(R.id.tvTimer);
        tvNumber = (TextView) findViewById(R.id.tvNumber);
        rgAnswers = (RadioGroup) findViewById(R.id.rgAnswers);
        rb0 = (RadioButton) findViewById(R.id.rb0);
        rb1 = (RadioButton) findViewById(R.id.rb1);
        rb2 = (RadioButton) findViewById(R.id.rb2);
        rb3 = (RadioButton) findViewById(R.id.rb3);
        btnNext = (Button) findViewById(R.id.btnNext);

        // Показываем первый вопрос
        showQuestion();

        // По кнопке "Далее": останавливаем таймер, засчитываем ответ и идём дальше
        btnNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (timer != null) {
                    timer.cancel(); // чтобы таймер не сработал на следующем вопросе
                }
                checkAnswer();
                current++;
                if (current < questions.length) {
                    showQuestion();
                } else {
                    showResult();
                }
            }
        });
    }

    // Выводит на экран текущий вопрос и его варианты, запускает таймер
    void showQuestion() {
        tvNumber.setText("Вопрос " + (current + 1) + " из " + questions.length);
        tvQuestion.setText(questions[current]);
        // Подставляем варианты ответа в кнопки
        rb0.setText(answers[current][0]);
        rb1.setText(answers[current][1]);
        rb2.setText(answers[current][2]);
        rb3.setText(answers[current][3]);
        rgAnswers.clearCheck(); // сбрасываем прошлый выбор

        startTimer();
    }

    // Запускает 10-секундный таймер на текущий вопрос
    void startTimer() {
        // Первый аргумент — сколько миллисекунд всего (10000 = 10 сек),
        // второй — как часто вызывать onTick (1000 = раз в секунду)
        timer = new CountDownTimer(10000, 1000) {
            public void onTick(long millis) {
                // Показываем, сколько целых секунд осталось
                tvTimer.setText("Осталось: " + (millis / 1000) + " сек");
            }

            public void onFinish() {
                // Время вышло — ответ не засчитываем и идём дальше
                tvTimer.setText("Время вышло!");
                current++;
                if (current < questions.length) {
                    showQuestion();
                } else {
                    showResult();
                }
            }
        };
        timer.start();
    }

    // Проверяет выбранный ответ и, если он правильный, увеличивает счёт
    void checkAnswer() {
        int checkedId = rgAnswers.getCheckedRadioButtonId(); // id выбранной кнопки
        int selected = -1; // -1 означает "ничего не выбрано"
        // Определяем, какой именно вариант выбран (0..3)
        if (checkedId == R.id.rb0) selected = 0;
        else if (checkedId == R.id.rb1) selected = 1;
        else if (checkedId == R.id.rb2) selected = 2;
        else if (checkedId == R.id.rb3) selected = 3;

        // Сравниваем с правильным ответом
        if (selected == correct[current]) {
            score++;
        }
    }

    // Показывает финальный экран с результатом
    void showResult() {
        // Прячем варианты ответа и кнопку — вопросы закончились
        rgAnswers.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);
        tvTimer.setText("");
        tvNumber.setText("Тест завершён");

        int mark = getMark(score); // считаем оценку
        tvQuestion.setText("Правильных ответов: " + score + " из " + questions.length
                + "\nОценка: " + mark);
    }

    // Переводит число правильных ответов (из 5) в оценку по 5-балльной шкале
    int getMark(int s) {
        if (s == 5) return 5;
        if (s == 4) return 4;
        if (s == 3) return 3;
        if (s == 2) return 2;
        return 2; // 0-1 правильных — всё равно "2"
    }
}
