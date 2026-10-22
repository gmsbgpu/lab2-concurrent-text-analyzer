package com.bgpu.student.lab2;

import com.bgpu.student.lab2.analyzer.*;
import com.bgpu.student.lab2.benchmark.Benchmark;
import com.bgpu.student.lab2.benchmark.ChartWriter;
import com.bgpu.student.lab2.benchmark.CsvWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Режимы запуска:
 *   1) java ... Main <dir>
 *      — полный прогон: все варианты × [1,2,4,8,16,32,64],
 *        запись results/measurements.csv + три PNG.
 *
 *   2) java ... Main <dir> <variant> <threads>
 *      — один вариант с заданным числом потоков.
 *        variant = sequential | hashmap | synchronized-map |
 *                  concurrent-get-put | concurrent-merge |
 *                  long-adder | local-maps
 */
public class Main {

    private static final int[] THREADS = {1, 2, 4, 8, 16, 32, 64};
    private static final int WARMUP = 2;
    private static final int MEASUREMENTS = 5;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        Path dir = Path.of(args[0]);
        if (!dir.toFile().isDirectory()) {
            System.err.println("Not a directory: " + dir);
            System.exit(1);
        }

        Map<String, WordAnalyzer> all = createAnalyzers();

        if (args.length == 3) {
            runOne(dir, all, args[1], Integer.parseInt(args[2]));
            return;
        }
        if (args.length == 1) {
            runAll(dir, all);
            return;
        }

        printUsage();
        System.exit(1);
    }

    private static Map<String, WordAnalyzer> createAnalyzers() {
        Map<String, WordAnalyzer> m = new LinkedHashMap<>();
        m.put("sequential", new SequentialAnalyzer());
        m.put("hashmap", new SequentialAnalyzer()); // псевдоним: тот же код
        m.put("synchronized-map", new SynchronizedMapAnalyzer());
        m.put("concurrent-get-put", new ConcurrentGetPutAnalyzer());
        m.put("concurrent-merge", new ConcurrentMergeAnalyzer());
        m.put("long-adder", new LongAdderAnalyzer());
        m.put("local-maps", new LocalMapsAnalyzer());
        return m;
    }

    private static void runOne(Path dir, Map<String, WordAnalyzer> all,
                               String variant, int threads) throws Exception {
        WordAnalyzer a = all.get(variant);
        if (a == null) {
            System.err.println("Unknown variant: " + variant);
            System.err.println("Available: " + all.keySet());
            System.exit(1);
        }

        Benchmark b = new Benchmark(dir, WARMUP, MEASUREMENTS);
        Benchmark.Result r = b.run(a, threads);

        System.out.printf("%s, threads=%d, time=%.4f s, correct=%b%n",
                r.variant(), r.threads(), r.medianSeconds(), r.correct());
    }

    private static void runAll(Path dir, Map<String, WordAnalyzer> all)
            throws Exception {

        Benchmark b = new Benchmark(dir, WARMUP, MEASUREMENTS);
        List<Benchmark.Result> results = new ArrayList<>();

        for (Map.Entry<String, WordAnalyzer> e : all.entrySet()) {
            String name = e.getKey();
            WordAnalyzer a = e.getValue();

            for (int threads : THREADS) {
                // sequential и его псевдоним hashmap — только 1 поток.
                if ("sequential".equals(name) || "hashmap".equals(name)) {
                    if (threads != 1) continue;
                }

                System.out.printf("Running %-18s threads=%-3d ... ",
                        name, threads);
                System.out.flush();

                Benchmark.Result r = b.run(a, threads);
                results.add(r);

                System.out.printf("%.4f s, correct=%b%n",
                        r.medianSeconds(), r.correct());
            }
        }

        Path resultsDir = Path.of("results");
        CsvWriter.write(resultsDir.resolve("measurements.csv"), results);
        ChartWriter.writeAll(resultsDir, results);

        System.out.println();
        System.out.println("Written:");
        System.out.println("  " + resultsDir.resolve("measurements.csv"));
        System.out.println("  " + resultsDir.resolve("time.png"));
        System.out.println("  " + resultsDir.resolve("speedup.png"));
        System.out.println("  " + resultsDir.resolve("efficiency.png"));
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java ... Main <dir>");
        System.out.println("      full run: all variants × " + java.util.Arrays.toString(THREADS));
        System.out.println("  java ... Main <dir> <variant> <threads>");
        System.out.println("      one variant");
    }
}