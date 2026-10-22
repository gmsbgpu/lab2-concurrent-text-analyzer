package com.bgpu.student.lab2.analyzer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Общий интерфейс всех реализаций подсчёта частоты слов.
 */
public interface WordAnalyzer {

    /**
     * @return имя варианта для отчёта (например "concurrent-merge")
     */
    String name();

    /**
     * Обработать все *.txt из directory и вернуть частотный словарь
     * слово → количество вхождений.
     */
    Map<String, Long> analyze(Path directory, int threadCount)
            throws IOException, InterruptedException;
}