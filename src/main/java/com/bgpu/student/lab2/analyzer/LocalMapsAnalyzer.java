package com.bgpu.student.lab2.analyzer;

import com.bgpu.student.lab2.util.TextFiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Каждый поток ведёт локальную HashMap, после завершения все
 * локальные словари сливаются в итоговый.
 *
 * Не используем ParallelFileProcessor: нам нужен результат
 * от каждой задачи, а не просто "выполнить и забыть".
 */
public class LocalMapsAnalyzer implements WordAnalyzer {

    @Override
    public String name() {
        return "local-maps";
    }

    @Override
    public Map<String, Long> analyze(Path directory, int threadCount)
            throws IOException, InterruptedException {

        List<Path> files = TextFiles.listTextFiles(directory);
        if (files.isEmpty()) {
            return new HashMap<>();
        }

        int actualThreads = Math.max(1, Math.min(threadCount, files.size()));

        // Делим файлы на actualThreads кусков "по кругу".
        List<List<Path>> chunks = new ArrayList<>(actualThreads);
        for (int i = 0; i < actualThreads; i++) {
            chunks.add(new ArrayList<>());
        }
        for (int i = 0; i < files.size(); i++) {
            chunks.get(i % actualThreads).add(files.get(i));
        }

        // Каждый поток кладёт свою локальную мапу в общий список.
        List<Map<String, Long>> localResults =
                new ArrayList<>(actualThreads);

        var pool = java.util.concurrent.Executors
                .newFixedThreadPool(actualThreads);
        try {
            List<java.util.concurrent.Future<Map<String, Long>>> futures =
                    new ArrayList<>(actualThreads);

            for (List<Path> chunk : chunks) {
                futures.add(pool.submit(() -> {
                    Map<String, Long> local = new HashMap<>(1024);
                    for (Path file : chunk) {
                        try (BufferedReader reader =
                                     Files.newBufferedReader(file)) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                for (String word :
                                        TextFiles.tokenizeLine(line)) {
                                    local.merge(word, 1L, Long::sum);
                                }
                            }
                        }
                    }
                    return local;
                }));
            }

            for (var f : futures) {
                try {
                    localResults.add(f.get());
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof java.io.IOException io) {
                        throw io;
                    }
                    throw new RuntimeException(cause);
                }
            }
        } finally {
            pool.shutdownNow();
        }

        // Слияние локальных мап в одну.
        Map<String, Long> merged = new HashMap<>(2048);
        for (Map<String, Long> local : localResults) {
            for (Map.Entry<String, Long> e : local.entrySet()) {
                merged.merge(e.getKey(), e.getValue(), Long::sum);
            }
        }
        return merged;
    }
}