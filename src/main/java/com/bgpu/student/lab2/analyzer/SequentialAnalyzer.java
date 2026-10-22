package com.bgpu.student.lab2.analyzer;

import com.bgpu.student.lab2.util.TextFiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Последовательный подсчёт в одном потоке через HashMap.
 * Базовая точка отсчёта T1.
 */
public class SequentialAnalyzer implements WordAnalyzer {

    @Override
    public String name() {
        return "sequential";
    }

    @Override
    public Map<String, Long> analyze(Path directory, int threadCount)
            throws IOException {
        List<Path> files = TextFiles.listTextFiles(directory);

        Map<String, Long> frequencies = new HashMap<>(1024);

        for (Path file : files) {
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    for (String word : TextFiles.tokenizeLine(line)) {
                        frequencies.merge(word, 1L, Long::sum);
                    }
                }
            }
        }

        return frequencies;
    }
}