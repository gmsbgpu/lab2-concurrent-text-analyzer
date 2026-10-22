package com.bgpu.student.lab2.benchmark;

import com.bgpu.student.lab2.analyzer.WordAnalyzer;
import com.bgpu.student.lab2.util.StatisticsChecker;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Измеряет время работы одного анализатора при заданном числе потоков.
 * Методика: warmup-прогоны (время не учитывается) + N измерений,
 * в отчёт идёт медиана.
 */
public final class Benchmark {

    public record Result(
            String variant,
            int threads,
            double medianSeconds,
            boolean correct
    ) {
    }

    private final Path directory;
    private final Map<String, Long> expected;
    private final int warmupRuns;
    private final int measurementRuns;

    public Benchmark(Path directory, int warmupRuns, int measurementRuns)
            throws IOException {
        this.directory = directory;
        this.expected = StatisticsChecker.loadExpected(directory);
        this.warmupRuns = warmupRuns;
        this.measurementRuns = measurementRuns;
    }

    public Result run(WordAnalyzer analyzer, int threadCount)
            throws IOException, InterruptedException {

        // 1. Прогрев (JIT + прогрев кеша страниц файловой системы).
        for (int i = 0; i < warmupRuns; i++) {
            Map<String, Long> r = analyzer.analyze(directory, threadCount);
            // проверку корректности на прогреве не делаем — только гоняем код
            if (r.isEmpty() && !expected.isEmpty()) {
                throw new IllegalStateException(
                        "Warmup returned empty result for " + analyzer.name());
            }
        }

        // 2. Измерения.
        List<Double> times = new ArrayList<>(measurementRuns);
        boolean correct = true;

        for (int i = 0; i < measurementRuns; i++) {
            long start = System.nanoTime();
            Map<String, Long> actual = analyzer.analyze(directory, threadCount);
            long end = System.nanoTime();

            times.add((end - start) / 1_000_000_000.0);

            if (correct) {
                String error = StatisticsChecker.compare(actual, expected);
                if (error != null) {
                    correct = false;
                }
            }
        }

        Collections.sort(times);
        double median = times.get(times.size() / 2);

        return new Result(analyzer.name(), threadCount, median, correct);
    }
}