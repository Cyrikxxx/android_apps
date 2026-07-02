ПРИЛОЖЕНИЕ 2. Конвертер валют
=====================================

Что делает:
- Вводишь сумму в рублях, выбираешь валюту (USD / EUR через RadioGroup).
- По кнопке считает результат по актуальному курсу ЦБ РФ.
- Курсы грузятся при запуске из открытого API:
      https://www.cbr-xml-daily.ru/daily_json.js
  через HttpURLConnection в отдельном Thread, парсинг org.json,
  обновление экрана через runOnUiThread.

Формула: рубли делятся на курс (в API Value = сколько рублей за 1 единицу
валюты), поэтому рубли / курс = сумма в валюте.

--------------------------------------------------
КУДА КОПИРОВАТЬ ФАЙЛЫ
--------------------------------------------------

1) MainActivity.java
   -> app/src/main/java/com/example/<ваш_пакет>/MainActivity.java
   Заменить содержимое.

2) activity_main.xml
   -> app/src/main/res/layout/activity_main.xml
   Заменить содержимое.

3) AndroidManifest.xml
   -> app/src/main/AndroidManifest.xml
   ВАЖНО: скопировать строку с uses-permission INTERNET (см. ниже).

--------------------------------------------------
ЧТО ПОМЕНЯТЬ РУКАМИ
--------------------------------------------------
- Первая строка MainActivity.java:
      package com.example.currencyconverter;
  замените на реальный пакет вашего проекта.
- В AndroidManifest.xml package="com.example.currencyconverter"
  привести в соответствие (или убрать, если namespace задан в build.gradle).

--------------------------------------------------
PERMISSIONS (ОБЯЗАТЕЛЬНО!)
--------------------------------------------------
В AndroidManifest.xml, ВЫШЕ тега <application>, добавить:

    <uses-permission android:name="android.permission.INTERNET" />

Без этого разрешения сетевой запрос упадёт и курсы не загрузятся.

--------------------------------------------------
НЮАНСЫ СЕТИ (ВАЖНО)
--------------------------------------------------
- Android ЗАПРЕЩАЕТ сетевые запросы в главном (UI) потоке.
  Если сделать HttpURLConnection прямо в onCreate/onClick, приложение
  упадёт с NetworkOnMainThreadException. Поэтому запрос вынесен в
  new Thread(...).start(), а результат возвращается в UI через
  runOnUiThread(...).
- API отдаёт данные по HTTPS, поэтому usesCleartextTraffic не нужен
  (стоит false). Если вдруг понадобится HTTP — поставьте true.
- Нужен интернет на эмуляторе/телефоне, иначе увидите "Ошибка загрузки курсов".
- minSdk: любой (21+), спец. API не используется.
