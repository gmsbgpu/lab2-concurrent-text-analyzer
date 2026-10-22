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
import java.util.concurrent.atomic.LongAdder;

/**
 * ConcurrentHashMap<String, LongAdder> + computeIfAbsent(...).increment().
 * LongAdder разносит инкременты по ячейкам и снижает конкуренцию
 * на горячих ключах (у нас 500 слов с перекосом вероятностей —
 * как раз горячие ключи).
 */
public class LongAdderAnalyzer implements WordAnalyzer {

    @Override
    public String name() {
        return "long-adder";
    }

    @Override
    public Map<String, Long> analyze(Path directory, int threadCount)
            throws IOException, InterruptedException {

        List<Path> files = TextFiles.listTextFiles(directory);

        ConcurrentHashMap<String, LongAdder> frequencies =
                new ConcurrentHashMap<>(1024);

        ParallelFileProcessor.run(files, threadCount, file -> {
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    for (String word : TextFiles.tokenizeLine(line)) {
                        frequencies
                                .computeIfAbsent(word, k -> new LongAdder())
                                .increment();
                    }
                }
            }
        });

        Map<String, Long> result = new HashMap<>(frequencies.size() * 2);
        for (Map.Entry<String, LongAdder> e : frequencies.entrySet()) {
            result.put(e.getKey(), e.getValue().sum());
        }
        return result;
    }
}