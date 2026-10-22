# Лабораторная работа №2. Многопоточный анализ текстовых данных

## Требования

- Java 21
- Maven 3.9+

## Сборка

```bash
mvn compile
```

## Генерация данных

```bash
java -cp target/classes ru.student.lab2.TestDataGenerator generated-data
```

## Запуск эксперимента

Все варианты × все количества потоков, с сохранением в `results/measurements.csv`:

```bash
java -cp target/classes ru.student.lab2.Main generated-data
```

Запуск одного варианта:

```bash
java -cp target/classes ru.student.lab2.Main generated-data concurrent-merge 4
```

## Результаты

- `results/measurements.csv` — исходные измерения
- графики и выводы строятся по CSV

Подробности см. в `results/conclusions.md`.