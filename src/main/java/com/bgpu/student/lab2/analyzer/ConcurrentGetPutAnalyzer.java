package com.bgpu.student.lab2.analyzer;

import com.bgpu.student.lab2.util.TextFiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ДЕМОНСТРАЦИОННО НЕКОРРЕКТНЫЙ вариант.
 *
 * ConcurrentHashMap делает каждую отдельную операцию get/put
 * потокобезопасной, но составная операция
 *     map.put(word, map.getOrDefault(word, 0) + 1)
 * НЕ является атомарной. Между get и put другой поток может
 * успеть обновить значение — возникает lost update.
 *
 * Этот класс оставлен намеренно: он должен падать на проверке
 * корректности и иллюстрировать ошибку.
 */
public class ConcurrentGetPutAnalyzer implements WordAnalyzer {

    @Override
    public String name() {
        return "concurrent-get-put";
    }

    @Override
    public Map<String, Long> analyze(Path directory, int threadCount)
            throws IOException, InterruptedException {

        List<Path> files = TextFiles.listTextFiles(directory);

        // <String, Long> — но обновление через get+put, не атомарно.
        ConcurrentHashMap<String, Long> frequencies =
                new ConcurrentHashMap<>(1024);

        ParallelFileProcessor.run(files, threadCount, file -> {
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    for (String word : TextFiles.tokenizeLine(line)) {
                        // LOST UPDATE: get и put — две разные операции.
                        frequencies.put(word,
                                frequencies.getOrDefault(word, 0L) + 1L);
                    }
                }
            }
        });

        return new HashMap<>(frequencies);
    }
}