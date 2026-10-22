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
 * ConcurrentHashMap.merge(word, 1L, Long::sum).
 * merge атомарен на уровне отдельного бакета (внутри CHM),
 * поэтому lost update невозможен.
 */
public class ConcurrentMergeAnalyzer implements WordAnalyzer {

    @Override
    public String name() {
        return "concurrent-merge";
    }

    @Override
    public Map<String, Long> analyze(Path directory, int threadCount)
            throws IOException, InterruptedException {

        List<Path> files = TextFiles.listTextFiles(directory);

        ConcurrentHashMap<String, Long> frequencies =
                new ConcurrentHashMap<>(1024);

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

        return new HashMap<>(frequencies);
    }
}