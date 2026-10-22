package com.bgpu.student.lab2.analyzer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Раздаёт файлы потокам и ждёт завершения.
 * Каждый поток обрабатывает свой кусок списка файлов.
 */
public final class ParallelFileProcessor {

    /** Обработчик одного файла. */
    @FunctionalInterface
    public interface FileTask {
        void process(Path file) throws IOException;
    }

    private ParallelFileProcessor() {
    }

    public static void run(List<Path> files, int threadCount, FileTask task)
            throws IOException, InterruptedException {

        if (files.isEmpty()) {
            return;
        }
        if (threadCount <= 0) {
            throw new IllegalArgumentException("threadCount <= 0");
        }

        // Если потоков больше, чем файлов — лишние не нужны.
        int actualThreads = Math.min(threadCount, files.size());

        // Распределяем файлы по потокам "по кругу": i, i+actualThreads, ...
        List<List<Path>> chunks = new ArrayList<>(actualThreads);
        for (int i = 0; i < actualThreads; i++) {
            chunks.add(new ArrayList<>());
        }
        for (int i = 0; i < files.size(); i++) {
            chunks.get(i % actualThreads).add(files.get(i));
        }

        ExecutorService pool = Executors.newFixedThreadPool(actualThreads);
        try {
            List<Future<?>> futures = new ArrayList<>(actualThreads);
            for (List<Path> chunk : chunks) {
                futures.add(pool.submit(() -> {
                    for (Path file : chunk) {
                        try {
                            task.process(file);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                try {
                    f.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException re
                            && re.getCause() instanceof IOException io) {
                        throw io;
                    }
                    throw new RuntimeException(cause);
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }
}