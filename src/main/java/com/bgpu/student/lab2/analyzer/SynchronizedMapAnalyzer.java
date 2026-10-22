package com.bgpu.student.lab2.analyzer;

import com.bgpu.student.lab2.util.TextFiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Общая HashMap, обёрнутая в Collections.synchronizedMap.
 *
 * ВАЖНО: даже synchronizedMap не делает составные операции
 * (get + put) атомарными. Здесь используется merge(), который
 * внутри синхронизируется на самой мапе (для synchronizedMap
 * это работает, т.к. merge делегируется в synchronized-обёртку
 * с захватом монитора).
 */
public class SynchronizedMapAnalyzer implements WordAnalyzer {

    @Override
    public String name() {
        return "synchronized-map";
    }

    @Override
    public Map<String, Long> analyze(Path directory, int threadCount)
            throws IOException, InterruptedException {

        List<Path> files = TextFiles.listTextFiles(directory);

        Map<String, Long> frequencies =
                Collections.synchronizedMap(new HashMap<>(1024));

        ParallelFileProcessor.run(files, threadCount, file -> {
            try (BufferedReader reader = Files.newBufferedReader(file)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    for (String word : TextFiles.tokenizeLine(line)) {
                        frequencies.merge(word, 1L, Long::sum);
                    }
                }
            }
        });

        // Возвращаем снимок — чтобы не отдавать наружу живую synchronizedMap.
        synchronized (frequencies) {
            return new HashMap<>(frequencies);
        }
    }
}